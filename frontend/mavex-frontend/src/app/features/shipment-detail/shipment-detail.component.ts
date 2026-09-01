import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, input, signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService }   from '../../core/services/layout.service';
import { ShipmentService } from '../../core/services/shipment.service';
import { ShipperService }  from '../../core/services/shipper.service';
import { OrderService }    from '../../core/services/order.service';
import { ClientService }   from '../../core/services/client.service';
import { EmailService }    from '../../core/services/email.service';
import { ToastService }    from '../../core/services/toast.service';
import { AuthService }     from '../../core/auth/auth.service';
import { MyPermissionsService } from '../../core/services/my-permissions.service';
import { BadgeComponent }  from '../../shared/badge/badge.component';
import { ShipmentResponse, ShipmentPatch, ShipmentStatus } from '../../core/models/shipment.model';
import { ShipperResponse } from '../../core/models/shipper.model';
import { OrderResponse, OrderRequest, OrderPatch, OrderStatus, OrderStatusUpdate } from '../../core/models/order.model';
import { ClientResponse }  from '../../core/models/client.model';

interface OrderColumnFilters {
  hawb?:       string;
  client?:     string;
  desc?:       string;
  weight?:     number;
  items?:      number;
  customsMin?: number;
  customsMax?: number;
  currency?:   string;
  totalMin?:   number;
  totalMax?:   number;
}

@Component({
  selector: 'app-shipment-detail',
  imports: [RouterLink, ReactiveFormsModule, BadgeComponent],
  templateUrl: './shipment-detail.component.html',
  styleUrl:    './shipment-detail.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShipmentDetailComponent implements OnInit {
  id = input.required<string>();

  private readonly layout     = inject(LayoutService);
  private readonly shipSvc    = inject(ShipmentService);
  private readonly shipperSvc = inject(ShipperService);
  private readonly orderSvc   = inject(OrderService);
  private readonly clientSvc  = inject(ClientService);
  private readonly emailSvc   = inject(EmailService);
  private readonly toast      = inject(ToastService);
  private readonly fb         = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth          = inject(AuthService);
  protected readonly myPermissions = inject(MyPermissionsService);

  /* ── Data ─────────────────────────────────────────────── */
  shipment  = signal<ShipmentResponse | null>(null);
  orders    = signal<OrderResponse[]>([]);
  clients   = signal<ClientResponse[]>([]);
  shippers  = signal<ShipperResponse[]>([]);
  loading   = signal(true);
  statusFilter = signal('');

  /* ── Panneau de filtres par colonne (100% local, aucun HTTP) ─── */
  filtersCollapsed = signal(false);
  advancedFiltersOpen = signal(false);
  flHawb        = signal('');
  flClient      = signal('');
  flDesc        = signal('');
  flWeight      = signal('');
  flItems       = signal('');
  flCustomsMin  = signal('');
  flCustomsMax  = signal('');
  flCurrency    = signal('');
  flTotalMin    = signal('');
  flTotalMax    = signal('');

  /** Filtres réellement appliqués — mis à jour uniquement au clic sur Search. */
  appliedFilters = signal<OrderColumnFilters>({});

  /* ── Duty edit ────────────────────────────────────────── */
  editingDuty   = signal(false);
  dutyRateInput = signal(0);
  savingDuty    = signal(false);

  /* ── Edit shipment ───────────────────────────────────── */
  editingShipment = signal(false);
  savingShipment  = signal(false);

  /* ── Email ────────────────────────────────────────────── */
  sendingId  = signal<number | null>(null);
  sendingAll = signal(false);

  /* ── Confirmation envoi groupé ─────────────────────────── */
  sendAllConfirm = signal(false);

  /* ── Create order ─────────────────────────────────────── */
  creatingOrder = signal(false);
  creating      = signal(false);

  /* ── Client autocomplete (create form) ───────────────── */
  clientSearch        = signal('');
  showClientDropdown  = signal(false);
  selectedClientName  = signal('');

  /* ── Mini formulaire nouveau client ──────────────────── */
  showNewClientForm  = signal(false);
  savingNewClient    = signal(false);
  newClientForm = this.fb.group({
    fullName:    ['', [Validators.required, Validators.minLength(2)]],
    email:       ['', [Validators.required, Validators.email]],
    phone:       [''],
    address:     [''],
    city:        [''],
    state:       ['', Validators.pattern(/^[A-Z]{0,2}$/)],
    zipCode:     [''],
    countryCode: ['MA', [Validators.required, Validators.pattern(/^[A-Z]{2}$/)]],
  });

  openNewClientForm(): void {
    this.showClientDropdown.set(false);
    this.newClientForm.reset({ countryCode: 'MA' });
    if (this.clientSearch()) {
      this.newClientForm.patchValue({ fullName: this.clientSearch() });
    }
    this.showNewClientForm.set(true);
  }

  closeNewClientForm(): void {
    this.showNewClientForm.set(false);
  }

  newClientFieldInvalid(f: string): boolean {
    const c = this.newClientForm.get(f);
    return !!(c?.invalid && c?.touched);
  }

  createAndSelectClient(): void {
    if (this.newClientForm.invalid) { this.newClientForm.markAllAsTouched(); return; }
    this.savingNewClient.set(true);
    const v = this.newClientForm.getRawValue();
    this.clientSvc.create({
      fullName:    v.fullName!,
      email:       v.email!,
      phone:       v.phone || undefined,
      address:     v.address || undefined,
      city:        v.city || undefined,
      state:       v.state || undefined,
      zipCode:     v.zipCode || undefined,
      countryCode: v.countryCode!,
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: created => {
        this.savingNewClient.set(false);
        this.showNewClientForm.set(false);
        this.clients.update(list => [...list, created]);
        this.orderForm.patchValue({ clientId: created.id });
        this.selectedClientName.set(created.fullName);
        this.clientSearch.set('');
        this.toast.success(`Client "${created.fullName}" créé et sélectionné.`);
      },
      error: err => {
        this.savingNewClient.set(false);
        this.toast.error(err?.error?.message || 'Erreur lors de la création du client.');
      },
    });
  }

  filteredClients = computed(() => {
    const q = this.clientSearch().toLowerCase().trim();
    if (!q) return this.clients().slice(0, 50);
    return this.clients().filter(c =>
      c.fullName?.toLowerCase().includes(q) ||
      c.email?.toLowerCase().includes(q) ||
      c.city?.toLowerCase().includes(q)
    ).slice(0, 50);
  });

  /* ── Client autocomplete (edit form) ─────────────────── */
  editClientSearch        = signal('');
  showEditClientDropdown  = signal(false);
  selectedEditClientName  = signal('');

  filteredEditClients = computed(() => {
    const q = this.editClientSearch().toLowerCase().trim();
    if (!q) return this.clients().slice(0, 50);
    return this.clients().filter(c =>
      c.fullName?.toLowerCase().includes(q) ||
      c.email?.toLowerCase().includes(q) ||
      c.city?.toLowerCase().includes(q)
    ).slice(0, 50);
  });

  /* ── Edit order ───────────────────────────────────────── */
  editingOrder = signal<OrderResponse | null>(null);
  savingOrder  = signal(false);

  /* ── Change status ────────────────────────────────────── */
  changingStatusOrder = signal<OrderResponse | null>(null);
  selectedNewStatus   = signal<OrderStatus | null>(null);
  statusNote          = signal('');
  changingStatus      = signal(false);
  confirmingPaidChange = signal(false);

  /* ── Delete order ─────────────────────────────────────── */
  deleteOrderId = signal<number | null>(null);
  deletingOrder = signal(false);

  /* ── Status options ───────────────────────────────────── */
  readonly statusOptions: ReadonlyArray<{ value: OrderStatus; label: string; color: string; bg: string }> = [
    { value: 'CREATED',          label: 'Created',                color: '#6B7280', bg: '#F3F4F6' },
    { value: 'EMAIL_SENT',       label: 'Email envoyé',           color: '#3B82F6', bg: '#EFF6FF' },
    { value: 'EMAIL_OUTDATED',   label: 'Email obsolète',         color: '#F97316', bg: '#FFF7ED' },
    { value: 'PAID',             label: 'Payé',                   color: '#22C55E', bg: '#F0FDF4' },
    { value: 'CANCELLED',        label: 'Annulé',                 color: '#EF4444', bg: '#FEF2F2' },
  ];

  /* ── Computed ─────────────────────────────────────────── */
  filteredOrders = computed(() => {
    const st = this.statusFilter();
    const f  = this.appliedFilters();

    return this.orders().filter(o => {
      // 1. Statut (onglets)
      if (st && o.status !== st) return false;

      // 2. Filtres par colonne
      if (f.hawb   && !o.hawb?.toLowerCase().includes(f.hawb))             return false;
      if (f.client && !o.clientFullName?.toLowerCase().includes(f.client)) return false;
      if (f.desc   && !o.goodsDescription?.toLowerCase().includes(f.desc)) return false;

      if (f.weight   != null && o.shipmentWeight !== f.weight) return false;
      if (f.items    != null && o.numberOfItems  !== f.items)  return false;
      if (f.currency && o.customsCurrency !== f.currency)      return false;

      if (f.customsMin != null && (o.customsValue ?? 0) < f.customsMin) return false;
      if (f.customsMax != null && (o.customsValue ?? 0) > f.customsMax) return false;
      if (f.totalMin   != null && (o.totalAmount  ?? 0) < f.totalMin)   return false;
      if (f.totalMax   != null && (o.totalAmount  ?? 0) > f.totalMax)   return false;

      return true;
    });
  });

  /** Un filtre de colonne est-il réellement appliqué ? (source de vérité : appliedFilters) */
  hasColumnFilters = computed(() =>
    Object.values(this.appliedFilters()).some(v => v !== undefined)
  );

  /* ── Sélection bulk (basée sur filteredOrders, pas orders) ──── */
  selectedIds  = signal<Set<number>>(new Set());
  someSelected = computed(() => this.selectedIds().size > 0);
  allSelected  = computed(() => this.filteredOrders().length > 0 && this.selectedIds().size === this.filteredOrders().length);
  selCount     = computed(() => this.selectedIds().size);

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
      : this.selectedIds.set(new Set(this.filteredOrders().map(o => o.id)));
  }

  isSelected(id: number): boolean { return this.selectedIds().has(id); }
  clearSelection(): void          { this.selectedIds.set(new Set()); }

  /* ── Export ───────────────────────────────────────────── */
  exportOpen = signal(false);

  /* ── Panneau de filtres ───────────────────────────────── */
  onFiltersSearch(): void {
    const num = (s: string) => (s.trim() !== '' ? Number(s) : undefined);
    this.appliedFilters.set({
      hawb:       this.flHawb().trim().toLowerCase()   || undefined,
      client:     this.flClient().trim().toLowerCase() || undefined,
      desc:       this.flDesc().trim().toLowerCase()   || undefined,
      weight:     num(this.flWeight()),
      items:      num(this.flItems()),
      customsMin: num(this.flCustomsMin()),
      customsMax: num(this.flCustomsMax()),
      currency:   this.flCurrency() || undefined,
      totalMin:   num(this.flTotalMin()),
      totalMax:   num(this.flTotalMax()),
    });
  }

  onFiltersReset(): void {
    this.flHawb.set('');
    this.flClient.set('');
    this.flDesc.set('');
    this.flWeight.set('');
    this.flItems.set('');
    this.flCurrency.set('');
    this.flCustomsMin.set('');
    this.flCustomsMax.set('');
    this.flTotalMin.set('');
    this.flTotalMax.set('');
    this.appliedFilters.set({});
  }

  totalOrders     = computed(() => this.orders().length);
  createdCount    = computed(() => this.orders().filter(o => o.status === 'CREATED').length);
  emailCount      = computed(() => this.orders().filter(o => o.status === 'EMAIL_SENT').length);
  paidCount       = computed(() => this.orders().filter(o => o.status === 'PAID').length);
  emailOutdatedCount = computed(() => this.orders().filter(o => o.status === 'EMAIL_OUTDATED').length);
  cancelledCount     = computed(() => this.orders().filter(o => o.status === 'CANCELLED').length);
  eligibleForEmailCount = computed(() =>
    this.orders().filter(o =>
      o.status === 'CREATED' || o.status === 'EMAIL_SENT' || o.status === 'EMAIL_OUTDATED'
    ).length
  );

  /* ── Financial totals ─────────────────────────────────── */
  totalWeight  = computed(() => Math.round(this.orders().reduce((s, o) => s + (o.shipmentWeight ?? 0), 0) * 100) / 100);
  totalCustoms = computed(() => Math.round(this.orders().reduce((s, o) => s + (o.customsValue   ?? 0), 0) * 100) / 100);
  totalDuty    = computed(() => Math.round(this.orders().reduce((s, o) => s + (o.dutyAmount     ?? 0), 0) * 100) / 100);
  grandTotal   = computed(() => Math.round(this.orders().reduce((s, o) => s + (o.totalAmount    ?? 0), 0) * 100) / 100);

  /* ── Forms ────────────────────────────────────────────── */
  orderForm = this.fb.group({
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

  /* ── Shipment edit form ───────────────────────────────── */
  shipmentEditForm = this.fb.group({
    mawb:             ['', [Validators.required, Validators.minLength(3)]],
    shipperId:        [null as number | null],
    exportDate:       [''],
    importDate:       [''],
    importingCarrier: [''],
    modeOfTransport:  [''],
    portCode:         [''],
    status:           ['DRAFT' as ShipmentStatus, Validators.required],
  });

  /* ── Lifecycle ────────────────────────────────────────── */
  ngOnInit(): void {
    this.layout.setPage('Shipment Detail');
    this.loadData();
    this.loadClients();
    this.loadShippers();
  }

  /* ── Load ─────────────────────────────────────────────── */
  private loadData(): void {
    const sid = Number(this.id());
    this.loading.set(true);
    this.shipSvc.getById(sid).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: s => {
        this.shipment.set(s);
        this.dutyRateInput.set(Math.round((s.dutyRate ?? 0) * 10000) / 100);
        this.loading.set(false);
      },
      error: () => this.loading.set(false),
    });
    this.orderSvc.getByShipment(sid).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: list => this.orders.set(list),
    });
  }

  private loadClients(): void {
    this.clientSvc.getAllActive().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: list => this.clients.set(list),
      error: () => {},
    });
  }

  private loadShippers(): void {
    this.shipperSvc.getAllActive().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: list => this.shippers.set(list),
      error: () => {},
    });
  }

  /* ── Duty rate ────────────────────────────────────────── */
  startEditDuty(): void  { this.editingDuty.set(true); }
  cancelEditDuty(): void {
    this.editingDuty.set(false);
    this.dutyRateInput.set(Math.round((this.shipment()?.dutyRate ?? 0) * 10000) / 100);
  }

  saveDuty(): void {
    const s = this.shipment();
    if (!s) return;
    const pct = this.dutyRateInput();
    if (pct < 0 || pct > 100) {
      this.toast.error('Le taux douanier doit être entre 0% et 100%');
      return;
    }
    this.savingDuty.set(true);
    this.shipSvc.updateDutyRate(s.id, { dutyRate: pct / 100 })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.shipment.set(updated);
          this.savingDuty.set(false);
          this.editingDuty.set(false);
          this.toast.success('Duty rate mis à jour.');
          this.reloadOrders();
        },
        error: err => {
          this.savingDuty.set(false);
          this.toast.error(err?.error?.message || 'Erreur.');
        },
      });
  }

  /* ── Emails ───────────────────────────────────────────── */
  sendEmail(orderId: number): void {
    this.sendingId.set(orderId);
    this.emailSvc.sendToOrder(orderId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.sendingId.set(null);
        if (res.success) {
          this.toast.success('Email envoyé.');
        } else {
          this.toast.error(res.message || 'Échec envoi email.');
        }
        this.reloadOrders();
      },
      error: err => {
        this.sendingId.set(null);
        this.toast.error(err?.error?.message || 'Erreur.');
      },
    });
  }

  sendAll(): void {
    this.sendAllConfirm.set(true);
  }

  closeSendAllConfirm(): void { this.sendAllConfirm.set(false); }

  executeSendAll(): void {
    const shipment = this.shipment();
    if (!shipment) return;
    this.sendAllConfirm.set(false);
    this.sendingAll.set(true);
    this.emailSvc.sendToShipment(shipment.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.sendingAll.set(false);
        if (res.failed > 0) {
          this.toast.error(`${res.sent}/${res.total} email(s) envoyé(s) — ${res.failed} échec(s).`);
        } else {
          this.toast.success(`${res.sent}/${res.total} email(s) envoyé(s).`);
        }
        this.reloadOrders();
      },
      error: err => {
        this.sendingAll.set(false);
        this.toast.error(err?.error?.message || 'Erreur.');
      },
    });
  }

  /* ── Client autocomplete helpers ─────────────────────── */
  selectClient(c: ClientResponse): void {
    this.orderForm.patchValue({ clientId: c.id });
    this.selectedClientName.set(c.fullName);
    this.clientSearch.set('');
    this.showClientDropdown.set(false);
  }

  clearClient(): void {
    this.orderForm.patchValue({ clientId: null });
    this.selectedClientName.set('');
    this.clientSearch.set('');
  }

  onClientSearchInput(val: string): void {
    this.clientSearch.set(val);
    this.showClientDropdown.set(true);
    if (!val) this.clearClient();
  }

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

  /* ── Create Order ─────────────────────────────────────── */
  openCreateOrder(): void {
    const s = this.shipment();
    const defaultDuty = s?.dutyRate != null ? Math.round(s.dutyRate * 10000) / 100 : null;
    this.orderForm.reset({
      hawb: '', clientId: null, numberOfItems: null,
      goodsDescription: '', shipmentWeight: null, htsusCode: '',
      customsValue: null, customsCurrency: 'USD',
      dutyRate: defaultDuty,
      bankCharges: null,
    });
    this.selectedClientName.set('');
    this.clientSearch.set('');
    this.showClientDropdown.set(false);
    this.creatingOrder.set(true);
  }
  closeCreateOrder(): void { this.creatingOrder.set(false); }

  submitOrder(): void {
    if (this.orderForm.invalid) { this.orderForm.markAllAsTouched(); return; }
    const s = this.shipment();
    if (!s) return;
    this.creating.set(true);
    const v = this.orderForm.value;
    const req: OrderRequest = {
      hawb:             v.hawb!,
      shipmentId:       s.id,
      clientId:         v.clientId!,
      numberOfItems:    v.numberOfItems ?? undefined,
      goodsDescription: v.goodsDescription || undefined,
      shipmentWeight:   v.shipmentWeight ?? undefined,
      htsusCode:        v.htsusCode || undefined,
      customsValue:     v.customsValue ?? undefined,
      customsCurrency:  v.customsCurrency || undefined,
      dutyRate:         v.dutyRate != null ? v.dutyRate / 100 : undefined,
      bankCharges:      v.bankCharges ?? undefined,
    };
    this.orderSvc.create(req).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.creating.set(false);
        this.creatingOrder.set(false);
        this.toast.success('Order créé avec succès.');
        this.reloadOrders();
      },
      error: err => {
        this.creating.set(false);
        this.toast.error(err?.error?.message || 'Erreur lors de la création.');
      },
    });
  }

  orderFieldInvalid(name: string): boolean {
    const c = this.orderForm.get(name);
    return !!(c?.invalid && c?.touched);
  }

  /* ── Edit Order ───────────────────────────────────────── */
  openEditOrder(o: OrderResponse): void {
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
        this.toast.success('Order mis à jour.');
        this.reloadOrders();
      },
      error: err => {
        this.savingOrder.set(false);
        this.toast.error(err?.error?.message || 'Erreur.');
      },
    });
  }

  editOrderFieldInvalid(name: string): boolean {
    const c = this.editOrderForm.get(name);
    return !!(c?.invalid && c?.touched);
  }

  fmtDuty(rate: number | null | undefined): string {
    if (rate == null) return '—';
    return Math.round(rate * 10000) / 100 + ' %';
  }

  /* ── Change Status ────────────────────────────────────── */
  openStatusModal(o: OrderResponse): void {
    this.changingStatusOrder.set(o);
    this.selectedNewStatus.set(null);
    this.statusNote.set('');
    this.confirmingPaidChange.set(false);
  }
  closeStatusModal(): void {
    this.changingStatusOrder.set(null);
    this.confirmingPaidChange.set(false);
  }

  cancelPaidConfirm(): void { this.confirmingPaidChange.set(false); }

  confirmStatusChange(): void {
    const o      = this.changingStatusOrder();
    const status = this.selectedNewStatus();
    if (!o || !status) return;
    if (status === 'PAID' && !this.confirmingPaidChange()) {
      this.confirmingPaidChange.set(true);
      return;
    }
    this.changingStatus.set(true);
    const req: OrderStatusUpdate = { newStatus: status, note: this.statusNote() || undefined };
    this.orderSvc.updateStatus(o.id, req).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.changingStatus.set(false);
        this.changingStatusOrder.set(null);
        this.confirmingPaidChange.set(false);
        this.toast.success('Statut mis à jour.');
        this.reloadOrders();
      },
      error: err => {
        this.changingStatus.set(false);
        this.toast.error(err?.error?.message || 'Erreur.');
      },
    });
  }

  /* ── Delete Order ─────────────────────────────────────── */
  confirmDeleteOrder(id: number): void { this.deleteOrderId.set(id); }
  cancelDeleteOrder(): void            { this.deleteOrderId.set(null); }

  executeDeleteOrder(): void {
    const id = this.deleteOrderId();
    if (id == null) return;
    this.deletingOrder.set(true);
    this.orderSvc.delete(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.deletingOrder.set(false);
        this.deleteOrderId.set(null);
        this.toast.success('Order supprimé.');
        this.reloadOrders();
      },
      error: err => {
        this.deletingOrder.set(false);
        this.toast.error(err?.error?.message || 'Erreur.');
      },
    });
  }

  private reloadOrders(): void {
    const s = this.shipment();
    if (!s) return;
    this.orderSvc.getByShipment(s.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: list => this.orders.set(list),
    });
  }

  /* ── Status filter ────────────────────────────────────── */
  setStatusFilter(value: string): void {
    this.statusFilter.set(this.statusFilter() === value ? '' : value);
  }

  /* ── Edit Shipment ────────────────────────────────────── */
  openEditShipment(): void {
    const s = this.shipment();
    if (!s) return;
    this.shipmentEditForm.patchValue({
      mawb:             s.mawb,
      shipperId:        s.shipper?.id ?? null,
      exportDate:       s.exportDate ?? '',
      importDate:       s.importDate ?? '',
      importingCarrier: s.importingCarrier ?? '',
      modeOfTransport:  s.modeOfTransport ?? '',
      portCode:         s.portCode ?? '',
      status:           s.status,
    });
    this.editingShipment.set(true);
  }

  closeEditShipment(): void { this.editingShipment.set(false); }

  saveShipment(): void {
    if (this.shipmentEditForm.invalid) { this.shipmentEditForm.markAllAsTouched(); return; }
    const s = this.shipment();
    if (!s) return;
    this.savingShipment.set(true);
    const v = this.shipmentEditForm.value;
    const req: ShipmentPatch = {
      mawb:             v.mawb!,
      shipperId:        v.shipperId ?? null,
      exportDate:       v.exportDate || undefined,
      importDate:       v.importDate || undefined,
      importingCarrier: v.importingCarrier || undefined,
      modeOfTransport:  v.modeOfTransport || undefined,
      portCode:         v.portCode || undefined,
      status:           v.status as ShipmentStatus,
    };
    this.shipSvc.patch(s.id, req).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: updated => {
        this.shipment.set(updated);
        this.savingShipment.set(false);
        this.editingShipment.set(false);
        this.toast.success('Shipment mis à jour.');
      },
      error: err => {
        this.savingShipment.set(false);
        this.toast.error(err?.error?.message || 'Erreur.');
      },
    });
  }

  shipmentFieldInvalid(name: string): boolean {
    const c = this.shipmentEditForm.get(name);
    return !!(c?.invalid && c?.touched);
  }

  /* ── Permissions (Shipment uniquement — pas les Orders) ──── */
  canUpdateShipment(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('SHIPMENTS', 'UPDATE');
  }

  canUpdateDutyRate(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('SHIPMENTS', 'UPDATE_DUTY_RATE');
  }

  /* ── Permissions (Orders rattachés à ce shipment) ────────── */
  canCreateOrder(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('ORDERS', 'CREATE');
  }

  canUpdateOrder(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('ORDERS', 'UPDATE');
  }

  canDeleteOrder(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('ORDERS', 'DELETE');
  }

  canChangeOrderStatus(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('ORDERS', 'CHANGE_STATUS');
  }

  canSendEmail(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('ORDERS', 'SEND_EMAIL');
  }

  canBulkSendEmail(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('ORDERS', 'BULK_SEND_EMAIL');
  }

  canExportOrders(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('ORDERS', 'EXPORT');
  }

  /* ── Clipboard ────────────────────────────────────────── */
  copiedText = signal<string | null>(null);

  copyToClipboard(text: string): void {
    navigator.clipboard.writeText(text).then(() => {
      this.copiedText.set(text);
      setTimeout(() => this.copiedText.set(null), 1500);
    });
  }

  /* ── Helpers ──────────────────────────────────────────── */
  fmtNum(n: number): string {
    if (n === 0) return '0';
    return n % 1 === 0 ? n.toString() : n.toFixed(2);
  }

  fmtDate(d: string | null | undefined): string {
    if (!d) return '—';
    const date = new Date(d);
    if (isNaN(date.getTime())) return d;
    return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  /* ── Export ───────────────────────────────────────────── */
  exportAll(): void {
    const ids = this.filteredOrders().map(o => o.id);
    if (ids.length === 0) {
      this.toast.error('Aucune commande à exporter.');
      return;
    }
    this.exportOpen.set(false);
    this.orderSvc.exportDetailPdf(ids)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => this.triggerDownload(blob, 'commandes_shipment.pdf'),
        error: () => this.toast.error('Erreur lors de l\'export PDF.'),
      });
  }

  exportAllExcel(): void {
    const ids = this.filteredOrders().map(o => o.id);
    if (ids.length === 0) {
      this.toast.error('Aucune commande à exporter.');
      return;
    }
    this.exportOpen.set(false);
    this.orderSvc.exportDetailExcel(ids)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => this.triggerDownload(blob, 'commandes_shipment.xlsx'),
        error: () => this.toast.error('Erreur lors de l\'export Excel.'),
      });
  }

  exportSelection(): void {
    if (this.selectedIds().size === 0) {
      this.toast.error('Veuillez sélectionner au moins une commande.');
      return;
    }
    this.exportOpen.set(false);
    this.orderSvc.exportDetailExcel(Array.from(this.selectedIds()))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => this.triggerDownload(blob, 'commandes_selection.xlsx'),
        error: () => this.toast.error('Erreur lors de l\'export de la sélection.'),
      });
  }

  private triggerDownload(blob: Blob, filename: string): void {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = filename;
    document.body.appendChild(a);
    a.click();
    a.remove();
    URL.revokeObjectURL(url);
  }
}
