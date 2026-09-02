import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService } from '../../core/services/layout.service';
import { ShipmentService } from '../../core/services/shipment.service';
import { DutyChangeHistoryResponse } from '../../core/models/shipment.model';

@Component({
  selector: 'app-duty-history-orders',
  imports: [RouterLink, DatePipe],
  templateUrl: './duty-history-orders.component.html',
  styleUrl: './duty-history-orders.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class DutyHistoryOrdersComponent implements OnInit {
  private readonly layout     = inject(LayoutService);
  private readonly shipSvc    = inject(ShipmentService);
  private readonly route      = inject(ActivatedRoute);
  private readonly destroyRef = inject(DestroyRef);

  shipmentId = signal(0);

  /* ── Data ─────────────────────────────────────────────── */
  history = signal<DutyChangeHistoryResponse[]>([]);
  total   = signal(0);
  page    = signal(0);
  loading = signal(true);
  pageSize = signal(20);

  /* ── Filtres ──────────────────────────────────────────── */
  filtersCollapsed = signal(false);
  flHawb            = signal('');
  flChangedByName   = signal('');
  flFrom            = signal('');
  flTo              = signal('');

  hasFilters = computed(() =>
    !!this.flHawb() || !!this.flChangedByName() || !!this.flFrom() || !!this.flTo()
  );

  totalPages = computed(() => Math.ceil(this.total() / this.pageSize()));

  ngOnInit(): void {
    this.layout.setPage('Historique Duty — Orders');
    const idParam = this.route.snapshot.paramMap.get('id');
    this.shipmentId.set(Number(idParam));
    this.loadHistory();
  }

  private loadHistory(): void {
    this.loading.set(true);
    this.shipSvc.getOrderDutyHistory(
      this.shipmentId(),
      {
        hawb: this.flHawb().trim() || undefined,
        changedByName: this.flChangedByName().trim() || undefined,
        from: this.flFrom() || undefined,
        to:   this.flTo()   || undefined,
      },
      this.page(),
      this.pageSize(),
    ).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: p => { this.history.set(p.content); this.total.set(p.totalElements); this.loading.set(false); },
      error: () => this.loading.set(false),
    });
  }

  onSearchClick(): void {
    this.page.set(0);
    this.loadHistory();
  }

  clearFilters(): void {
    this.flHawb.set('');
    this.flChangedByName.set('');
    this.flFrom.set('');
    this.flTo.set('');
    this.page.set(0);
    this.loadHistory();
  }

  goToPage(p: number): void { this.page.set(p); this.loadHistory(); }
  pagesArray(): number[]    { return Array.from({ length: this.totalPages() }, (_, i) => i); }
  onPageSizeChange(size: number): void { this.pageSize.set(size); this.page.set(0); this.loadHistory(); }

  /** Même comportement que fmtDuty() dans ShipmentDetailComponent : 0.10 → "10 %". */
  fmtDuty(rate: number | null | undefined): string {
    if (rate == null) return '—';
    return Math.round(rate * 10000) / 100 + ' %';
  }
}
