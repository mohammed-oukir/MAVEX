import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService }       from '../../core/services/layout.service';
import { ShipmentService }     from '../../core/services/shipment.service';
import { OrderService }        from '../../core/services/order.service';
import { ToastService }        from '../../core/services/toast.service';
import { PaymentAdminService } from '../../core/services/payment-admin.service';
import { ShipmentResponse }    from '../../core/models/shipment.model';
import { OrderResponse }       from '../../core/models/order.model';
import {
  GatewayConfigResponse, GatewayConfigRequest,
  PaymentTransactionResponse,
} from '../../core/models/payment.model';

type ActiveTab = 'manifests' | 'gateways';

@Component({
  selector: 'app-payments',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './payments.component.html',
  styleUrl: './payments.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentsComponent implements OnInit {
  private readonly layout     = inject(LayoutService);
  private readonly shipSvc    = inject(ShipmentService);
  private readonly orderSvc   = inject(OrderService);
  private readonly payAdm     = inject(PaymentAdminService);
  private readonly toast      = inject(ToastService);
  private readonly fb         = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  /* ── Tabs ─────────────────────────────────────────── */
  activeTab = signal<ActiveTab>('manifests');

  /* ── Manifests data ───────────────────────────────── */
  allShipments  = signal<ShipmentResponse[]>([]);
  loadingShips  = signal(true);
  statusFilter  = signal<string>('');
  searchQ       = signal('');
  expandedOrders = signal<Map<number, OrderResponse[]>>(new Map());
  loadingOrders  = signal<Set<number>>(new Set());

  /* ── Gateways data ────────────────────────────────── */
  gateways     = signal<GatewayConfigResponse[]>([]);
  loadingGways = signal(true);
  togglingId   = signal<number | null>(null);
  deletingId   = signal<number | null>(null);

  /* ── Transaction modal ────────────────────────────── */
  txModal       = signal<PaymentTransactionResponse[] | null>(null);
  txModalHawb   = signal('');
  loadingTx     = signal(false);

  /* ── Gateway form modal ───────────────────────────── */
  gwFormMode = signal<'create' | 'edit' | null>(null);
  gwEditingId = signal<number | null>(null);
  gwSaving    = signal(false);

  gwForm = this.fb.group({
    type:               ['PAYPAL', Validators.required],
    name:               ['', [Validators.required, Validators.minLength(3)]],
    apiKey:             [''],
    secretKey:          [''],
    webhookSecret:      [''],
    mode:               ['TEST', Validators.required],
    supportedCurrencies:['USD'],
    description:        [''],
  });

  /* ── KPIs ─────────────────────────────────────────── */
  kpiTotal     = computed(() => this.allShipments().length);
  kpiActive    = computed(() => this.allShipments().filter(s => s.status !== 'CLOSED').length);
  kpiClosed    = computed(() => this.allShipments().filter(s => s.status === 'CLOSED').length);
  kpiGateway   = computed(() => this.gateways().find(g => g.active)?.type ?? 'AUCUN');

  /* ── Filtered shipments ───────────────────────────── */
  filtered = computed(() => {
    const q  = this.searchQ().toLowerCase();
    const st = this.statusFilter();
    return this.allShipments().filter(s => {
      if (st && s.status !== st) return false;
      if (q && !s.mawb.toLowerCase().includes(q) &&
          !(s.shipper?.companyName ?? '').toLowerCase().includes(q)) return false;
      return true;
    });
  });

  /* ── Per-shipment stats ───────────────────────────── */
  shipmentStats = computed(() => {
    const map = new Map<number, {
      paid: number; pending: number; email: number;
      collected: number; total: number; pct: number;
    }>();
    for (const [id, orders] of this.expandedOrders()) {
      const paid      = orders.filter(o => o.status === 'PAID').length;
      const pending   = orders.filter(o => o.status === 'PENDING_PAYMENT').length;
      const email     = orders.filter(o => o.status === 'EMAIL_SENT').length;
      const collected = orders.filter(o => o.status === 'PAID')
                              .reduce((s, o) => s + (o.totalAmount ?? 0), 0);
      const total     = orders.reduce((s, o) => s + (o.totalAmount ?? 0), 0);
      const pct       = orders.length > 0 ? Math.round((paid / orders.length) * 100) : 0;
      map.set(id, { paid, pending, email, collected, total, pct });
    }
    return map;
  });

  /* ── Lifecycle ────────────────────────────────────── */
  ngOnInit(): void {
    this.layout.setPage('Paiements');
    this.loadShipments();
    this.loadGateways();
  }

  /* ── Load ─────────────────────────────────────────── */
  loadShipments(): void {
    this.loadingShips.set(true);
    this.shipSvc.getAllUnpaged(500)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: p  => { this.allShipments.set(p.content); this.loadingShips.set(false); },
        error: () => this.loadingShips.set(false),
      });
  }

  loadGateways(): void {
    this.loadingGways.set(true);
    this.payAdm.getGateways()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: g  => { this.gateways.set(g); this.loadingGways.set(false); },
        error: () => this.loadingGways.set(false),
      });
  }

  /* ── Expand orders ────────────────────────────────── */
  toggleExpand(id: number): void {
    const map = this.expandedOrders();
    if (map.has(id)) {
      const n = new Map(map); n.delete(id);
      this.expandedOrders.set(n);
    } else {
      this.loadingOrders.update(s => new Set([...s, id]));
      this.orderSvc.getByShipment(id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: orders => {
            this.expandedOrders.update(m => new Map([...m, [id, orders]]));
            this.loadingOrders.update(s => { const n = new Set(s); n.delete(id); return n; });
          },
          error: () => this.loadingOrders.update(s => { const n = new Set(s); n.delete(id); return n; }),
        });
    }
  }

  isExpanded(id: number):        boolean         { return this.expandedOrders().has(id); }
  isLoadingOrders(id: number):   boolean         { return this.loadingOrders().has(id); }
  getOrders(id: number):         OrderResponse[] { return this.expandedOrders().get(id) ?? []; }
  getStats(id: number)                           { return this.shipmentStats().get(id); }

  /* ── Transaction modal ────────────────────────────── */
  showTransaction(orderId: number, hawb: string): void {
    this.txModalHawb.set(hawb);
    this.txModal.set([]);
    this.loadingTx.set(true);
    this.payAdm.getTransactionsByOrder(orderId)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: txs => { this.txModal.set(txs); this.loadingTx.set(false); },
        error: ()  => { this.loadingTx.set(false); this.toast.error('Erreur chargement transactions.'); },
      });
  }
  closeTransaction(): void { this.txModal.set(null); }

  /* ── Gateway activate / deactivate ───────────────── */
  toggleGateway(g: GatewayConfigResponse): void {
    this.togglingId.set(g.id);
    const obs = g.active
      ? this.payAdm.deactivateGateway(g.id)
      : this.payAdm.activateGateway(g.id);
    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: updated => {
        this.togglingId.set(null);
        this.gateways.update(list => list.map(x => x.id === updated.id ? updated : x));
        if (!g.active) this.gateways.update(list => list.map(x => x.id !== updated.id ? { ...x, active: false } : x));
        this.toast.success(g.active ? 'Gateway désactivé.' : 'Gateway activé.');
      },
      error: () => { this.togglingId.set(null); this.toast.error('Erreur.'); },
    });
  }

  /* ── Gateway form ─────────────────────────────────── */
  openCreateGw(): void {
    this.gwForm.reset({ type: 'PAYPAL', mode: 'TEST', supportedCurrencies: 'USD' });
    this.gwEditingId.set(null);
    this.gwFormMode.set('create');
  }

  openEditGw(g: GatewayConfigResponse): void {
    this.gwForm.reset({
      type: g.type, name: g.name, apiKey: '', secretKey: '',
      webhookSecret: '', mode: g.mode,
      supportedCurrencies: g.supportedCurrencies ?? 'USD',
      description: g.description ?? '',
    });
    this.gwEditingId.set(g.id);
    this.gwFormMode.set('edit');
  }

  closeGwForm(): void { this.gwFormMode.set(null); }

  saveGw(): void {
    if (this.gwForm.invalid) { this.gwForm.markAllAsTouched(); return; }
    this.gwSaving.set(true);
    const v = this.gwForm.getRawValue();
    const req: GatewayConfigRequest = {
      type: v.type as 'PAYPAL' | 'STRIPE' | 'CMI',
      name: v.name!,
      apiKey: v.apiKey || undefined,
      secretKey: v.secretKey || undefined,
      webhookSecret: v.webhookSecret || undefined,
      mode: v.mode as 'TEST' | 'PRODUCTION',
      supportedCurrencies: v.supportedCurrencies || 'USD',
      description: v.description || undefined,
    };
    const obs = this.gwEditingId()
      ? this.payAdm.updateGateway(this.gwEditingId()!, req)
      : this.payAdm.createGateway(req);
    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: saved => {
        this.gwSaving.set(false);
        this.gwFormMode.set(null);
        this.toast.success(this.gwEditingId() ? 'Gateway modifié.' : 'Gateway créé.');
        if (this.gwEditingId()) {
          this.gateways.update(list => list.map(x => x.id === saved.id ? saved : x));
        } else {
          this.gateways.update(list => [...list, saved]);
        }
      },
      error: err => { this.gwSaving.set(false); this.toast.error(err?.error?.message || 'Erreur.'); },
    });
  }

  deleteGw(id: number): void {
    this.deletingId.set(id);
    this.payAdm.deleteGateway(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deletingId.set(null);
          this.gateways.update(list => list.filter(g => g.id !== id));
          this.toast.success('Gateway supprimé.');
        },
        error: err => { this.deletingId.set(null); this.toast.error(err?.error?.message || 'Erreur.'); },
      });
  }

  gwFieldInvalid(f: string): boolean {
    const c = this.gwForm.get(f);
    return !!(c?.invalid && c?.touched);
  }

  /* ── Helpers ──────────────────────────────────────── */
  statusClass(status: string): string {
    const m: Record<string, string> = {
      DRAFT: 'pm-s-draft', IMPORTED: 'pm-s-imported',
      PROCESSING: 'pm-s-processing', CLOSED: 'pm-s-closed',
    };
    return m[status] ?? '';
  }

  statusLabel(status: string): string {
    const m: Record<string, string> = {
      DRAFT: 'Draft', IMPORTED: 'Importé',
      PROCESSING: 'En cours', CLOSED: 'Fermé',
    };
    return m[status] ?? status;
  }

  orderStatusClass(s: string): string {
    const m: Record<string, string> = {
      CREATED: 'pm-os-created', EMAIL_SENT: 'pm-os-email',
      PENDING_PAYMENT: 'pm-os-pending', PAID: 'pm-os-paid',
      IN_DELIVERY: 'pm-os-delivery', DELIVERED: 'pm-os-delivered',
      CANCELLED: 'pm-os-cancelled',
    };
    return m[s] ?? '';
  }

  orderStatusLabel(s: string): string {
    const m: Record<string, string> = {
      CREATED: 'Créé', EMAIL_SENT: 'Email envoyé',
      PENDING_PAYMENT: 'En attente', PAID: 'Payé',
      IN_DELIVERY: 'En livraison', DELIVERED: 'Livré', CANCELLED: 'Annulé',
    };
    return m[s] ?? s;
  }

  gatewayIcon(type: string): string {
    if (type === 'PAYPAL')  return '🅿';
    if (type === 'STRIPE')  return '⚡';
    if (type === 'CMI')     return '🏦';
    return '💳';
  }

  fmtAmt(n: number | null | undefined): string {
    if (n == null) return '—';
    return n.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' USD';
  }

  txStatusClass(s: string): string {
    const m: Record<string, string> = {
      INITIATED: 'pm-tx-initiated', SUCCESS: 'pm-tx-success',
      FAILED: 'pm-tx-failed', CANCELLED: 'pm-tx-cancelled', REFUNDED: 'pm-tx-refunded',
    };
    return m[s] ?? '';
  }
}
