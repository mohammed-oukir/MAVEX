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
import { BadgeComponent }  from '../../shared/badge/badge.component';
import { ShipmentResponse, ShipmentPatch, ShipmentStatus } from '../../core/models/shipment.model';
import { ShipperResponse } from '../../core/models/shipper.model';
import { OrderResponse, OrderRequest, OrderPatch, OrderStatus, OrderStatusUpdate } from '../../core/models/order.model';
import { ClientResponse }  from '../../core/models/client.model';

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

  /* ── Data ─────────────────────────────────────────────── */
  shipment  = signal<ShipmentResponse | null>(null);
  orders    = signal<OrderResponse[]>([]);
  clients   = signal<ClientResponse[]>([]);
  shippers  = signal<ShipperResponse[]>([]);
  loading   = signal(true);
  search    = signal('');
  statusFilter = signal('');

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

  /* ── Create order ─────────────────────────────────────── */
  creatingOrder = signal(false);
  creating      = signal(false);

  /* ── Edit order ───────────────────────────────────────── */
  editingOrder = signal<OrderResponse | null>(null);
  savingOrder  = signal(false);

  /* ── Change status ────────────────────────────────────── */
  changingStatusOrder = signal<OrderResponse | null>(null);
  selectedNewStatus   = signal<OrderStatus | null>(null);
  statusNote          = signal('');
  changingStatus      = signal(false);

  /* ── Delete order ─────────────────────────────────────── */
  deleteOrderId = signal<number | null>(null);
  deletingOrder = signal(false);

  /* ── Status options ───────────────────────────────────── */
  readonly statusOptions: ReadonlyArray<{ value: OrderStatus; label: string; color: string; bg: string }> = [
    { value: 'CREATED',          label: 'Created',                color: '#6B7280', bg: '#F3F4F6' },
    { value: 'EMAIL_SENT',       label: 'Email envoyé',           color: '#3B82F6', bg: '#EFF6FF' },
    { value: 'PENDING_PAYMENT',  label: 'En attente de paiement', color: '#F59E0B', bg: '#FFFBEB' },
    { value: 'PAID',             label: 'Payé',                   color: '#22C55E', bg: '#F0FDF4' },
    { value: 'IN_DELIVERY',      label: 'En livraison',           color: '#F97316', bg: '#FFF7ED' },
    { value: 'DELIVERED',        label: 'Livré',                  color: '#22C55E', bg: '#F0FDF4' },
    { value: 'CANCELLED',        label: 'Annulé',                 color: '#EF4444', bg: '#FEF2F2' },
  ];

  /* ── Computed ─────────────────────────────────────────── */
  filteredOrders = computed(() => {
    const q  = this.search().toLowerCase().trim();
    const st = this.statusFilter();
    return this.orders().filter(o => {
      if (st && o.status !== st) return false;
      if (!q) return true;
      return (
        o.hawb?.toLowerCase().includes(q) ||
        o.clientFullName?.toLowerCase().includes(q) ||
        o.goodsDescription?.toLowerCase().includes(q) ||
        o.clientEmail?.toLowerCase().includes(q) ||
        o.clientCity?.toLowerCase().includes(q) ||
        o.htsusCode?.toLowerCase().includes(q)
      );
    });
  });

  totalOrders     = computed(() => this.orders().length);
  createdCount    = computed(() => this.orders().filter(o => o.status === 'CREATED').length);
  emailCount      = computed(() => this.orders().filter(o => o.status === 'EMAIL_SENT').length);
  pendingCount    = computed(() => this.orders().filter(o => o.status === 'PENDING_PAYMENT').length);
  paidCount       = computed(() => this.orders().filter(o => o.status === 'PAID').length);
  inDeliveryCount = computed(() => this.orders().filter(o => o.status === 'IN_DELIVERY').length);
  deliveredCount  = computed(() => this.orders().filter(o => o.status === 'DELIVERED').length);

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
    customsValue:     [null as number | null],
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
    customsValue:     [null as number | null],
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
        this.layout.setPage(s.mawb);
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
    this.shipperSvc.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
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
    this.savingDuty.set(true);
    this.shipSvc.updateDutyRate(s.id, { dutyRate: this.dutyRateInput() / 100 })
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
      next: () => { this.sendingId.set(null); this.toast.success('Email envoyé.'); this.reloadOrders(); },
      error: err => { this.sendingId.set(null); this.toast.error(err?.error?.message || 'Erreur.'); },
    });
  }

  sendAll(): void {
    const s = this.shipment();
    if (!s) return;
    this.sendingAll.set(true);
    this.emailSvc.sendToShipment(s.id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { this.sendingAll.set(false); this.toast.success('Emails envoyés.'); this.reloadOrders(); },
      error: err => { this.sendingAll.set(false); this.toast.error(err?.error?.message || 'Erreur.'); },
    });
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
  }
  closeStatusModal(): void { this.changingStatusOrder.set(null); }

  confirmStatusChange(): void {
    const o      = this.changingStatusOrder();
    const status = this.selectedNewStatus();
    if (!o || !status) return;
    this.changingStatus.set(true);
    const req: OrderStatusUpdate = { newStatus: status, note: this.statusNote() || undefined };
    this.orderSvc.updateStatus(o.id, req).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.changingStatus.set(false);
        this.changingStatusOrder.set(null);
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
}
