import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService }   from '../../core/services/layout.service';
import { ShipmentService } from '../../core/services/shipment.service';
import { OrderService }    from '../../core/services/order.service';
import { ImportService }   from '../../core/services/import.service';
import { BadgeComponent }  from '../../shared/badge/badge.component';
import { ShipmentResponse } from '../../core/models/shipment.model';
import { OrderResponse }    from '../../core/models/order.model';
import { ImportLogResponse } from '../../core/models/import.model';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, BadgeComponent, DatePipe],
  templateUrl: './dashboard.component.html',
  styleUrl:    './dashboard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnInit {
  private readonly layout     = inject(LayoutService);
  private readonly shipSvc    = inject(ShipmentService);
  private readonly orderSvc   = inject(OrderService);
  private readonly importSvc  = inject(ImportService);
  private readonly destroyRef = inject(DestroyRef);

  shipments = signal<ShipmentResponse[]>([]);
  orders    = signal<OrderResponse[]>([]);
  imports   = signal<ImportLogResponse[]>([]);
  loading   = signal(true);

  /* ── KPI computeds ─────────────────────────────────────── */
  totalShipments  = computed(() => this.shipments().length);
  activeShipments = computed(() => this.shipments().filter(s => s.status !== 'CLOSED').length);
  totalOrders     = computed(() => this.orders().length);
  paidOrders      = computed(() => this.orders().filter(o => o.status === 'PAID').length);
  pendingOrders   = computed(() => this.orders().filter(o =>
    ['PENDING_PAYMENT', 'CREATED', 'EMAIL_SENT'].includes(o.status)).length);

  paymentRate = computed(() => {
    const total = this.totalOrders();
    return total === 0 ? 0 : Math.round((this.paidOrders() / total) * 100);
  });

  paymentRateColor = computed(() => {
    if (this.totalOrders() === 0) return 'amber';
    const r = this.paymentRate();
    return r >= 80 ? 'green' : r >= 50 ? 'amber' : 'red';
  });

  paymentFillClass = computed(() => `dk-pay-fill fill-${this.paymentRateColor()}`);

  /* ── Recent slices ──────────────────────────────────────── */
  recentShipments = computed(() => this.shipments().slice(0, 5));
  recentImports   = computed(() => this.imports().slice(0, 5));
  recentOrders    = computed(() => this.orders().slice(0, 8));

  /* ── Lifecycle ──────────────────────────────────────────── */
  ngOnInit(): void {
    this.layout.setPage('Dashboard', { label: 'Importer', routerLink: '/imports' });
    this.loadAll();
  }

  refresh(): void {
    this.loading.set(true);
    this.loadAll();
  }

  private loadAll(): void {
    let done = 0;
    const mark = () => { if (++done === 3) this.loading.set(false); };

    this.shipSvc.getAllUnpaged().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:     p  => this.shipments.set(p.content),
      error:    mark,
      complete: mark,
    });

    this.orderSvc.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:     list => this.orders.set(list),
      error:    mark,
      complete: mark,
    });

    this.importSvc.getHistory(0, 10).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:     p  => this.imports.set(p.content),
      error:    mark,
      complete: mark,
    });
  }
}
