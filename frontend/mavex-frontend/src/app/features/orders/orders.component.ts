import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe }   from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin }   from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService }       from '../../core/services/layout.service';
import { OrderService }        from '../../core/services/order.service';
import { ClientService }       from '../../core/services/client.service';
import { EmailService }        from '../../core/services/email.service';
import { ToastService }        from '../../core/services/toast.service';
import { ExchangeRateService } from '../../core/services/exchange-rate.service';
import { BadgeComponent } from '../../shared/badge/badge.component';
import { BulkEmailResult, EmailLogResponse, OrderPatch, OrderResponse, OrderSearchParams, OrderStatus } from '../../core/models/order.model';
import { ClientResponse } from '../../core/models/client.model';

@Component({
  selector: 'app-orders',
  imports: [RouterLink, DatePipe, BadgeComponent, ReactiveFormsModule],
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
  private readonly route      = inject(ActivatedRoute);

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
  kpiPaid    = signal(0);

  /* ── Panneau de filtres ───────────────────────────────── */
  filtersCollapsed    = signal(false);
  advancedFiltersOpen = signal(false);
  flHawb         = signal('');
  flClient       = signal('');
  flClientEmail  = signal('');
  flShipment     = signal('');
  flStatus       = signal<OrderStatus | ''>('');
  flCurrency     = signal('');
  flWeight       = signal('');
  flCustomsValue = signal('');
  flTotalAmount  = signal('');
  flDutyRate     = signal('');
  flFrom         = signal('');
  flTo           = signal('');

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
  hasFilters = computed(() =>
    !!this.flHawb()         || !!this.flClient()      || !!this.flClientEmail() ||
    !!this.flShipment()     ||
    !!this.flStatus()       || !!this.flCurrency()    || !!this.flWeight()   ||
    !!this.flCustomsValue() || !!this.flTotalAmount() || !!this.flDutyRate() ||
    !!this.flFrom()         || !!this.flTo()
  );

  /* ── Lifecycle ────────────────────────────────────────── */
  ngOnInit(): void {
    this.layout.setPage('Orders');

    const hawbParam = this.route.snapshot.queryParamMap.get('hawb');
    if (hawbParam) {
      this.flHawb.set(hawbParam);
    }

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
    this.orderSvc.search(this.buildParams(), this.page(), this.pageSize())
    .pipe(takeUntilDestroyed(this.destroyRef))
    .subscribe({
      next: p => { this.orders.set(p.content); this.total.set(p.totalElements); this.loading.set(false); },
      error: ()  => this.loading.set(false),
    });
  }

  /** Duty : le backend stocke une fraction (0.10 = 10 %) → on divise par 100. */
  private buildParams(): OrderSearchParams {
    const num = (s: string) => (s.trim() !== '' ? Number(s) : undefined);
    const duty = this.flDutyRate().trim();
    return {
      hawb:            this.flHawb().trim()        || undefined,
      client:          this.flClient().trim()      || undefined,
      clientEmail:     this.flClientEmail().trim() || undefined,
      shipmentSearch:  this.flShipment().trim()    || undefined,
      customsCurrency: this.flCurrency()        || undefined,
      status:          this.flStatus()          || undefined,
      shipmentWeight:  num(this.flWeight()),
      customsValue:    num(this.flCustomsValue()),
      totalAmount:     num(this.flTotalAmount()),
      dutyRate:        duty !== '' ? Number(duty) / 100 : undefined,
      from:            this.flFrom() || undefined,
      to:              this.flTo()   || undefined,
    };
  }

  private loadKpis(): void {
    forkJoin({
      all:     this.orderSvc.search({}, 0, 1),
      noEmail: this.orderSvc.search({ status: 'CREATED' }, 0, 1),
      paid:    this.orderSvc.search({ status: 'PAID' }, 0, 1),
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: ({ all, noEmail, paid }) => {
        this.kpiTotal.set(all.totalElements);
        this.kpiNoEmail.set(noEmail.totalElements);
        this.kpiPaid.set(paid.totalElements);
      },
    });
  }

  /* ── Filtres ──────────────────────────────────────────── */
  setTab(tab: OrderStatus | ''): void {
    this.flStatus.set(tab);
    this.page.set(0);
    this.loadOrders();
  }

  onSearchClick(): void {
    this.page.set(0);
    this.loadOrders();
  }

  clearFilters(): void {
    this.flHawb.set('');
    this.flClient.set('');
    this.flClientEmail.set('');
    this.flShipment.set('');
    this.flStatus.set('');
    this.flCurrency.set('');
    this.flWeight.set('');
    this.flCustomsValue.set('');
    this.flTotalAmount.set('');
    this.flDutyRate.set('');
    this.flFrom.set('');
    this.flTo.set('');
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

  /* ── Email unitaire — envoi direct ───────────────────── */
  sendEmail(id: number): void {
    this.sendingId.set(id);
    this.emailSvc.sendToOrder(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.sendingId.set(null);
        if (res.success) {
          this.toast.success('Email envoyé avec succès.');
        } else {
          this.toast.error(res.message || 'Échec envoi email.');
        }
        this.loadOrders(); this.loadKpis();
      },
      error: err => {
        this.sendingId.set(null);
        this.toast.error(err?.error?.message || 'Erreur envoi email.');
      },
    });
  }

  /* ── Bulk actions ─────────────────────────────────────── */
  openBulkConfirm(action: 'email' | 'paid'): void {
    this.bulkConfirm.set(action);
  }
  closeBulkConfirm(): void { this.bulkConfirm.set(null); }

  executeBulkPaid(): void {
    const ids = Array.from(this.selectedIds());
    this.bulkLoading.set(true);
    this.bulkConfirm.set(null);
    this.orderSvc.bulkStatus(ids, 'PAID', 'Paiement enregistré en masse').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (r) => {
        this.bulkLoading.set(false);
        this.clearSelection();
        const msg = `${r.succeeded}/${r.total} order(s) marqués comme payés${r.failed > 0 ? ` — ${r.failed} échec(s)` : ''}.`;
        if (r.failed > 0) this.toast.error(msg);
        else              this.toast.success(msg);
        this.loadOrders(); this.loadKpis();
      },
      error: () => { this.bulkLoading.set(false); this.toast.error('Erreur lors de la mise à jour.'); },
    });
  }

  executeBulkEmail(): void {
    const ids = Array.from(this.selectedIds());
    this.bulkLoading.set(true);
    this.bulkConfirm.set(null);
    this.emailSvc.sendBulkEmail(ids).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: r => {
        this.bulkLoading.set(false);
        this.lastBulkResult.set(r);
        this.clearSelection();
        const msg = `${r.sent}/${r.total} email(s) envoyé(s)${r.failed > 0 ? ` — ${r.failed} échec(s)` : ''}.`;
        if (r.failed > 0) this.toast.error(msg);
        else              this.toast.success(msg);
        this.loadOrders(); this.loadKpis();
      },
      error: () => {
        this.bulkLoading.set(false);
        this.toast.error('Erreur lors de l\'envoi.');
      },
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
      error: (err) => { this.deleting.set(false); this.toast.error(err?.error?.message || 'Erreur.'); },
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
      PAID:            'or-row-paid',
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
