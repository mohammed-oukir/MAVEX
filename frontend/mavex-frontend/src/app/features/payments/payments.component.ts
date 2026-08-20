import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService } from '../../core/services/layout.service';
import { PaymentHistoryService } from '../../core/services/payment-history.service';
import { ToastService } from '../../core/services/toast.service';
import { AuthService } from '../../core/auth/auth.service';
import { MyPermissionsService } from '../../core/services/my-permissions.service';
import { BadgeComponent } from '../../shared/badge/badge.component';
import {
  PaymentTransactionResponse, PaymentTransactionSearchParams,
  PaymentGatewayType, PaymentStatus,
} from '../../core/models/payment.model';

@Component({
  selector: 'app-payments',
  imports: [DatePipe, RouterLink, BadgeComponent],
  templateUrl: './payments.component.html',
  styleUrl: './payments.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentsComponent implements OnInit {
  private readonly layout     = inject(LayoutService);
  private readonly svc        = inject(PaymentHistoryService);
  private readonly toast      = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);
  protected readonly auth          = inject(AuthService);
  protected readonly myPermissions = inject(MyPermissionsService);

  /* ── Data ─────────────────────────────────────────────── */
  transactions = signal<PaymentTransactionResponse[]>([]);
  total    = signal(0);
  page     = signal(0);
  loading  = signal(true);
  pageSize = signal(20);

  /* ── Sélection bulk ───────────────────────────────────── */
  selectedIds  = signal<Set<number>>(new Set());
  someSelected = computed(() => this.selectedIds().size > 0);
  allSelected  = computed(() => this.transactions().length > 0 && this.selectedIds().size === this.transactions().length);
  selCount     = computed(() => this.selectedIds().size);

  /* ── Export ───────────────────────────────────────────── */
  exportOpen = signal(false);

  /* ── Envoi manuel du reçu (gateway MANUEL) ───────────────── */
  sendingReceiptId = signal<number | null>(null);

  /* ── KPIs ─────────────────────────────────────────────── */
  kpiTotal          = signal(0);
  kpiSuccess        = signal(0);
  kpiFailed         = signal(0);
  kpiInitiated      = signal(0);
  kpiTotalCollected = signal(0);

  /* ── Panneau de filtres ───────────────────────────────── */
  filtersCollapsed    = signal(false);
  advancedFiltersOpen = signal(false);
  flHawb       = signal('');
  flClient     = signal('');
  flGateway    = signal<PaymentGatewayType | ''>('');
  flStatus     = signal<PaymentStatus | ''>('');
  flAmountMin  = signal('');
  flAmountMax  = signal('');
  flFrom       = signal('');
  flTo         = signal('');

  /* ── Computed ─────────────────────────────────────────── */
  totalPages = computed(() => Math.ceil(this.total() / this.pageSize()));
  hasFilters = computed(() =>
    !!this.flHawb()      || !!this.flClient()    || !!this.flGateway() ||
    !!this.flStatus()    || !!this.flAmountMin() || !!this.flAmountMax() ||
    !!this.flFrom()      || !!this.flTo()
  );

  /* ── Lifecycle ────────────────────────────────────────── */
  ngOnInit(): void {
    this.layout.setPage('Paiements');
    this.loadKpis();
    this.loadTransactions();
  }

  /* ── Chargement ───────────────────────────────────────── */
  private loadTransactions(): void {
    this.loading.set(true);
    this.svc.search(this.buildParams(), this.page(), this.pageSize())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: p => { this.transactions.set(p.content); this.total.set(p.totalElements); this.loading.set(false); },
        error: () => this.loading.set(false),
      });
  }

  private buildParams(): PaymentTransactionSearchParams {
    const num = (s: string) => (s.trim() !== '' ? Number(s) : undefined);
    return {
      hawb:       this.flHawb().trim()   || undefined,
      client:     this.flClient().trim() || undefined,
      gateway:    this.flGateway()       || undefined,
      status:     this.flStatus()        || undefined,
      amountMin:  num(this.flAmountMin()),
      amountMax:  num(this.flAmountMax()),
      from:       this.flFrom() || undefined,
      to:         this.flTo()   || undefined,
    };
  }

  private loadKpis(): void {
    forkJoin({
      all:       this.svc.search({}, 0, 1),
      success:   this.svc.search({ status: 'SUCCESS' }, 0, 1),
      failed:    this.svc.search({ status: 'FAILED' }, 0, 1),
      initiated: this.svc.search({ status: 'INITIATED' }, 0, 1),
      collected: this.svc.getTotalCollected(),
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: ({ all, success, failed, initiated, collected }) => {
        this.kpiTotal.set(all.totalElements);
        this.kpiSuccess.set(success.totalElements);
        this.kpiFailed.set(failed.totalElements);
        this.kpiInitiated.set(initiated.totalElements);
        this.kpiTotalCollected.set(collected);
      },
    });
  }

  /* ── Filtres ──────────────────────────────────────────── */
  setTab(tab: PaymentStatus | ''): void {
    this.flStatus.set(tab);
    this.page.set(0);
    this.loadTransactions();
  }

  onSearchClick(): void {
    this.page.set(0);
    this.loadTransactions();
  }

  clearFilters(): void {
    this.flHawb.set('');
    this.flClient.set('');
    this.flGateway.set('');
    this.flStatus.set('');
    this.flAmountMin.set('');
    this.flAmountMax.set('');
    this.flFrom.set('');
    this.flTo.set('');
    this.page.set(0);
    this.loadTransactions();
  }

  /* ── Pagination ───────────────────────────────────────── */
  goToPage(p: number): void { this.page.set(p); this.loadTransactions(); }
  pagesArray(): number[]    { return Array.from({ length: this.totalPages() }, (_, i) => i); }
  onPageSizeChange(size: number): void { this.pageSize.set(size); this.page.set(0); this.loadTransactions(); }

  /* ── Helpers ──────────────────────────────────────────── */
  fmtAmount(n?: number | null, currency?: string | null): string {
    if (n == null) return '—';
    const cur = currency ?? 'USD';
    return n.toLocaleString('fr-MA', { minimumFractionDigits: 2, maximumFractionDigits: 2 }) + ' ' + cur;
  }

  /* ── Sélection ────────────────────────────────────────── */
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
      : this.selectedIds.set(new Set(this.transactions().map(t => t.id)));
  }

  isSelected(id: number): boolean { return this.selectedIds().has(id); }
  clearSelection(): void          { this.selectedIds.set(new Set()); }

  /* ── Export ───────────────────────────────────────────── */
  exportPdf(): void {
    this.exportOpen.set(false);
    this.svc.exportPdf(this.buildParams())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => this.triggerDownload(blob, 'transactions.pdf'),
        error: () => this.toast.error('Erreur lors de l\'export PDF.'),
      });
  }

  exportExcel(): void {
    this.exportOpen.set(false);
    this.svc.exportExcel(this.buildParams())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => this.triggerDownload(blob, 'transactions.xlsx'),
        error: () => this.toast.error('Erreur lors de l\'export Excel.'),
      });
  }

  exportSelection(): void {
    if (this.selectedIds().size === 0) {
      this.toast.error('Veuillez sélectionner au moins une transaction.');
      return;
    }
    this.exportOpen.set(false);
    this.svc.exportExcelSelection(Array.from(this.selectedIds()))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => this.triggerDownload(blob, 'transactions_selection.xlsx'),
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

  /* ── Envoi manuel du reçu ─────────────────────────────────── */
  sendReceipt(transaction: PaymentTransactionResponse): void {
    this.sendingReceiptId.set(transaction.id);
    this.svc.sendReceipt(transaction.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.sendingReceiptId.set(null);
          if (res.success) {
            this.toast.success(res.message || 'Reçu envoyé avec succès.');
            this.loadTransactions();
          } else {
            this.toast.error(res.message || 'Échec de l\'envoi du reçu.');
          }
        },
        error: err => {
          this.sendingReceiptId.set(null);
          this.toast.error(err?.error?.message || 'Erreur lors de l\'envoi du reçu.');
        },
      });
  }

  /* ── Permissions ──────────────────────────────────── */
  canView(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('PAYMENTS', 'VIEW');
  }

  canExport(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('PAYMENTS', 'EXPORT');
  }

  canSendReceipt(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('PAYMENTS', 'SEND_RECEIPT');
  }
}
