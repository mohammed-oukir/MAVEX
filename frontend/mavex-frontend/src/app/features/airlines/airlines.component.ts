import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService }  from '../../core/services/layout.service';
import { AirlineService } from '../../core/services/airline.service';
import { ToastService }   from '../../core/services/toast.service';
import { AuthService }    from '../../core/auth/auth.service';
import { MyPermissionsService } from '../../core/services/my-permissions.service';
import { Airline }        from '../../core/models/airline.model';

@Component({
  selector: 'app-airlines',
  imports: [ReactiveFormsModule],
  templateUrl: './airlines.component.html',
  styleUrl: './airlines.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class AirlinesComponent implements OnInit {
  private readonly layout      = inject(LayoutService);
  private readonly airlineSvc  = inject(AirlineService);
  private readonly toast       = inject(ToastService);
  private readonly fb          = inject(FormBuilder);
  private readonly destroyRef  = inject(DestroyRef);
  protected readonly auth          = inject(AuthService);
  protected readonly myPermissions = inject(MyPermissionsService);

  /* ── Data ─────────────────────────────────────────── */
  allAirlines = signal<Airline[]>([]);
  loading     = signal(true);

  /* ── Filters ──────────────────────────────────────── */
  fSearch = signal('');
  fMode   = signal('');

  filtered = computed(() => {
    const q = this.fSearch().toLowerCase();
    const m = this.fMode();
    return this.allAirlines().filter(a => {
      if (m && a.mode !== m) return false;
      if (!q) return true;
      return (
        a.prefix.includes(q) ||
        a.name?.toLowerCase().includes(q) ||
        a.iataCode?.toLowerCase().includes(q) ||
        a.icaoCode?.toLowerCase().includes(q) ||
        a.country?.toLowerCase().includes(q)
      );
    });
  });

  total = computed(() => this.filtered().length);

  /* ── Modals ───────────────────────────────────────── */
  formMode  = signal<'create' | 'edit' | null>(null);
  editingPrefix = signal<string | null>(null);
  deletePrefix  = signal<string | null>(null);
  saving    = signal(false);
  deleting  = signal(false);

  /* ── Form ─────────────────────────────────────────── */
  form = this.fb.group({
    prefix:   ['', [Validators.required, Validators.pattern(/^\d{3}$/)]],
    name:     ['', Validators.required],
    iataCode: ['', Validators.maxLength(2)],
    icaoCode: ['', Validators.maxLength(3)],
    country:  [''],
    mode:     ['AIR', Validators.required],
  });

  ngOnInit(): void {
    this.layout.setPage('Compagnies aériennes');
    this.load();
  }

  private load(): void {
    this.loading.set(true);
    this.airlineSvc.getAll().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: list => { this.allAirlines.set(list); this.loading.set(false); },
      error: ()   => this.loading.set(false),
    });
  }

  openCreate(): void {
    this.form.reset({ mode: 'AIR' });
    this.form.get('prefix')?.enable();
    this.editingPrefix.set(null);
    this.formMode.set('create');
  }

  openEdit(a: Airline): void {
    this.form.reset({
      prefix:   a.prefix,
      name:     a.name,
      iataCode: a.iataCode ?? '',
      icaoCode: a.icaoCode ?? '',
      country:  a.country  ?? '',
      mode:     a.mode     ?? 'AIR',
    });
    this.form.get('prefix')?.disable();
    this.editingPrefix.set(a.prefix);
    this.formMode.set('edit');
  }

  closeForm(): void { this.formMode.set(null); }

  save(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const v = this.form.getRawValue();
    const obs = this.editingPrefix()
      ? this.airlineSvc.update(this.editingPrefix()!, v as Airline)
      : this.airlineSvc.create(v as Airline);

    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving.set(false);
        this.formMode.set(null);
        this.toast.success(this.editingPrefix() ? 'Compagnie mise à jour.' : 'Compagnie ajoutée.');
        this.load();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err?.error?.message || 'Erreur.');
      },
    });
  }

  fieldInvalid(f: string): boolean {
    const c = this.form.get(f);
    return !!(c?.invalid && c?.touched);
  }

  confirmDelete(prefix: string): void { this.deletePrefix.set(prefix); }
  cancelDelete(): void { this.deletePrefix.set(null); }

  executeDelete(): void {
    const p = this.deletePrefix();
    if (!p) return;
    this.deleting.set(true);
    this.airlineSvc.delete(p).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deletePrefix.set(null);
        this.allAirlines.update(list => list.filter(a => a.prefix !== p));
        this.toast.success('Compagnie supprimée.');
      },
      error: () => { this.deleting.set(false); this.toast.error('Erreur suppression.'); },
    });
  }

  modeLabel(mode: string | undefined): string {
    return mode === 'CARGO' ? 'Cargo' : 'Passagers';
  }

  canCreate(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('AIRLINES', 'CREATE');
  }

  canUpdate(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('AIRLINES', 'UPDATE');
  }

  canDelete(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('AIRLINES', 'DELETE');
  }
}
