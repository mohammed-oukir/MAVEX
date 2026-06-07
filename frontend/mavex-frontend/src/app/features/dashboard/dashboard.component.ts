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
import { AuthService }      from '../../core/auth/auth.service';
import { BadgeComponent }   from '../../shared/badge/badge.component';
import { DashboardKpi, StatusCount, MonthlyCount, MonthlyRevenue, DashboardAlerts } from '../../core/models/dashboard.model';
import { ShipmentResponse } from '../../core/models/shipment.model';
import { ImportLogResponse } from '../../core/models/import.model';
import type {
  ApexChart, ApexNonAxisChartSeries, ApexDataLabels, ApexLegend,
  ApexPlotOptions, ApexStroke, ApexTooltip, ApexXAxis, ApexYAxis,
  ApexFill, ApexGrid,
} from 'ng-apexcharts';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, BadgeComponent, NgApexchartsModule],
  templateUrl: './dashboard.component.html',
  styleUrl:    './dashboard.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardComponent implements OnInit {
  private readonly layout     = inject(LayoutService);
  private readonly dashSvc    = inject(DashboardService);
  private readonly shipSvc    = inject(ShipmentService);
  private readonly importSvc  = inject(ImportService);
  private readonly auth       = inject(AuthService);
  private readonly destroyRef = inject(DestroyRef);

  /* ── Data signals ─────────────────────────────────────── */
  kpis            = signal<DashboardKpi | null>(null);
  statusCounts    = signal<StatusCount[]>([]);
  monthlyShips    = signal<MonthlyCount[]>([]);
  monthlyRev      = signal<MonthlyRevenue[]>([]);
  alerts          = signal<DashboardAlerts | null>(null);
  recentShipments = signal<ShipmentResponse[]>([]);
  recentImports   = signal<ImportLogResponse[]>([]);
  loading         = signal(true);

  /* ── Greeting ─────────────────────────────────────────── */
  greeting = computed(() => {
    const h = new Date().getHours();
    if (h < 12) return 'Bonjour';
    if (h < 18) return 'Bon après-midi';
    return 'Bonsoir';
  });

  userName = computed(() => {
    const u = this.auth.currentUser();
    return u.fullName || u.email || 'Mohammed';
  });

  /* ── KPI computed ─────────────────────────────────────── */
  paymentRate = computed(() => {
    const k = this.kpis();
    if (!k || k.totalOrders === 0) return 0;
    return Math.round((k.paidOrders / k.totalOrders) * 100);
  });

  paymentRateColor = computed(() => {
    const r = this.paymentRate();
    return r >= 80 ? '#22C55E' : r >= 50 ? '#F59E0B' : '#EF4444';
  });

  totalAlerts = computed(() => {
    const a = this.alerts();
    if (!a) return 0;
    return a.ordersNeverEmailed + a.stuckShipments + a.overduePayments;
  });

  /* ── Donut — orders by status ─────────────────────────── */
  donutSeries  = computed<ApexNonAxisChartSeries>(() => this.statusCounts().map(s => s.count));
  donutLabels  = computed(() => this.statusCounts().map(s => this.fmtStatus(s.status)));
  donutColors  = computed(() => this.statusCounts().map(s => this.statusColor(s.status)));

  readonly donutChart: ApexChart = {
    type: 'donut', height: 280,
    fontFamily: 'DM Sans, sans-serif',
    toolbar: { show: false },
    animations: { enabled: true, speed: 700, animateGradually: { enabled: true, delay: 80 } },
  };
  readonly donutDataLabels: ApexDataLabels = {
    enabled: false,
  };
  readonly donutLegend: ApexLegend = {
    position: 'bottom', fontSize: '12px',
    fontFamily: 'DM Sans, sans-serif',
    markers: { size: 7 },
    itemMargin: { horizontal: 10, vertical: 4 },
    labels: { colors: '#6B7280' },
  };
  readonly donutStroke: ApexStroke = { width: 3, colors: ['#fff'] };
  readonly donutPlotOptions: ApexPlotOptions = {
    pie: {
      donut: {
        size: '72%',
        labels: {
          show: true,
          name: { show: true, fontSize: '13px', fontFamily: 'DM Sans', color: '#9CA3AF', offsetY: -8 },
          value: { show: true, fontSize: '28px', fontFamily: 'Syne, sans-serif', fontWeight: '800', color: '#0C0C0E', offsetY: 6 },
          total: {
            show: true, label: 'Total orders',
            fontSize: '12px', fontFamily: 'DM Sans', fontWeight: '600', color: '#9CA3AF',
            formatter: (w: { globals: { seriesTotals: number[] } }) =>
              w.globals.seriesTotals.reduce((a, b) => a + b, 0).toString(),
          },
        },
      },
    },
  };

  /* ── Bar — shipments by month ─────────────────────────── */
  barSeries      = computed(() => [{ name: 'Shipments', data: this.monthlyShips().map(m => m.count) }]);
  barCategories  = computed(() => this.monthlyShips().map(m => m.month));

  readonly barChart: ApexChart = {
    type: 'bar', height: 280,
    fontFamily: 'DM Sans, sans-serif',
    toolbar: { show: false },
    animations: { enabled: true, speed: 700 },
    sparkline: { enabled: false },
  };
  readonly barXAxis: ApexXAxis = {
    labels: { style: { fontSize: '11px', fontFamily: 'DM Sans', colors: '#9CA3AF' } },
    axisBorder: { show: false }, axisTicks: { show: false },
  };
  readonly barYAxis: ApexYAxis = {
    labels: { style: { fontSize: '11px', colors: '#9CA3AF' }, formatter: (v: number) => Math.round(v).toString() },
    min: 0, tickAmount: 4,
  };
  readonly barFill: ApexFill = {
    type: 'gradient',
    gradient: { shade: 'light', type: 'vertical', shadeIntensity: 0.3, gradientToColors: ['#FDBA74'], opacityFrom: 1, opacityTo: 0.75, stops: [0, 100] },
  };
  readonly barGrid: ApexGrid = { borderColor: '#F3F4F6', strokeDashArray: 4, xaxis: { lines: { show: false } } };
  readonly barTooltip: ApexTooltip = { theme: 'light', y: { formatter: (v: number) => v + ' shipment(s)' } };
  readonly barColors  = ['#F97316'];
  readonly barPlotOptions: ApexPlotOptions = { bar: { borderRadius: 6, columnWidth: '50%' } };

  /* ── Area — revenue by month ──────────────────────────── */
  areaSeries     = computed(() => [{ name: 'Revenus (USD)', data: this.monthlyRev().map(m => +m.amount) }]);
  areaCategories = computed(() => this.monthlyRev().map(m => m.month));

  readonly areaChart: ApexChart = {
    type: 'area', height: 280,
    fontFamily: 'DM Sans, sans-serif',
    toolbar: { show: false },
    animations: { enabled: true, speed: 700 },
  };
  readonly areaXAxis: ApexXAxis = {
    labels: { style: { fontSize: '11px', fontFamily: 'DM Sans', colors: '#9CA3AF' } },
    axisBorder: { show: false }, axisTicks: { show: false },
  };
  readonly areaYAxis: ApexYAxis = {
    labels: {
      style: { fontSize: '11px', colors: '#9CA3AF' },
      formatter: (v: number) => v === 0 ? '0' : v >= 1000 ? (v / 1000).toFixed(1) + 'k' : v.toString(),
    },
    min: 0,
  };
  readonly areaFill: ApexFill = {
    type: 'gradient',
    gradient: { shade: 'light', type: 'vertical', shadeIntensity: 0.05, gradientToColors: ['#F97316'], opacityFrom: 0.35, opacityTo: 0.01, stops: [0, 100] },
  };
  readonly areaStroke: ApexStroke = { curve: 'smooth', width: 2.5, colors: ['#F97316'] };
  readonly areaGrid: ApexGrid  = { borderColor: '#F3F4F6', strokeDashArray: 4, xaxis: { lines: { show: false } } };
  readonly areaTooltip: ApexTooltip = { theme: 'light', y: { formatter: (v: number) => '$' + v.toLocaleString('en-US', { minimumFractionDigits: 0, maximumFractionDigits: 0 }) } };
  readonly areaColors = ['#F97316'];
  readonly areaDataLabels: ApexDataLabels = { enabled: false };

  /* ── Radial — payment rate ────────────────────────────── */
  radialSeries  = computed(() => [this.paymentRate()]);
  radialColors  = computed(() => [this.paymentRateColor()]);

  readonly radialChart: ApexChart = {
    type: 'radialBar', height: 280,
    fontFamily: 'DM Sans, sans-serif',
    toolbar: { show: false },
    animations: { enabled: true, speed: 800 },
  };
  readonly radialPlotOptions: ApexPlotOptions = {
    radialBar: {
      startAngle: -130, endAngle: 130,
      hollow: { size: '60%', background: 'transparent' },
      track: { background: '#F3F4F6', strokeWidth: '100%' },
      dataLabels: {
        name: { show: true, fontSize: '13px', fontFamily: 'DM Sans', color: '#9CA3AF', offsetY: 20 },
        value: { show: true, fontSize: '34px', fontFamily: 'Syne, sans-serif', fontWeight: '800', color: '#0C0C0E', offsetY: -10,
                 formatter: (v: number) => v + '%' },
      },
    },
  };
  readonly radialLabels = ['Taux de paiement'];

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

    this.dashSvc.getKpis().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: d => { this.kpis.set(d); mark(); }, error: mark });
    this.dashSvc.getOrdersByStatus().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: d => { this.statusCounts.set(d); mark(); }, error: mark });
    this.dashSvc.getShipmentsByMonth().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: d => { this.monthlyShips.set(d); mark(); }, error: mark });
    this.dashSvc.getRevenueByMonth().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: d => { this.monthlyRev.set(d); mark(); }, error: mark });
    this.dashSvc.getAlerts().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: d => { this.alerts.set(d); mark(); }, error: mark });
    this.shipSvc.getAll(0, 5, 'createdAt,desc').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: p => { this.recentShipments.set(p.content); mark(); }, error: mark });
    this.importSvc.getHistory(0, 6).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: p => { this.recentImports.set(p.content); mark(); }, error: mark });
  }

  /* ── Helpers ──────────────────────────────────────────── */
  fmtUsd(n: number): string {
    if (n === 0) return '$0';
    if (n >= 1_000_000) return '$' + (n / 1_000_000).toFixed(1) + 'M';
    if (n >= 1_000) return '$' + (n / 1_000).toFixed(1) + 'k';
    return '$' + n.toFixed(0);
  }

  fmtDuty(rate?: number | null): string {
    if (rate == null) return '—';
    return (Math.round(rate * 10000) / 100) + '%';
  }

  fmtDate(d?: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'short', year: 'numeric' });
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
