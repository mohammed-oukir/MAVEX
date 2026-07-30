import {
  ChangeDetectionStrategy, Component, DestroyRef,
  OnInit, computed, inject, signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { NgApexchartsModule } from 'ng-apexcharts';
import type {
  ApexChart, ApexDataLabels, ApexLegend,
  ApexStroke, ApexTooltip, ApexXAxis, ApexYAxis, ApexGrid,
} from 'ng-apexcharts';

import { LayoutService }              from '../../core/services/layout.service';
import { DashboardAnalyticsService }  from '../../core/services/dashboard-analytics.service';
import { AuthService }                from '../../core/auth/auth.service';
import {
  EmailStatsHistoryResponse,
  BrevoQuotaResponse,
} from '../../core/models/dashboard-stats.model';

@Component({
  selector: 'app-dashboard-analytics',
  imports: [NgApexchartsModule],
  templateUrl: './dashboard-analytics.component.html',
  styleUrl:    './dashboard-analytics.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DashboardAnalyticsComponent implements OnInit {
  private readonly layout  = inject(LayoutService);
  private readonly statsService = inject(DashboardAnalyticsService);
  private readonly destroy = inject(DestroyRef);
  readonly auth = inject(AuthService);

  readonly statsHistory = signal<EmailStatsHistoryResponse | null>(null);
  readonly quota        = signal<BrevoQuotaResponse | null>(null);
  readonly loadingStats = signal(true);
  readonly loadingQuota = signal(true);
  readonly statsError   = signal<string | null>(null);

  /** Valeur affichée par la card "Total envoyés", animée de 0 à sa valeur finale */
  readonly animatedSent = signal(0);

  /** Affichage replié/déplié du tableau détaillé jour par jour */
  readonly showDetailTable = signal(false);

  /** Niveau du quota Brevo restant, pour la couleur du bandeau */
  readonly quotaLevel = computed<'green' | 'orange' | 'red'>(() => {
    const remaining = this.quota()?.remainingCredits ?? 0;
    if (remaining > 150) return 'green';
    if (remaining >= 50) return 'orange';
    return 'red';
  });

  /** Recalcul manuel (ADMIN uniquement) */
  readonly isRecomputing    = signal(false);
  readonly recomputeMessage = signal<{ text: string; type: 'success' | 'error' } | null>(null);
  private recomputeMessageTimeout?: ReturnType<typeof setTimeout>;

  /* ── Graphique — évolution envoyés/livrés/ouverts/bounced ── */
  readonly lineSeries = computed(() => {
    const days = this.statsHistory()?.days ?? [];
    return [
      { name: 'Envoyés', data: days.map(d => d.sent) },
      { name: 'Livrés',  data: days.map(d => d.delivered) },
      { name: 'Ouverts', data: days.map(d => d.opened) },
      { name: 'Bounced', data: days.map(d => d.bounced) },
    ];
  });

  readonly lineCategories = computed(() =>
    (this.statsHistory()?.days ?? []).map(d => d.date)
  );

  readonly lineChart: ApexChart = {
    type: 'line', height: 320,
    fontFamily: 'DM Sans, sans-serif',
    toolbar: { show: false },
    animations: { enabled: true, speed: 600 },
  };
  readonly lineColors = ['#F97316', '#22C55E', '#374151', '#EF4444'];
  readonly lineStroke: ApexStroke = { curve: 'smooth', width: 2.5 };
  readonly lineXAxis: ApexXAxis = {
    labels: { style: { fontSize: '11px', fontFamily: 'DM Sans', colors: '#9CA3AF' } },
    axisBorder: { show: false }, axisTicks: { show: false },
  };
  readonly lineYAxis: ApexYAxis = {
    labels: { style: { fontSize: '11px', colors: '#9CA3AF' } },
    min: 0,
  };
  readonly lineGrid: ApexGrid = { borderColor: '#F3F4F6', strokeDashArray: 4, xaxis: { lines: { show: false } } };
  readonly lineLegend: ApexLegend = {
    position: 'top', horizontalAlign: 'right',
    fontSize: '12px', fontFamily: 'DM Sans, sans-serif',
    markers: { size: 7 },
  };
  readonly lineTooltip: ApexTooltip = { theme: 'light', shared: true, intersect: false };
  readonly lineDataLabels: ApexDataLabels = { enabled: false };

  ngOnInit(): void {
    this.layout.setPage('Analytics email');
    this.loadStats();
    this.loadQuota();
  }

  toggleDetailTable(): void {
    this.showDetailTable.update(v => !v);
  }

  isToday(date: string): boolean {
    return date === new Date().toISOString().slice(0, 10);
  }

  /* ── Panneau Aperçu — dérivé de statsHistory(), aucun appel API ── */
  readonly bestDay = computed(() => {
    const days = this.statsHistory()?.days ?? [];
    if (days.length === 0) return null;
    const best = days.reduce((a, b) => (b.sent > a.sent ? b : a));
    if (best.sent === 0) return null;
    return best;
  });

  readonly dailyAverage = computed(() => {
    const data = this.statsHistory();
    if (!data || data.days.length === 0) return 0;
    return Math.round((data.summary.totalSent / data.days.length) * 10) / 10;
  });

  readonly worstBounceDay = computed(() => {
    const days = (this.statsHistory()?.days ?? []).filter(d => d.bounced > 0);
    if (days.length === 0) return null;
    const worst = days.reduce((a, b) => (b.bounced > a.bounced ? b : a));
    return { day: worst, otherDaysCount: days.length - 1 };
  });

  readonly todaySummary = computed(() => {
    const days = this.statsHistory()?.days ?? [];
    return days.find(d => this.isToday(d.date)) ?? null;
  });

  recomputeNow(): void {
    this.isRecomputing.set(true);
    this.statsService.recomputeStats()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: () => {
          this.isRecomputing.set(false);
          this.loadStats();
          this.loadQuota();
          this.showRecomputeMessage('Stats recalculées', 'success');
        },
        error: () => {
          this.isRecomputing.set(false);
          this.showRecomputeMessage('Erreur lors du recalcul.', 'error');
        },
      });
  }

  private showRecomputeMessage(text: string, type: 'success' | 'error'): void {
    if (this.recomputeMessageTimeout) {
      clearTimeout(this.recomputeMessageTimeout);
    }
    this.recomputeMessage.set({ text, type });
    this.recomputeMessageTimeout = setTimeout(() => {
      this.recomputeMessage.set(null);
    }, 2500);
  }

  private loadStats(): void {
    this.loadingStats.set(true);
    this.statsError.set(null);
    this.statsService.getStatsHistory(30)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: data => {
          this.statsHistory.set(data);
          this.loadingStats.set(false);
          this.animateSentCount(data.summary.totalSent);
        },
        error: () => {
          this.statsError.set('Erreur lors du chargement des statistiques.');
          this.loadingStats.set(false);
        },
      });
  }

  /** Anime le compteur de 0 jusqu'à `target` en ~800ms (ease-out) */
  private animateSentCount(target: number): void {
    if (target <= 0) {
      this.animatedSent.set(target);
      return;
    }

    const duration = 800;
    const start    = performance.now();

    const step = (now: number) => {
      const progress = Math.min((now - start) / duration, 1);
      const eased    = 1 - Math.pow(1 - progress, 3);
      this.animatedSent.set(Math.round(eased * target));
      if (progress < 1) {
        requestAnimationFrame(step);
      }
    };

    requestAnimationFrame(step);
  }

  private loadQuota(): void {
    this.loadingQuota.set(true);
    this.statsService.getQuota()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: data => {
          this.quota.set(data);
          this.loadingQuota.set(false);
        },
        error: () => {
          this.quota.set({ remainingCredits: null, available: false, errorMessage: 'Erreur lors du chargement du quota.' });
          this.loadingQuota.set(false);
        },
      });
  }
}
