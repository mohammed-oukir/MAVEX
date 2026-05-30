import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService } from '../../core/services/layout.service';
import { OrderService } from '../../core/services/order.service';
import { ShipmentService } from '../../core/services/shipment.service';
import { ClientService } from '../../core/services/client.service';
import { EmailService } from '../../core/services/email.service';
import { ToastService } from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/badge/badge.component';
import { OrderResponse, OrderRequest } from '../../core/models/order.model';
import { ShipmentResponse } from '../../core/models/shipment.model';
import { ClientResponse } from '../../core/models/client.model';

interface OrderGroup {
  mawb:       string;
  shipmentId: number;
  orders:     OrderResponse[];
}

@Component({
  selector: 'app-orders',
  imports: [ReactiveFormsModule, BadgeComponent],
  templateUrl: './orders.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class OrdersComponent implements OnInit {
  private readonly layout     = inject(LayoutService);
  private readonly orderSvc   = inject(OrderService);
  private readonly shipSvc    = inject(ShipmentService);
  private readonly clientSvc  = inject(ClientService);
  private readonly emailSvc   = inject(EmailService);
  private readonly toast      = inject(ToastService);
  private readonly fb         = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  orders      = signal<OrderResponse[]>([]);
  shipments   = signal<ShipmentResponse[]>([]);
  clients     = signal<ClientResponse[]>([]);
  loading     = signal(true);
  search      = signal('');
  statusFilter = signal('');
  expanded    = signal<Set<string>>(new Set());

  showModal   = signal(false);
  editingId   = signal<number | null>(null);
  saving      = signal(false);
  deleteId    = signal<number | null>(null);
  deleting    = signal(false);
  sendingId   = signal<number | null>(null);
  sendingAll  = signal<number | null>(null);

  orderForm = this.fb.group({
    shipmentId:       [null as number | null, Validators.required],
    clientId:         [null as number | null, Validators.required],
    hawb:             ['', Validators.required],
    numberOfItems:    [null as number | null],
    goodsDescription: [''],
    shipmentWeight:   [null as number | null],
    htsusCode:        [''],
    customsValue:     [null as number | null],
    customsCurrency:  ['USD'],
    dutyRate:         [null as number | null],
    bankCharges:      [0],
  });

  filteredOrders = computed(() => {
    const q = this.search().toLowerCase();
    const s = this.statusFilter();
    return this.orders().filter(o => {
      const matchQ = !q || (o.hawb?.toLowerCase().includes(q) || o.clientFullName?.toLowerCase().includes(q) || o.mawb?.toLowerCase().includes(q));
      const matchS = !s || o.status === s;
      return matchQ && matchS;
    });
  });

  groups = computed<OrderGroup[]>(() => {
    const map = new Map<string, OrderGroup>();
    for (const o of this.filteredOrders()) {
      const key = o.mawb || 'Sans MAWB';
      if (!map.has(key)) map.set(key, { mawb: key, shipmentId: o.shipmentId ?? 0, orders: [] });
      map.get(key)!.orders.push(o);
    }
    return Array.from(map.values());
  });

  totalOrders   = computed(() => this.orders().length);
  paidCount     = computed(() => this.orders().filter(o => o.status === 'PAID').length);
  pendingCount  = computed(() => this.orders().filter(o => o.status === 'PENDING_PAYMENT').length);
  emailCount    = computed(() => this.orders().filter(o => o.status === 'EMAIL_SENT').length);

  ngOnInit(): void {
    this.layout.setPage('Orders', { label: 'Nouvel order', onClick: () => this.openCreate() });
    this.loadOrders();
    this.loadDropdowns();
  }

  toggleGroup(mawb: string): void {
    this.expanded.update(s => {
      const n = new Set(s);
      n.has(mawb) ? n.delete(mawb) : n.add(mawb);
      return n;
    });
  }

  isExpanded(mawb: string): boolean { return this.expanded().has(mawb); }

  openCreate(): void {
    this.editingId.set(null);
    this.orderForm.reset({ customsCurrency: 'USD', bankCharges: 0 });
    this.showModal.set(true);
  }

  openEdit(o: OrderResponse): void {
    this.editingId.set(o.id);
    this.orderForm.patchValue({
      shipmentId: o.shipmentId ?? null,
      clientId:   o.clientId ?? null,
      hawb:       o.hawb,
      numberOfItems: o.numberOfItems ?? null,
      goodsDescription: o.goodsDescription ?? '',
      shipmentWeight:   o.shipmentWeight ?? null,
      htsusCode:        o.htsusCode ?? '',
      customsValue:     o.customsValue ?? null,
      customsCurrency:  o.customsCurrency ?? 'USD',
      dutyRate:         o.dutyRate ?? null,
      bankCharges:      o.bankCharges ?? 0,
    });
    this.showModal.set(true);
  }

  closeModal(): void { this.showModal.set(false); }

  save(): void {
    if (this.orderForm.invalid) { this.orderForm.markAllAsTouched(); return; }
    this.saving.set(true);
    const v    = this.orderForm.value;
    const req: OrderRequest = {
      hawb:             v.hawb!,
      shipmentId:       v.shipmentId!,
      clientId:         v.clientId!,
      numberOfItems:    v.numberOfItems ?? undefined,
      goodsDescription: v.goodsDescription ?? undefined,
      shipmentWeight:   v.shipmentWeight ?? undefined,
      htsusCode:        v.htsusCode ?? undefined,
      customsValue:     v.customsValue ?? undefined,
      customsCurrency:  v.customsCurrency ?? undefined,
      dutyRate:         v.dutyRate ?? undefined,
      bankCharges:      v.bankCharges ?? undefined,
    };
    const id = this.editingId();
    const call = id ? this.orderSvc.update(id, req) : this.orderSvc.create(req);
    call.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving.set(false);
        this.showModal.set(false);
        this.toast.success(id ? 'Order mis à jour.' : 'Order créé.');
        this.loadOrders();
      },
      error: err => { this.saving.set(false); this.toast.error(err?.error?.message || 'Erreur.'); },
    });
  }

  confirmDelete(id: number): void { this.deleteId.set(id); }
  cancelDelete(): void            { this.deleteId.set(null); }

  executeDelete(): void {
    const id = this.deleteId();
    if (!id) return;
    this.deleting.set(true);
    this.orderSvc.delete(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { this.deleting.set(false); this.deleteId.set(null); this.toast.success('Order supprimé.'); this.loadOrders(); },
      error: () => { this.deleting.set(false); this.toast.error('Erreur.'); },
    });
  }

  sendEmail(id: number): void {
    this.sendingId.set(id);
    this.emailSvc.sendToOrder(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { this.sendingId.set(null); this.toast.success('Email envoyé.'); this.loadOrders(); },
      error: err => { this.sendingId.set(null); this.toast.error(err?.error?.message || 'Erreur envoi email.'); },
    });
  }

  sendAll(shipmentId: number): void {
    this.sendingAll.set(shipmentId);
    this.emailSvc.sendToShipment(shipmentId).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => { this.sendingAll.set(null); this.toast.success('Emails envoyés.'); this.loadOrders(); },
      error: err => { this.sendingAll.set(null); this.toast.error(err?.error?.message || 'Erreur.'); },
    });
  }

  fieldInvalid(name: string): boolean {
    const c = this.orderForm.get(name);
    return !!(c?.invalid && c?.touched);
  }

  private loadOrders(): void {
    this.loading.set(true);
    this.orderSvc.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: list => { this.orders.set(list); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  private loadDropdowns(): void {
    this.shipSvc.getAllUnpaged().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: p => this.shipments.set(p.content) });
    this.clientSvc.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: list => this.clients.set(list) });
  }
}
