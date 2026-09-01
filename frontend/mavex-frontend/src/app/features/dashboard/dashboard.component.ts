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
import { DashboardKpi, StatusCount, MonthlyRevenue, DashboardAlerts } from '../../core/models/dashboard.model';
import { ShipmentResponse } from '../../core/models/shipment.model';
import { ImportLogResponse } from '../../core/models/import.model';
import type {
  ApexChart, ApexDataLabels,
  ApexStroke, ApexTooltip, ApexXAxis, ApexYAxis,
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

  /* ── Funnel — pipeline des orders (ordre du workflow) ─────
     Montre OÙ les colis se bloquent dans le cycle de vie.
     Les statuts sont rangés dans l'ordre métier, pas par volume. */
  private readonly funnelOrder = [
    'CREATED', 'EMAIL_SENT', 'PAID',
  ];

  funnel = computed(() => {
    const counts = new Map(this.statusCounts().map(s => [s.status, s.count]));
    const steps = this.funnelOrder
      .map(status => ({
        status,
        label: this.fmtStatus(status),
        color: this.statusColor(status),
        count: counts.get(status) ?? 0,
      }))
      .filter(s => s.count > 0);

    const max = Math.max(1, ...steps.map(s => s.count));
    const total = steps.reduce((a, s) => a + s.count, 0);

    return steps.map(s => ({
      ...s,
      // largeur de barre relative à l'étape la plus chargée
      width: Math.round((s.count / max) * 100),
      // part du total (pour le %)
      share: total > 0 ? Math.round((s.count / total) * 100) : 0,
    }));
  });

  /* Annulés affichés à part (hors du flux normal) */
  cancelledCount = computed(() =>
    this.statusCounts().find(s => s.status === 'CANCELLED')?.count ?? 0,
  );

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
  readonly areaTooltip: ApexTooltip = { theme: 'light', y: { formatter: (v: number) => v.toLocaleString('fr-FR', { style: 'currency', currency: 'USD', minimumFractionDigits: 2, maximumFractionDigits: 2 }) } };
  readonly areaColors = ['#F97316'];
  readonly areaDataLabels: ApexDataLabels = { enabled: false };

  /* ── Lifecycle ────────────────────────────────────────── */
  ngOnInit(): void {
    this.layout.setPage('Dashboard');
    this.loadAll();
  }

  refresh(): void { this.loading.set(true); this.loadAll(); }

  private loadAll(): void {
    let done = 0;
    const total = 6;
    const mark = () => { if (++done === total) this.loading.set(false); };

    this.dashSvc.getKpis().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: d => { this.kpis.set(d); mark(); }, error: mark });
    this.dashSvc.getOrdersByStatus().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: d => { this.statusCounts.set(d); mark(); }, error: mark });
    this.dashSvc.getRevenueByMonth().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: d => { this.monthlyRev.set(d); mark(); }, error: mark });
    this.dashSvc.getAlerts().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: d => { this.alerts.set(d); mark(); }, error: mark });
    this.shipSvc.getAll(0, 5, 'createdAt,desc').pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: p => { this.recentShipments.set(p.content); mark(); }, error: mark });
    this.importSvc.getHistory(0, 6).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({ next: p => { this.recentImports.set(p.content); mark(); }, error: mark });
  }

  /* ── Helpers ──────────────────────────────────────────── */
  fmtUsd(n: number): string {
    if (n === 0) return '0 USD';
    if (n >= 1_000_000) return (n / 1_000_000).toLocaleString('fr-FR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }) + 'M USD';
    if (n >= 1_000) return (n / 1_000).toLocaleString('fr-FR', { minimumFractionDigits: 1, maximumFractionDigits: 1 }) + 'k USD';
    return n.toLocaleString('fr-FR', { maximumFractionDigits: 0 }) + ' USD';
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
      PAID: 'Payé', CANCELLED: 'Annulé',
    };
    return map[s] ?? s;
  }

  statusColor(s: string): string {
    const map: Record<string, string> = {
      CREATED: '#9CA3AF', EMAIL_SENT: '#3B82F6',
      PAID: '#22C55E', CANCELLED: '#EF4444',
    };
    return map[s] ?? '#9CA3AF';
  }
}
