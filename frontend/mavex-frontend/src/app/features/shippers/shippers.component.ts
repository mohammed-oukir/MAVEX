import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { LayoutService }   from '../../core/services/layout.service';
import { ShipperService }  from '../../core/services/shipper.service';
import { ToastService }    from '../../core/services/toast.service';
import { ShipperResponse, ShipperSearchCriteria } from '../../core/models/shipper.model';

@Component({
  selector: 'app-shippers',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './shippers.component.html',
  styleUrl: './shippers.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ShippersComponent implements OnInit {
  private readonly layout     = inject(LayoutService);
  private readonly shipperSvc = inject(ShipperService);
  private readonly toast      = inject(ToastService);
  private readonly fb         = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  /* ── Data (recherche paginée côté serveur) ─────────── */
  pageItems      = signal<ShipperResponse[]>([]);
  totalElements  = signal(0);
  totalPages     = signal(0);
  loading        = signal(true);
  page           = signal(0);
  readonly pageSize = 15;

  /* ── Data KPIs (liste complète, indépendante de la pagination) ── */
  allShippers = signal<ShipperResponse[]>([]);

  /* ── Filtres par colonne ──────────────────────────── */
  fCompany = signal('');
  fContact = signal('');
  fEmail   = signal('');
  fPhone   = signal('');
  fCity    = signal('');
  fCountry = signal('');
  fStatus  = signal<'all' | 'active' | 'inactive'>('all');

  /* ── KPIs ─────────────────────────────────────────── */
  kpiTotal    = computed(() => this.allShippers().length);
  kpiActive   = computed(() => this.allShippers().filter(s => s.active).length);
  kpiInactive = computed(() => this.allShippers().filter(s => !s.active).length);
  kpiMonth    = computed(() => {
    const now = new Date();
    return this.allShippers().filter(s => {
      if (!s.createdAt) return false;
      const d = new Date(s.createdAt);
      return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
    }).length;
  });

  /* ── Critères combinés → déclenchent la recherche serveur ── */
  private readonly searchParams = computed(() => ({
    criteria: {
      company: this.fCompany(),
      contact: this.fContact(),
      email:   this.fEmail(),
      phone:   this.fPhone(),
      city:    this.fCity(),
      country: this.fCountry(),
      status:  this.fStatus(),
    } satisfies ShipperSearchCriteria,
    page: this.page(),
  }));

  /* ── Observable des critères — créé en champ de classe (contexte d'injection
     valide via le constructeur), consommé dans ngOnInit() ── */
  private readonly searchParams$ = toObservable(this.searchParams);

  total = computed(() => this.totalElements());
  hasFilters = computed(() =>
    !!this.fCompany() || !!this.fContact() || !!this.fEmail() ||
    !!this.fPhone()   || !!this.fCity()    || !!this.fCountry() ||
    this.fStatus() !== 'all'
  );

  /* ── Sélection bulk ──────────────────────────────── */
  selectedIds       = signal<Set<number>>(new Set());
  bulkActing        = signal(false);
  bulkDeleteConfirm = signal(false);

  allSelected = computed(() => {
    const items = this.pageItems();
    return items.length > 0 && items.every(s => this.selectedIds().has(s.id));
  });
  selectedCount = computed(() => this.selectedIds().size);

  /* ── Modals ───────────────────────────────────────── */
  formMode   = signal<'create' | 'edit' | null>(null);
  editingId  = signal<number | null>(null);
  deleteId   = signal<number | null>(null);
  saving     = signal(false);
  deleting   = signal(false);
  toggling   = signal<number | null>(null);

  /* ── Form ─────────────────────────────────────────── */
  form = this.fb.group({
    companyName: ['', [Validators.required, Validators.minLength(2)]],
    email:       ['', [Validators.required, Validators.email]],
    contactName: [''],
    phone:       [''],
    address:     [''],
    city:        [''],
    countryCode: ['MA', [Validators.pattern(/^[A-Z]{2}$/)]],
  });

  /* ── Lifecycle ────────────────────────────────────── */
  ngOnInit(): void {
    this.layout.setPage('Shippers');
    this.loadKpiData();

    this.searchParams$
      .pipe(
        debounceTime(300),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        switchMap(({ criteria, page }) => {
          this.loading.set(true);
          return this.shipperSvc.search(criteria, page, this.pageSize);
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe({
        next: result => {
          this.pageItems.set(result.content);
          this.totalElements.set(result.totalElements);
          this.totalPages.set(result.totalPages);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  /* ── Load KPIs (liste complète, indépendante des filtres/pagination) ── */
  private loadKpiData(): void {
    this.shipperSvc.getAll(500)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: shippers => this.allShippers.set(shippers),
        error: () => {},
      });
  }

  /* ── Rafraîchit la page courante après une mutation (create/edit/delete/…) ── */
  private reloadCurrentPage(): void {
    const { criteria, page } = this.searchParams();
    this.loading.set(true);
    this.shipperSvc.search(criteria, page, this.pageSize)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: result => {
          this.pageItems.set(result.content);
          this.totalElements.set(result.totalElements);
          this.totalPages.set(result.totalPages);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
    this.loadKpiData();
  }

  /* ── Filtres ──────────────────────────────────────── */
  setFilter(f: 'company' | 'contact' | 'email' | 'phone' | 'city' | 'country', val: string): void {
    this.page.set(0);
    if (f === 'company') this.fCompany.set(val);
    if (f === 'contact') this.fContact.set(val);
    if (f === 'email')   this.fEmail.set(val);
    if (f === 'phone')   this.fPhone.set(val);
    if (f === 'city')    this.fCity.set(val);
    if (f === 'country') this.fCountry.set(val);
  }

  setStatus(s: 'all' | 'active' | 'inactive'): void { this.fStatus.set(s); this.page.set(0); }

  clearFilters(): void {
    this.fCompany.set(''); this.fContact.set(''); this.fEmail.set('');
    this.fPhone.set('');   this.fCity.set('');    this.fCountry.set('');
    this.fStatus.set('all'); this.page.set(0);
  }

  /* ── Create / Edit ────────────────────────────────── */
  openCreate(): void {
    this.form.reset({ countryCode: 'MA' });
    this.editingId.set(null);
    this.formMode.set('create');
  }

  openEdit(s: ShipperResponse): void {
    this.form.reset({
      companyName: s.companyName,
      email:       s.email       ?? '',
      contactName: s.contactName ?? '',
      phone:       s.phone       ?? '',
      address:     s.address     ?? '',
      city:        s.city        ?? '',
      countryCode: s.countryCode?.code ?? 'MA',
    });
    this.editingId.set(s.id);
    this.formMode.set('edit');
  }

  closeForm(): void { this.formMode.set(null); }

  saveForm(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const v = this.form.getRawValue();
    const req = {
      companyName: v.companyName!,
      email:       v.email!,
      contactName: v.contactName || undefined,
      phone:       v.phone       || undefined,
      address:     v.address     || undefined,
      city:        v.city        || undefined,
      countryCode: v.countryCode || undefined,
    };

    const obs = this.editingId()
      ? this.shipperSvc.update(this.editingId()!, req)
      : this.shipperSvc.create(req);

    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: ({ data }) => {
        this.saving.set(false);
        this.formMode.set(null);
        const isEdit = !!this.editingId();
        this.toast.success(isEdit ? 'Shipper modifié.' : 'Shipper créé.');
        if (!isEdit) this.page.set(0);
        this.reloadCurrentPage();
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

  /* ── Toggle actif ─────────────────────────────────── */
  toggleActive(s: ShipperResponse): void {
    this.toggling.set(s.id);
    const obs = s.active ? this.shipperSvc.deactivate(s.id) : this.shipperSvc.activate(s.id);
    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.toggling.set(null);
        this.reloadCurrentPage();
        this.toast.success(s.active ? 'Shipper désactivé.' : 'Shipper activé.');
      },
      error: () => { this.toggling.set(null); this.toast.error('Erreur.'); },
    });
  }

  /* ── Delete ───────────────────────────────────────── */
  confirmDelete(id: number): void { this.deleteId.set(id); }
  cancelDelete(): void            { this.deleteId.set(null); }

  executeDelete(): void {
    const id = this.deleteId();
    if (!id) return;
    this.deleting.set(true);
    this.shipperSvc.delete(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deleting.set(false);
          this.deleteId.set(null);
          this.reloadCurrentPage();
          this.toast.success('Shipper supprimé.');
        },
        error: () => { this.deleting.set(false); this.toast.error('Erreur suppression.'); },
      });
  }

  /* ── Bulk actions ────────────────────────────────── */
  toggleSelect(id: number): void {
    this.selectedIds.update(s => {
      const next = new Set(s);
      next.has(id) ? next.delete(id) : next.add(id);
      return next;
    });
  }

  toggleSelectAll(): void {
    if (this.allSelected()) {
      this.selectedIds.update(s => {
        const next = new Set(s);
        this.pageItems().forEach(sh => next.delete(sh.id));
        return next;
      });
    } else {
      this.selectedIds.update(s => {
        const next = new Set(s);
        this.pageItems().forEach(sh => next.add(sh.id));
        return next;
      });
    }
  }

  clearSelection(): void { this.selectedIds.set(new Set()); }

  openBulkDeleteConfirm(): void  { this.bulkDeleteConfirm.set(true); }
  closeBulkDeleteConfirm(): void { this.bulkDeleteConfirm.set(false); }

  executeBulkDelete(): void {
    const ids = [...this.selectedIds()];
    this.bulkActing.set(true);
    this.shipperSvc.bulkDelete(ids).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.bulkActing.set(false);
        this.bulkDeleteConfirm.set(false);
        this.selectedIds.set(new Set());
        this.reloadCurrentPage();
        this.toast.success(`${res.deleted} shipper(s) supprimé(s).`);
      },
      error: () => { this.bulkActing.set(false); this.toast.error('Erreur suppression en masse.'); },
    });
  }

  executeBulkActivate(): void {
    const ids = [...this.selectedIds()];
    this.bulkActing.set(true);
    this.shipperSvc.bulkActivate(ids).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.bulkActing.set(false);
        this.selectedIds.set(new Set());
        this.reloadCurrentPage();
        this.toast.success(`${res.activated} shipper(s) activé(s).`);
      },
      error: () => { this.bulkActing.set(false); this.toast.error('Erreur activation en masse.'); },
    });
  }

  executeBulkDeactivate(): void {
    const ids = [...this.selectedIds()];
    this.bulkActing.set(true);
    this.shipperSvc.bulkDeactivate(ids).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.bulkActing.set(false);
        this.selectedIds.set(new Set());
        this.reloadCurrentPage();
        this.toast.success(`${res.deactivated} shipper(s) désactivé(s).`);
      },
      error: () => { this.bulkActing.set(false); this.toast.error('Erreur désactivation en masse.'); },
    });
  }

  /* ── Pagination ───────────────────────────────────── */
  goToPage(p: number): void { this.page.set(p); }
  pages(): number[]         { return Array.from({ length: this.totalPages() }, (_, i) => i); }

  /* ── Helpers ──────────────────────────────────────── */
  getInitials(name: string): string {
    return name.split(' ').slice(0, 2).map(w => w[0] ?? '').join('').toUpperCase();
  }

}

