import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgApexchartsModule } from 'ng-apexcharts';
import { LayoutService }    from '../../core/services/layout.service';
import { DashboardService } from '../../core/services/dashboard.service';
import { ShipmentService }  from '../../core/services/shipment.service';
import { ImportService }    from '../../core/services/import.service';
import { BadgeComponent }   from '../../shared/badge/badge.component';
import { DashboardKpi, StatusCount, MonthlyCount, MonthlyRevenue, DashboardAlerts } from '../../core/models/dashboard.model';
import { ShipmentResponse } from '../../core/models/shipment.model';
import { ImportLogResponse } from '../../core/models/import.model';
import type { ApexChart, ApexNonAxisChartSeries, ApexDataLabels,
              ApexLegend, ApexPlotOptions, ApexStroke, ApexTooltip,
              ApexXAxis, ApexYAxis, ApexFill, ApexGrid } from 'ng-apexcharts';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, BadgeComponent, NgApexchartsModule],
  templateUrl: './dashboard.component.html',
  styleUrl:    './dashboard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnInit {
  private readonly layout      = inject(LayoutService);
  private readonly dashSvc     = inject(DashboardService);
  private readonly shipSvc     = inject(ShipmentService);
  private readonly importSvc   = inject(ImportService);
  private readonly destroyRef  = inject(DestroyRef);

  /* ── Data ─────────────────────────────────────────────── */
  kpis            = signal<DashboardKpi | null>(null);
  statusCounts    = signal<StatusCount[]>([]);
  monthlyShips    = signal<MonthlyCount[]>([]);
  monthlyRev      = signal<MonthlyRevenue[]>([]);
  alerts          = signal<DashboardAlerts | null>(null);
  recentShipments = signal<ShipmentResponse[]>([]);
  recentImports   = signal<ImportLogResponse[]>([]);
  loading         = signal(true);

  /* ── Computed helpers ─────────────────────────────────── */
  paymentRate = computed(() => {
    const k = this.kpis();
    if (!k || k.totalOrders === 0) return 0;
    return Math.round((k.paidOrders / k.totalOrders) * 100);
  });

  paymentRateColor = computed(() => {
    const r = this.paymentRate();
    return r >= 80 ? 'green' : r >= 50 ? 'amber' : 'red';
  });

  totalAlerts = computed(() => {
    const a = this.alerts();
    if (!a) return 0;
    return a.ordersNeverEmailed + a.stuckShipments + a.overduePayments;
  });

  /* ── Donut chart — orders by status ──────────────────── */
  donutSeries = computed<ApexNonAxisChartSeries>(() =>
    this.statusCounts().map(s => s.count)
  );
  donutLabels = computed(() => this.statusCounts().map(s => this.fmtStatus(s.status)));
  donutColors = computed(() => this.statusCounts().map(s => this.statusColor(s.status)));

  readonly donutChart: ApexChart = {
    type: 'donut', height: 260,
    fontFamily: 'DM Sans, sans-serif',
    toolbar: { show: false },
    animations: { enabled: true, speed: 600 },
  };
  readonly donutDataLabels: ApexDataLabels = {
    enabled: true,
    formatter: (val: number) => Math.round(val) + '%',
    style: { fontSize: '12px', fontFamily: 'DM Sans, sans-serif', fontWeight: '600' },
    dropShadow: { enabled: false },
  };
  readonly donutLegend: ApexLegend = {
    position: 'bottom', fontSize: '12px',
    fontFamily: 'DM Sans, sans-serif',
    markers: { size: 8 },
    itemMargin: { horizontal: 8 },
  };
  readonly donutStroke: ApexStroke = { width: 2, colors: ['#fff'] };
  readonly donutPlotOptions: ApexPlotOptions = {
    pie: { donut: { size: '65%', labels: { show: true,
      total: { show: true, label: 'Total', fontSize: '13px',
               fontFamily: 'Syne, sans-serif', fontWeight: '800', color: '#0C0C0E',
               formatter: (w: { globals: { seriesTotals: number[] } }) =>
                 w.globals.seriesTotals.reduce((a, b) => a + b, 0).toString() }
    }}}
  };

  /* ── Bar chart — shipments by month ──────────────────── */
  barSeries = computed(() => [{
    name: 'Shipments',
    data: this.monthlyShips().map(m => m.count),
  }]);
  barCategories = computed(() => this.monthlyShips().map(m => m.month));

  readonly barChart: ApexChart = {
    type: 'bar', height: 260,
    fontFamily: 'DM Sans, sans-serif',
    toolbar: { show: false },
    animations: { enabled: true, speed: 600 },
  };
  readonly barXAxis: ApexXAxis = {
    labels: { style: { fontSize: '11px', fontFamily: 'DM Sans, sans-serif', colors: '#9CA3AF' } },
    axisBorder: { show: false }, axisTicks: { show: false },
  };
  readonly barYAxis: ApexYAxis = {
    labels: { style: { fontSize: '11px', colors: '#9CA3AF' } },
    min: 0, tickAmount: 4,
  };
  readonly barFill: ApexFill = {
    type: 'gradient',
    gradient: { shade: 'light', type: 'vertical', shadeIntensity: 0.4,
                gradientToColors: ['#FDBA74'], opacityFrom: 1, opacityTo: 0.7, stops: [0, 100] },
  };
  readonly barGrid: ApexGrid = {
    borderColor: '#F3F4F6', strokeDashArray: 4,
    xaxis: { lines: { show: false } },
  };
  readonly barTooltip: ApexTooltip = {
    theme: 'light',
    y: { formatter: (v: number) => v + ' shipment(s)' },
  };
  readonly barColors = ['#F97316'];
  readonly barPlotOptions: ApexPlotOptions = {
    bar: { borderRadius: 6, columnWidth: '55%' },
  };

  /* ── Area chart — revenue by month ───────────────────── */
  areaSeries = computed(() => [{
    name: 'Encaissé (USD)',
    data: this.monthlyRev().map(m => +m.amount),
  }]);
  areaCategories = computed(() => this.monthlyRev().map(m => m.month));

  readonly areaChart: ApexChart = {
    type: 'area', height: 260,
    fontFamily: 'DM Sans, sans-serif',
    toolbar: { show: false },
    animations: { enabled: true, speed: 600 },
  };
  readonly areaXAxis: ApexXAxis = {
    labels: { style: { fontSize: '11px', fontFamily: 'DM Sans, sans-serif', colors: '#9CA3AF' } },
    axisBorder: { show: false }, axisTicks: { show: false },
  };
  readonly areaYAxis: ApexYAxis = {
    labels: { style: { fontSize: '11px', colors: '#9CA3AF' },
              formatter: (v: number) => v === 0 ? '0' : v >= 1000 ? (v/1000).toFixed(1)+'k' : v.toString() },
    min: 0,
  };
  readonly areaFill: ApexFill = {
    type: 'gradient',
    gradient: { shade: 'light', type: 'vertical', shadeIntensity: 0.1,
                gradientToColors: ['#22C55E'], opacityFrom: 0.4, opacityTo: 0.02, stops: [0, 100] },
  };
  readonly areaStroke: ApexStroke = { curve: 'smooth', width: 2.5, colors: ['#22C55E'] };
  readonly areaGrid: ApexGrid = {
    borderColor: '#F3F4F6', strokeDashArray: 4,
    xaxis: { lines: { show: false } },
  };
  readonly areaTooltip: ApexTooltip = {
    theme: 'light',
    y: { formatter: (v: number) => v.toFixed(2) + ' USD' },
  };
  readonly areaColors = ['#22C55E'];

  /* ── Lifecycle ────────────────────────────────────────── */
  ngOnInit(): void {
    this.layout.setPage('Dashboard', { label: 'Importer', routerLink: '/imports' });
    this.loadAll();
  }

  refresh(): void { this.loading.set(true); this.loadAll(); }

  private loadAll(): void {
    let done = 0;
    const total = 7;
    const mark = () => { if (++done === total) this.loading.set(false); };

    this.dashSvc.getKpis().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: d => this.kpis.set(d), error: mark, complete: mark,
    });
    this.dashSvc.getOrdersByStatus().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: d => this.statusCounts.set(d), error: mark, complete: mark,
    });
    this.dashSvc.getShipmentsByMonth().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: d => this.monthlyShips.set(d), error: mark, complete: mark,
    });
    this.dashSvc.getRevenueByMonth().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: d => this.monthlyRev.set(d), error: mark, complete: mark,
    });
    this.dashSvc.getAlerts().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: d => this.alerts.set(d), error: mark, complete: mark,
    });
    this.shipSvc.getAll(0, 5, 'createdAt,desc').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: p => this.recentShipments.set(p.content), error: mark, complete: mark,
    });
    this.importSvc.getHistory(0, 6).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: p => this.recentImports.set(p.content), error: mark, complete: mark,
    });
  }

  /* ── Helpers ──────────────────────────────────────────── */
  fmtAmount(n: number): string {
    if (n === 0) return '0';
    if (n >= 1000) return (n / 1000).toFixed(1) + 'k';
    return n.toFixed(0);
  }

  fmtStatus(s: string): string {
    const map: Record<string, string> = {
      CREATED: 'Créé', EMAIL_SENT: 'Email envoyé',
      PENDING_PAYMENT: 'En attente', PAID: 'Payé',
      IN_DELIVERY: 'En livraison', DELIVERED: 'Livré', CANCELLED: 'Annulé',
    };
    return map[s] ?? s;
  }

  statusColor(s: string): string {
    const map: Record<string, string> = {
      CREATED: '#9CA3AF', EMAIL_SENT: '#3B82F6',
      PENDING_PAYMENT: '#F59E0B', PAID: '#22C55E',
      IN_DELIVERY: '#F97316', DELIVERED: '#10B981', CANCELLED: '#EF4444',
    };
    return map[s] ?? '#9CA3AF';
  }
}
