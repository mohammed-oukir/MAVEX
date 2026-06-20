import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService }   from '../../core/services/layout.service';
import { ShipperService }  from '../../core/services/shipper.service';
import { ToastService }    from '../../core/services/toast.service';
import { ShipperResponse } from '../../core/models/shipper.model';
import { RequiresPermissionDirective } from '../../core/directives/requires-permission.directive';

@Component({
  selector: 'app-shippers',
  imports: [ReactiveFormsModule, DatePipe, RequiresPermissionDirective],
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

  /* ── Data ─────────────────────────────────────────── */
  allShippers = signal<ShipperResponse[]>([]);
  loading     = signal(true);
  page        = signal(0);
  readonly pageSize = 15;

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

  /* ── Filtered + paginated ─────────────────────────── */
  filtered = computed(() => {
    const co = this.fCompany().toLowerCase();
    const cn = this.fContact().toLowerCase();
    const em = this.fEmail().toLowerCase();
    const ph = this.fPhone().toLowerCase();
    const ci = this.fCity().toLowerCase();
    const ct = this.fCountry().toLowerCase();
    const st = this.fStatus();

    return this.allShippers().filter(s => {
      if (co && !s.companyName?.toLowerCase().includes(co))  return false;
      if (cn && !s.contactName?.toLowerCase().includes(cn))  return false;
      if (em && !s.email?.toLowerCase().includes(em))        return false;
      if (ph && !s.phone?.toLowerCase().includes(ph))        return false;
      if (ci && !s.city?.toLowerCase().includes(ci))         return false;
      if (ct) {
        const cc = s.countryCode?.code?.toLowerCase() ?? '';
        if (!cc.includes(ct)) return false;
      }
      if (st === 'active'   && !s.active)  return false;
      if (st === 'inactive' && s.active)   return false;
      return true;
    });
  });

  total      = computed(() => this.filtered().length);
  totalPages = computed(() => Math.ceil(this.total() / this.pageSize));
  pageItems  = computed(() => {
    const start = this.page() * this.pageSize;
    return this.filtered().slice(start, start + this.pageSize);
  });
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
    this.loadShippers();
  }

  /* ── Load ─────────────────────────────────────────── */
  loadShippers(): void {
    this.loading.set(true);
    this.shipperSvc.getAll(500)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: shippers => { this.allShippers.set(shippers); this.loading.set(false); },
        error: ()       => this.loading.set(false),
      });
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
        if (isEdit) {
          this.allShippers.update(list => list.map(s => s.id === data.id ? data : s));
        } else {
          this.allShippers.update(list => [data, ...list]);
          this.page.set(0);
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

  /* ── Toggle actif ─────────────────────────────────── */
  toggleActive(s: ShipperResponse): void {
    this.toggling.set(s.id);
    const obs = s.active ? this.shipperSvc.deactivate(s.id) : this.shipperSvc.activate(s.id);
    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.toggling.set(null);
        this.allShippers.update(list =>
          list.map(x => x.id === s.id ? { ...x, active: !s.active } : x)
        );
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
          this.allShippers.update(list => list.filter(s => s.id !== id));
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
        this.allShippers.update(list => list.filter(s => !ids.includes(s.id)));
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
        this.allShippers.update(list => list.map(s => ids.includes(s.id) ? { ...s, active: true } : s));
        this.selectedIds.set(new Set());
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
        this.allShippers.update(list => list.map(s => ids.includes(s.id) ? { ...s, active: false } : s));
        this.selectedIds.set(new Set());
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

