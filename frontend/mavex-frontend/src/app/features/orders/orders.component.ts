import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe }   from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subject }    from 'rxjs';
import { debounceTime, distinctUntilChanged } from 'rxjs/operators';
import { forkJoin }   from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService }       from '../../core/services/layout.service';
import { OrderService }        from '../../core/services/order.service';
import { ClientService }       from '../../core/services/client.service';
import { EmailService }        from '../../core/services/email.service';
import { ToastService }        from '../../core/services/toast.service';
import { ExchangeRateService } from '../../core/services/exchange-rate.service';
import { BadgeComponent } from '../../shared/badge/badge.component';
import { BulkEmailResult, EmailLogResponse, OrderPatch, OrderResponse, OrderStatus } from '../../core/models/order.model';
import { ClientResponse } from '../../core/models/client.model';
import { RequiresPermissionDirective } from '../../core/directives/requires-permission.directive';

@Component({
  selector: 'app-orders',
  imports: [RouterLink, DatePipe, BadgeComponent, ReactiveFormsModule, RequiresPermissionDirective],
  templateUrl: './orders.component.html',
  styleUrl:    './orders.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrdersComponent implements OnInit {
  private readonly layout     = inject(LayoutService);
  private readonly orderSvc   = inject(OrderService);
  private readonly clientSvc  = inject(ClientService);
  private readonly emailSvc   = inject(EmailService);
  private readonly toast      = inject(ToastService);
  private readonly fb         = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  private readonly erSvc      = inject(ExchangeRateService);

  private readonly searchSubject = new Subject<string>();

  /* ── Data ─────────────────────────────────────────────── */
  orders  = signal<OrderResponse[]>([]);

  /* ── Conversion devise ───────────────────────────────────── */
  readonly selectedCurrency = signal<string>('MAD');
  readonly selectedRateDate = signal<string>('');
  readonly currentRate      = signal<{ rate: number; effectiveDate: string } | null>(null);
  readonly currencies       = signal<string[]>([]);
  readonly rateLoading      = signal(false);
  total   = signal(0);
  page    = signal(0);
  loading = signal(true);
  pageSize = signal(20);

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

  /* ── Modal pièces jointes ─────────────────────────────── */
  emailModalOpen     = signal(false);
  emailModalOrderId  = signal<number | null>(null);   // null = bulk
  emailModalFiles    = signal<File[]>([]);
  emailModalDragOver = signal(false);
  emailModalSending  = signal(false);

  readonly ALLOWED_TYPES = [
    'application/pdf',
    'image/jpeg', 'image/png',
    'application/vnd.ms-excel',
    'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
  ];
  readonly MAX_FILE_SIZE = 10 * 1024 * 1024;
  readonly MAX_FILES = 5;

  /* ── Modals ───────────────────────────────────────────── */
  detail   = signal<OrderResponse | null>(null);
  deleteId = signal<number | null>(null);
  deleting = signal(false);

  /* ── Historique emails ────────────────────────────────── */
  emailHistory      = signal<EmailLogResponse[]>([]);
  emailHistoryOrder = signal<OrderResponse | null>(null);
  loadingHistory    = signal(false);

  /* ── Edit order ───────────────────────────────────────── */
  editingOrder = signal<OrderResponse | null>(null);
  savingOrder  = signal(false);
  lastSavedId  = signal<number | null>(null);
  clients      = signal<ClientResponse[]>([]);

  editClientSearch       = signal('');
  showEditClientDropdown = signal(false);
  selectedEditClientName = signal('');

  filteredEditClients = computed(() => {
    const q = this.editClientSearch().toLowerCase().trim();
    if (!q) return this.clients().slice(0, 50);
    return this.clients().filter(c =>
      c.fullName?.toLowerCase().includes(q) ||
      c.email?.toLowerCase().includes(q) ||
      c.city?.toLowerCase().includes(q)
    ).slice(0, 50);
  });

  editOrderForm = this.fb.group({
    hawb:             ['', [Validators.required, Validators.minLength(2)]],
    clientId:         [null as number | null, Validators.required],
    numberOfItems:    [null as number | null],
    goodsDescription: [''],
    shipmentWeight:   [null as number | null],
    htsusCode:        [''],
    customsValue:     [null as number | null, [Validators.required, Validators.min(0.01)]],
    customsCurrency:  ['USD'],
    dutyRate:         [null as number | null],
    bankCharges:      [null as number | null],
  });

  /* ── Computed ─────────────────────────────────────────── */
  totalPages  = computed(() => Math.ceil(this.total() / this.pageSize()));
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
    this.erSvc.getCurrencies()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({ next: list => this.currencies.set(list.length ? list : ['USD', 'EUR', 'MAD']) });
    this.loadRate();
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
    }, this.page(), this.pageSize())
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

  /* ── Email unitaire — ouvre le modal ─────────────────── */
  sendEmail(id: number): void {
    this.emailModalOrderId.set(id);
    this.emailModalFiles.set([]);
    this.emailModalOpen.set(true);
  }

  /* ── Bulk actions ─────────────────────────────────────── */
  openBulkConfirm(action: 'email' | 'paid'): void {
    if (action === 'email') {
      this.emailModalOrderId.set(null);
      this.emailModalFiles.set([]);
      this.emailModalOpen.set(true);
    } else {
      this.bulkConfirm.set(action);
    }
  }
  closeBulkConfirm(): void { this.bulkConfirm.set(null); }

  /* ── Modal pièces jointes ─────────────────────────────── */
  closeEmailModal(): void {
    this.emailModalOpen.set(false);
    this.emailModalOrderId.set(null);
    this.emailModalFiles.set([]);
    this.emailModalDragOver.set(false);
  }

  onEmailModalDragOver(event: DragEvent): void {
    event.preventDefault();
    this.emailModalDragOver.set(true);
  }

  onEmailModalDragLeave(): void { this.emailModalDragOver.set(false); }

  onEmailModalDrop(event: DragEvent): void {
    event.preventDefault();
    this.emailModalDragOver.set(false);
    this.addFiles(Array.from(event.dataTransfer?.files ?? []));
  }

  onEmailModalFileInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    this.addFiles(Array.from(input.files ?? []));
    input.value = '';
  }

  private addFiles(newFiles: File[]): void {
    const current = this.emailModalFiles();
    const merged: File[] = [...current];
    for (const f of newFiles) {
      if (merged.length >= this.MAX_FILES) {
        this.toast.error(`Maximum ${this.MAX_FILES} fichiers autorisés.`);
        break;
      }
      if (!this.ALLOWED_TYPES.includes(f.type)) {
        this.toast.error(`Type non autorisé : ${f.name}`);
        continue;
      }
      if (f.size > this.MAX_FILE_SIZE) {
        this.toast.error(`Fichier trop volumineux (max 10 MB) : ${f.name}`);
        continue;
      }
      if (merged.some(x => x.name === f.name && x.size === f.size)) continue;
      merged.push(f);
    }
    this.emailModalFiles.set(merged);
  }

  removeAttachment(index: number): void {
    this.emailModalFiles.update(files => files.filter((_, i) => i !== index));
  }

  confirmSendEmail(): void {
    const orderId = this.emailModalOrderId();
    const files   = this.emailModalFiles();
    this.emailModalSending.set(true);

    if (orderId !== null) {
      this.sendingId.set(orderId);
      this.emailSvc.sendToOrder(orderId, files).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: res => {
          this.sendingId.set(null);
          this.emailModalSending.set(false);
          this.closeEmailModal();
          if (res.success) {
            this.toast.success('Email envoyé avec succès.');
          } else {
            this.toast.error(res.message || 'Échec envoi email.');
          }
          this.loadOrders(); this.loadKpis();
        },
        error: err => {
          this.sendingId.set(null);
          this.emailModalSending.set(false);
          this.toast.error(err?.error?.message || 'Erreur envoi email.');
        },
      });
    } else {
      const ids = Array.from(this.selectedIds());
      this.bulkLoading.set(true);
      this.emailSvc.sendBulkEmail(ids, files).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: r => {
          this.bulkLoading.set(false);
          this.emailModalSending.set(false);
          this.lastBulkResult.set(r);
          this.closeEmailModal();
          this.clearSelection();
          const msg = `${r.sent}/${r.total} email(s) envoyé(s)${r.failed > 0 ? ` — ${r.failed} échec(s)` : ''}.`;
          if (r.failed > 0) this.toast.error(msg);
          else              this.toast.success(msg);
          this.loadOrders(); this.loadKpis();
        },
        error: () => {
          this.bulkLoading.set(false);
          this.emailModalSending.set(false);
          this.toast.error('Erreur lors de l\'envoi.');
        },
      });
    }
  }

  emailModalTitle = computed(() =>
    this.emailModalOrderId() !== null
      ? 'Envoyer l\'email'
      : `Envoyer ${this.selCount()} email(s)`
  );

  emailModalSubtitle = computed(() => {
    const id = this.emailModalOrderId();
    if (id !== null) {
      const order = this.orders().find(o => o.id === id);
      return order ? `${order.hawb} — ${order.clientFullName ?? ''}` : '';
    }
    return `Action appliquée aux ${this.selCount()} orders sélectionnés.`;
  });

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

  /* ── Edit order ───────────────────────────────────────── */
  openEditOrder(o: OrderResponse): void {
    if (this.clients().length === 0) {
      this.clientSvc.getAllActive().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: list => this.clients.set(list),
      });
    }
    this.editingOrder.set(o);
    this.editOrderForm.patchValue({
      hawb:             o.hawb,
      clientId:         o.clientId ?? null,
      numberOfItems:    o.numberOfItems ?? null,
      goodsDescription: o.goodsDescription ?? '',
      shipmentWeight:   o.shipmentWeight ?? null,
      htsusCode:        o.htsusCode ?? '',
      customsValue:     o.customsValue ?? null,
      customsCurrency:  o.customsCurrency ?? 'USD',
      dutyRate:         o.dutyRate != null ? Math.round(o.dutyRate * 10000) / 100 : null,
      bankCharges:      o.bankCharges ?? null,
    });
    this.selectedEditClientName.set(o.clientFullName ?? '');
    this.editClientSearch.set('');
    this.showEditClientDropdown.set(false);
  }

  closeEditOrder(): void { this.editingOrder.set(null); }

  selectEditClient(c: ClientResponse): void {
    this.editOrderForm.patchValue({ clientId: c.id });
    this.selectedEditClientName.set(c.fullName);
    this.editClientSearch.set('');
    this.showEditClientDropdown.set(false);
  }

  clearEditClient(): void {
    this.editOrderForm.patchValue({ clientId: null });
    this.selectedEditClientName.set('');
    this.editClientSearch.set('');
  }

  onEditClientSearchInput(val: string): void {
    this.editClientSearch.set(val);
    this.showEditClientDropdown.set(true);
    if (!val) this.clearEditClient();
  }

  saveOrder(): void {
    if (this.editOrderForm.invalid) { this.editOrderForm.markAllAsTouched(); return; }
    const o = this.editingOrder();
    if (!o) return;
    this.savingOrder.set(true);
    const v = this.editOrderForm.value;
    const req: OrderPatch = {
      hawb:             v.hawb!,
      clientId:         v.clientId ?? undefined,
      numberOfItems:    v.numberOfItems ?? undefined,
      goodsDescription: v.goodsDescription || undefined,
      shipmentWeight:   v.shipmentWeight ?? undefined,
      htsusCode:        v.htsusCode || undefined,
      customsValue:     v.customsValue ?? undefined,
      customsCurrency:  v.customsCurrency || undefined,
      dutyRate:         v.dutyRate != null ? v.dutyRate / 100 : undefined,
      bankCharges:      v.bankCharges ?? undefined,
    };
    this.orderSvc.patch(o.id, req).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.savingOrder.set(false);
        this.editingOrder.set(null);
        this.toast.success('Order mis à jour avec succès.');
        this.loadOrders();
        this.loadKpis();
        this.lastSavedId.set(o.id);
        setTimeout(() => this.lastSavedId.set(null), 1800);
      },
      error: err => {
        this.savingOrder.set(false);
        this.toast.error(err?.error?.message || 'Erreur lors de la mise à jour.');
      },
    });
  }

  editOrderFieldInvalid(name: string): boolean {
    const c = this.editOrderForm.get(name);
    return !!(c?.invalid && c?.touched);
  }

  /* ── Detail ───────────────────────────────────────────── */
  openDetail(o: OrderResponse): void { this.detail.set(o); }
  closeDetail(): void                { this.detail.set(null); }

  /* ── Historique emails ────────────────────────────────── */
  openEmailHistory(o: OrderResponse): void {
    this.emailHistoryOrder.set(o);
    this.emailHistory.set([]);
    this.loadingHistory.set(true);
    this.orderSvc.getEmailLogs(o.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  logs => { this.emailHistory.set(logs); this.loadingHistory.set(false); },
        error: ()   => { this.loadingHistory.set(false); },
      });
  }

  closeEmailHistory(): void {
    this.emailHistoryOrder.set(null);
    this.emailHistory.set([]);
  }

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
  onPageSizeChange(size: number): void { this.pageSize.set(size); this.page.set(0); this.loadOrders(); }

  /* ── Helpers ──────────────────────────────────────────── */
  fmtAmount(n?: number | null, currency?: string | null): string {
    if (n == null) return '—';
    const cur = currency ?? 'MAD';
    return n.toLocaleString('fr-MA', { minimumFractionDigits: 0, maximumFractionDigits: 0 }) + ' ' + cur;
  }

  fmtWeight(n?: number | null): string {
    if (n == null) return '—';
    return n.toLocaleString('fr-FR', { maximumFractionDigits: 2 }) + ' kg';
  }

  fmtDuty(rate?: number | null): string {
    if (rate == null) return '—';
    return (Math.round(rate * 10000) / 100) + ' %';
  }

  rowClass(s: OrderStatus): string {
    const map: Record<OrderStatus, string> = {
      CREATED:         'or-row-created',
      EMAIL_SENT:      'or-row-email',
      EMAIL_OUTDATED:  'or-row-outdated',
      PENDING_PAYMENT: 'or-row-pending',
      PAID:            'or-row-paid',
      IN_DELIVERY:     'or-row-delivery',
      DELIVERED:       'or-row-delivered',
      CANCELLED:       'or-row-cancelled',
    };
    return map[s] ?? '';
  }

  loadRate(): void {
    const from = 'USD';
    const to   = this.selectedCurrency();
    if (from === to) { this.currentRate.set(null); return; }
    this.rateLoading.set(true);
    const date = this.selectedRateDate();
    const obs  = date
      ? this.erSvc.lookup(from, to, date)
      : this.erSvc.getActive(from, to);
    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:  r  => { this.currentRate.set({ rate: r.rate, effectiveDate: r.effectiveDate }); this.rateLoading.set(false); },
      error: () => { this.currentRate.set(null); this.rateLoading.set(false); },
    });
  }

  onCurrencyChange(val: string): void {
    this.selectedCurrency.set(val);
    this.loadRate();
  }

  onDateChange(val: string): void {
    this.selectedRateDate.set(val);
    this.loadRate();
  }

  getEquivalent(totalAmount?: number | null): number | null {
    if (totalAmount == null) return null;
    const r = this.currentRate();
    if (!r) return null;
    return Math.round(totalAmount * r.rate);
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
