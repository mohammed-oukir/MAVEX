import {
  ChangeDetectionStrategy, Component, DestroyRef,
  ElementRef, OnInit, ViewChild, computed, inject, signal,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';

import { LayoutService }       from '../../core/services/layout.service';
import { ToastService }        from '../../core/services/toast.service';
import { AuthService }         from '../../core/auth/auth.service';
import { ExchangeRateService } from '../../core/services/exchange-rate.service';
import {
  ExchangeRate,
  ExchangeRateImportResult,
} from '../../core/models/exchange-rate.model';

@Component({
  selector: 'app-exchange-rates',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './exchange-rates.component.html',
  styleUrl:    './exchange-rates.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ExchangeRatesComponent implements OnInit {
  private readonly layout  = inject(LayoutService);
  private readonly toast   = inject(ToastService);
  private readonly auth    = inject(AuthService);
  private readonly router  = inject(Router);
  private readonly erSvc   = inject(ExchangeRateService);
  private readonly fb      = inject(FormBuilder);
  private readonly destroy = inject(DestroyRef);

  @ViewChild('fileInput') fileInput!: ElementRef<HTMLInputElement>;

  // ── Access guard ──────────────────────────────────────────
  private readonly canAccess = computed(() =>
    this.auth.isAdmin() || this.auth.isComptable()
  );

  // ── Data ──────────────────────────────────────────────────
  readonly allRates = signal<ExchangeRate[]>([]);
  readonly loading  = signal(true);

  // ── Filters ───────────────────────────────────────────────
  readonly fFrom   = signal('');
  readonly fTo     = signal('');
  readonly fActive = signal<'all' | 'active' | 'inactive'>('all');

  // ── Filtered list ─────────────────────────────────────────
  readonly filtered = computed(() => {
    const from   = this.fFrom().toUpperCase();
    const to     = this.fTo().toUpperCase();
    const active = this.fActive();
    return this.allRates().filter(r => {
      if (from   && !r.fromCurrency.includes(from)) return false;
      if (to     && !r.toCurrency.includes(to))     return false;
      if (active === 'active'   && !r.isActive)     return false;
      if (active === 'inactive' &&  r.isActive)     return false;
      return true;
    });
  });

  readonly hasFilters = computed(() =>
    !!this.fFrom() || !!this.fTo() || this.fActive() !== 'all'
  );

  // ── KPIs ──────────────────────────────────────────────────
  readonly kpiTotal  = computed(() => this.allRates().length);
  readonly kpiActive = computed(() => this.allRates().filter(r => r.isActive).length);
  readonly kpiPairs  = computed(() => {
    const pairs = new Set(this.allRates().map(r => `${r.fromCurrency}/${r.toCurrency}`));
    return pairs.size;
  });
  readonly kpiLastUpdate = computed(() => {
    const dates = this.allRates()
      .map(r => r.updatedAt ?? r.createdAt)
      .filter((d): d is string => !!d)
      .map(d => new Date(d).getTime());
    return dates.length ? Math.max(...dates) : null;
  });

  // ── Quick Lookup ──────────────────────────────────────────
  readonly lookupFrom    = signal('USD');
  readonly lookupTo      = signal('MAD');
  readonly lookupResult  = signal<ExchangeRate | null>(null);
  readonly lookupError   = signal<string | null>(null);
  readonly lookupLoading = signal(false);

  // ── View toggle ───────────────────────────────────────────
  readonly viewMode       = signal<'current' | 'history'>('current');
  readonly historyDate    = signal<string>('');
  readonly historyRates   = signal<ExchangeRate[]>([]);
  readonly historyLoading = signal(false);

  // ── Form modal ────────────────────────────────────────────
  readonly formMode  = signal<'create' | 'edit' | null>(null);
  readonly editingId = signal<number | null>(null);
  readonly saving    = signal(false);

  readonly form = this.fb.group({
    fromCurrency:  ['', [Validators.required, Validators.maxLength(3), Validators.pattern(/^[A-Z]{2,3}$/)]],
    toCurrency:    ['', [Validators.required, Validators.maxLength(3), Validators.pattern(/^[A-Z]{2,3}$/)]],
    rate:          [null as number | null, [Validators.required, Validators.min(0.000001)]],
    effectiveDate: ['', Validators.required],
    source:        [''],
  });

  // ── Delete confirmation ───────────────────────────────────
  readonly deleteId    = signal<number | null>(null);
  readonly deleteLabel = signal('');
  readonly deleting    = signal(false);

  // ── Import ────────────────────────────────────────────────
  readonly importing    = signal(false);
  readonly importResult = signal<ExchangeRateImportResult | null>(null);

  // ── Known currencies for selects ─────────────────────────
  readonly currencies = signal<string[]>([]);

  // ── Lifecycle ─────────────────────────────────────────────
  ngOnInit(): void {
    if (!this.canAccess()) {
      this.router.navigate(['/403']);
      return;
    }
    this.layout.setPage('Taux de change');
    this.loadRates();
    this.erSvc.getCurrencies()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: list => this.currencies.set(
          list.length ? list : ['USD', 'EUR', 'MAD']
        ),
      });
  }

  // ── Load ──────────────────────────────────────────────────
  loadRates(): void {
    this.loading.set(true);
    this.erSvc.getAll()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: rates => { this.allRates.set(rates); this.loading.set(false); },
        error: ()   => { this.loading.set(false); this.toast.error('Erreur lors du chargement des taux.'); },
      });
  }

  // ── Filters ───────────────────────────────────────────────
  clearFilters(): void {
    this.fFrom.set('');
    this.fTo.set('');
    this.fActive.set('all');
  }

  // ── Quick Lookup ──────────────────────────────────────────
  onLookupFromChange(val: string): void {
    this.lookupFrom.set(val);
    this.lookupResult.set(null);
    this.lookupError.set(null);
  }

  onLookupToChange(val: string): void {
    this.lookupTo.set(val);
    this.lookupResult.set(null);
    this.lookupError.set(null);
  }

  doLookup(): void {
    const from = this.lookupFrom();
    const to   = this.lookupTo();
    if (!from || !to) return;
    if (from.toLowerCase() === to.toLowerCase()) {
      this.lookupResult.set(null);
      this.lookupError.set('Les deux devises ne peuvent pas être identiques');
      return;
    }
    this.lookupLoading.set(true);
    this.lookupResult.set(null);
    this.lookupError.set(null);
    const date = this.viewMode() === 'history' ? this.historyDate() : '';
    const obs  = date
      ? this.erSvc.lookup(from, to, date)
      : this.erSvc.getActive(from, to);
    obs
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: r  => { this.lookupResult.set(r);  this.lookupLoading.set(false); },
        error: (err: { error?: { message?: string } }) => {
          this.lookupResult.set(null);
          this.lookupError.set(err?.error?.message ?? `Aucun taux actif trouvé pour ${from} → ${to}`);
          this.lookupLoading.set(false);
        },
      });
  }

  switchToCurrent(): void {
    this.viewMode.set('current');
    this.historyDate.set('');
    this.historyRates.set([]);
    this.lookupResult.set(null);
    this.lookupError.set(null);
  }

  switchToHistory(): void {
    this.viewMode.set('history');
    this.lookupResult.set(null);
    this.lookupError.set(null);
  }

  onHistoryDateChange(date: string): void {
    this.historyDate.set(date);
    if (date) {
      this.loadHistoryRates(date);
      if (this.lookupFrom() !== this.lookupTo()) {
        this.doLookup();
      }
    }
  }

  loadHistoryRates(date: string): void {
    this.historyLoading.set(true);
    this.erSvc.getAll(undefined, undefined, undefined, date)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next:  rates => { this.historyRates.set(rates); this.historyLoading.set(false); },
        error: ()    => { this.historyLoading.set(false); },
      });
  }

  // ── Create / Edit ─────────────────────────────────────────
  openCreate(): void {
    this.form.reset();
    this.editingId.set(null);
    this.formMode.set('create');
  }

  openEdit(r: ExchangeRate): void {
    this.form.reset({
      fromCurrency:  r.fromCurrency,
      toCurrency:    r.toCurrency,
      rate:          r.rate,
      effectiveDate: r.effectiveDate,
      source:        r.source ?? '',
    });
    this.editingId.set(r.id);
    this.formMode.set('edit');
  }

  closeForm(): void { this.formMode.set(null); }

  saveForm(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const v = this.form.getRawValue();
    const req = {
      fromCurrency:  v.fromCurrency!.toUpperCase(),
      toCurrency:    v.toCurrency!.toUpperCase(),
      rate:          v.rate!,
      effectiveDate: v.effectiveDate!,
      source:        v.source || undefined,
    };
    const id  = this.editingId();
    const obs = id ? this.erSvc.update(id, req) : this.erSvc.create(req);

    obs.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: saved => {
        this.saving.set(false);
        this.formMode.set(null);
        if (id) {
          this.allRates.update(list => list.map(r => r.id === saved.id ? saved : r));
          this.toast.success('Taux modifié avec succès.');
        } else {
          this.allRates.update(list => [saved, ...list]);
          this.toast.success('Taux créé avec succès.');
        }
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err?.error?.message || 'Erreur lors de la sauvegarde.');
      },
    });
  }

  fieldInvalid(f: string): boolean {
    const c = this.form.get(f);
    return !!(c?.invalid && c?.touched);
  }

  // ── Delete ────────────────────────────────────────────────
  confirmDelete(r: ExchangeRate): void {
    this.deleteId.set(r.id);
    this.deleteLabel.set(`${r.fromCurrency} → ${r.toCurrency} (${r.rate})`);
  }

  cancelDelete(): void { this.deleteId.set(null); }

  executeDelete(): void {
    const id = this.deleteId();
    if (!id) return;
    this.deleting.set(true);
    this.erSvc.delete(id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: () => {
          this.deleting.set(false);
          this.deleteId.set(null);
          this.allRates.update(list => list.filter(r => r.id !== id));
          this.toast.success('Taux supprimé.');
        },
        error: err => {
          this.deleting.set(false);
          this.toast.error(err?.error?.message || 'Erreur lors de la suppression.');
        },
      });
  }

  // ── Import ────────────────────────────────────────────────
  triggerFileInput(): void { this.fileInput.nativeElement.click(); }

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    const file  = input.files?.[0];
    if (!file) return;
    this.importing.set(true);
    this.importResult.set(null);
    this.erSvc.importFile(file)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: result => {
          this.importing.set(false);
          this.importResult.set(result);
          input.value = '';
          if (result.imported > 0) {
            this.loadRates();
            this.toast.success(`${result.imported} taux importé(s) avec succès.`);
          } else {
            this.toast.error('Aucun taux importé. Vérifiez le fichier.');
          }
        },
        error: err => {
          this.importing.set(false);
          input.value = '';
          this.toast.error(err?.error?.message || "Erreur lors de l'import.");
        },
      });
  }

  downloadTemplate(): void {
    const csv = [
      'fromCurrency,toCurrency,rate,effectiveDate,source,isActive',
      'USD,MAD,9.95,2026-06-22,Banque Al-Maghrib,true',
      'EUR,MAD,10.80,2026-06-22,Banque Al-Maghrib,true',
    ].join('\n');
    const blob = new Blob([csv], { type: 'text/csv' });
    const url  = URL.createObjectURL(blob);
    const a    = document.createElement('a');
    a.href     = url;
    a.download = 'exchange-rates-template.csv';
    a.click();
    URL.revokeObjectURL(url);
  }

  // ── Helpers ───────────────────────────────────────────────
  trackById(_: number, r: ExchangeRate): number { return r.id; }

  formatRate(r: number): string {
    return r.toLocaleString('fr-FR', { minimumFractionDigits: 4, maximumFractionDigits: 6 });
  }

  private readonly CURRENCY_COLORS: Record<string, string> = {
    USD: '#2563EB',
    EUR: '#7C3AED',
    MAD: '#F97316',
    GBP: '#16A34A',
    JPY: '#DC2626',
    CAD: '#0891B2',
  };

  getCurrencyColor(currency: string): string {
    return this.CURRENCY_COLORS[currency] ?? '#374151';
  }
}
