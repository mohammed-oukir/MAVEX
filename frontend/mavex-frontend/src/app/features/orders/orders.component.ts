import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe }   from '@angular/common';
import { Subject }    from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { forkJoin }   from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService }  from '../../core/services/layout.service';
import { OrderService }   from '../../core/services/order.service';
import { EmailService }   from '../../core/services/email.service';
import { ToastService }   from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/badge/badge.component';
import { BulkEmailResult, OrderResponse, OrderStatus } from '../../core/models/order.model';

@Component({
  selector: 'app-orders',
  imports: [RouterLink, DatePipe, BadgeComponent],
  templateUrl: './orders.component.html',
  styleUrl:    './orders.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrdersComponent implements OnInit {
  private readonly layout     = inject(LayoutService);
  private readonly orderSvc   = inject(OrderService);
  private readonly emailSvc   = inject(EmailService);
  private readonly toast      = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  private readonly searchSubject = new Subject<string>();

  /* ── Data ─────────────────────────────────────────────── */
  orders  = signal<OrderResponse[]>([]);
  total   = signal(0);
  page    = signal(0);
  loading = signal(true);
  readonly pageSize = 20;

  /* ── KPIs ─────────────────────────────────────────────── */
  kpiTotal   = signal(0);
  kpiNoEmail = signal(0);
  kpiPending = signal(0);
  kpiPaid    = signal(0);

  /* ── Filtres ──────────────────────────────────────────── */
  searchQ   = signal('');
  statusTab = signal<OrderStatus | ''>('');
  fromDate  = signal('');
  toDate    = signal('');

  /* ── Sélection bulk ───────────────────────────────────── */
  selectedIds  = signal<Set<number>>(new Set());
  someSelected = computed(() => this.selectedIds().size > 0);
  allSelected  = computed(() => this.orders().length > 0 && this.selectedIds().size === this.orders().length);
  selCount     = computed(() => this.selectedIds().size);

  /* ── Actions ──────────────────────────────────────────── */
  sendingId   = signal<number | null>(null);
  bulkLoading = signal(false);
  bulkConfirm = signal<'email' | 'paid' | null>(null);
  lastBulkResult = signal<BulkEmailResult | null>(null);

  /* ── Modals ───────────────────────────────────────────── */
  detail   = signal<OrderResponse | null>(null);
  deleteId = signal<number | null>(null);
  deleting = signal(false);

  /* ── Computed ─────────────────────────────────────────── */
  totalPages  = computed(() => Math.ceil(this.total() / this.pageSize));
  hasFilters  = computed(() => !!this.searchQ() || !!this.statusTab() || !!this.fromDate() || !!this.toDate());

  /* ── Lifecycle ────────────────────────────────────────── */
  ngOnInit(): void {
    this.layout.setPage('Orders');

    this.searchSubject.pipe(
      debounceTime(350),
      distinctUntilChanged(),
      takeUntilDestroyed(this.destroyRef),
    ).subscribe(q => {
      this.searchQ.set(q);
      this.page.set(0);
      this.loadOrders();
    });

    this.loadKpis();
    this.loadOrders();
  }

  /* ── Chargement ───────────────────────────────────────── */
  private loadOrders(): void {
    this.loading.set(true);
    this.selectedIds.set(new Set());
    this.orderSvc.search({
      q:          this.searchQ()   || undefined,
      status:     this.statusTab() || undefined,
      from:       this.fromDate()  || undefined,
      to:         this.toDate()    || undefined,
    }, this.page(), this.pageSize)
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe({
      next: p => { this.orders.set(p.content); this.total.set(p.totalElements); this.loading.set(false); },
      error: ()  => this.loading.set(false),
    });
  }

  private loadKpis(): void {
    forkJoin({
      all:     this.orderSvc.search({}, 0, 1),
      noEmail: this.orderSvc.search({ status: 'CREATED' }, 0, 1),
      pending: this.orderSvc.search({ status: 'PENDING_PAYMENT' }, 0, 1),
      paid:    this.orderSvc.search({ status: 'PAID' }, 0, 1),
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: ({ all, noEmail, pending, paid }) => {
        this.kpiTotal.set(all.totalElements);
        this.kpiNoEmail.set(noEmail.totalElements);
        this.kpiPending.set(pending.totalElements);
        this.kpiPaid.set(paid.totalElements);
      },
    });
  }

  /* ── Filtres ──────────────────────────────────────────── */
  onSearchInput(e: Event): void {
    this.searchSubject.next((e.target as HTMLInputElement).value);
  }

  setTab(tab: OrderStatus | ''): void {
    this.statusTab.set(tab);
    this.page.set(0);
    this.loadOrders();
  }

  applyDates(): void { this.page.set(0); this.loadOrders(); }

  clearFilters(): void {
    this.searchQ.set('');
    this.statusTab.set('');
    this.fromDate.set('');
    this.toDate.set('');
    this.page.set(0);
    this.loadOrders();
  }

  /* ── Sélection ────────────────────────────────────────── */
  toggleSelect(id: number): void {
    this.selectedIds.update(s => {
      const n = new Set(s);
      n.has(id) ? n.delete(id) : n.add(id);
      return n;
    });
  }

  toggleAll(): void {
    this.allSelected()
      ? this.selectedIds.set(new Set())
      : this.selectedIds.set(new Set(this.orders().map(o => o.id)));
  }

  isSelected(id: number): boolean { return this.selectedIds().has(id); }
  clearSelection(): void          { this.selectedIds.set(new Set()); }

  /* ── Email unitaire ───────────────────────────────────── */
  sendEmail(id: number): void {
    this.sendingId.set(id);
    this.emailSvc.sendToOrder(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.sendingId.set(null);
        this.toast.success('Email envoyé avec succès.');
        this.loadOrders(); this.loadKpis();
      },
      error: err => { this.sendingId.set(null); this.toast.error(err?.error?.message || 'Erreur envoi email.'); },
    });
  }

  /* ── Bulk actions ─────────────────────────────────────── */
  openBulkConfirm(action: 'email' | 'paid'): void { this.bulkConfirm.set(action); }
  closeBulkConfirm(): void                         { this.bulkConfirm.set(null); }

  executeBulkEmail(): void {
    const ids = Array.from(this.selectedIds());
    this.bulkLoading.set(true);
    this.bulkConfirm.set(null);
    this.orderSvc.bulkEmail(ids).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: r => {
        this.bulkLoading.set(false);
        this.lastBulkResult.set(r);
        this.clearSelection();
        this.toast.success(`${r.sent}/${r.total} email(s) envoyé(s)${r.failed > 0 ? ` — ${r.failed} échec(s)` : ''}.`);
        this.loadOrders(); this.loadKpis();
      },
      error: () => { this.bulkLoading.set(false); this.toast.error('Erreur lors de l\'envoi.'); },
    });
  }

  executeBulkPaid(): void {
    const ids = Array.from(this.selectedIds());
    this.bulkLoading.set(true);
    this.bulkConfirm.set(null);
    this.orderSvc.bulkStatus(ids, 'PAID', 'Paiement enregistré en masse').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.bulkLoading.set(false);
        this.clearSelection();
        this.toast.success(`${ids.length} order(s) marqués comme payés.`);
        this.loadOrders(); this.loadKpis();
      },
      error: () => { this.bulkLoading.set(false); this.toast.error('Erreur lors de la mise à jour.'); },
    });
  }

  /* ── Detail ───────────────────────────────────────────── */
  openDetail(o: OrderResponse): void { this.detail.set(o); }
  closeDetail(): void                { this.detail.set(null); }

  /* ── Suppression ──────────────────────────────────────── */
  confirmDelete(id: number): void { this.deleteId.set(id); }
  cancelDelete(): void            { this.deleteId.set(null); }

  executeDelete(): void {
    const id = this.deleteId();
    if (!id) return;
    this.deleting.set(true);
    this.orderSvc.delete(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.deleting.set(false); this.deleteId.set(null);
        this.toast.success('Order supprimé.'); this.loadOrders(); this.loadKpis();
      },
      error: () => { this.deleting.set(false); this.toast.error('Erreur.'); },
    });
  }

  /* ── Pagination ───────────────────────────────────────── */
  goToPage(p: number): void { this.page.set(p); this.loadOrders(); }
  pagesArray(): number[]    { return Array.from({ length: this.totalPages() }, (_, i) => i); }

  /* ── Helpers ──────────────────────────────────────────── */
  fmtAmount(n?: number | null): string {
    if (n == null) return '—';
    return n.toLocaleString('fr-MA', { minimumFractionDigits: 0, maximumFractionDigits: 0 }) + ' MAD';
  }

  fmtWeight(n?: number | null): string {
    if (n == null) return '—';
    return n.toLocaleString('fr-FR', { maximumFractionDigits: 2 }) + ' kg';
  }

  fmtDuty(rate?: number | null): string {
    if (rate == null) return '—';
    return (Math.round(rate * 10000) / 100) + ' %';
  }

  statusLabel(s: OrderStatus): string {
    const labels: Record<OrderStatus, string> = {
      CREATED:         'Créé',
      EMAIL_SENT:      'Email envoyé',
      PENDING_PAYMENT: 'En attente',
      PAID:            'Payé',
      IN_DELIVERY:     'En livraison',
      DELIVERED:       'Livré',
      CANCELLED:       'Annulé',
    };
    return labels[s] ?? s;
  }

  statusClass(s: OrderStatus): string {
    const map: Record<OrderStatus, string> = {
      CREATED:         'or-s-created',
      EMAIL_SENT:      'or-s-email',
      PENDING_PAYMENT: 'or-s-pending',
      PAID:            'or-s-paid',
      IN_DELIVERY:     'or-s-delivery',
      DELIVERED:       'or-s-delivered',
      CANCELLED:       'or-s-cancelled',
    };
    return map[s] ?? '';
  }

  rowClass(s: OrderStatus): string {
    const map: Record<OrderStatus, string> = {
      CREATED:         'or-row-created',
      EMAIL_SENT:      'or-row-email',
      PENDING_PAYMENT: 'or-row-pending',
      PAID:            'or-row-paid',
      IN_DELIVERY:     'or-row-delivery',
      DELIVERED:       'or-row-delivered',
      CANCELLED:       'or-row-cancelled',
    };
    return map[s] ?? '';
  }

  timeAgo(dateStr: string | undefined): string {
    if (!dateStr) return '';
    const diff = Date.now() - new Date(dateStr).getTime();
    const mins  = Math.floor(diff / 60000);
    const hours = Math.floor(diff / 3600000);
    const days  = Math.floor(diff / 86400000);
    const weeks = Math.floor(days / 7);
    if (mins  < 1)  return 'à l\'instant';
    if (mins  < 60) return `il y a ${mins}min`;
    if (hours < 24) return `il y a ${hours}h`;
    if (days  < 7)  return `il y a ${days}j`;
    return `il y a ${weeks}sem`;
  }
}
