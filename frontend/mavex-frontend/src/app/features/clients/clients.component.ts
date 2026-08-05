import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { LayoutService }  from '../../core/services/layout.service';
import { ClientService }  from '../../core/services/client.service';
import { OrderService }   from '../../core/services/order.service';
import { ToastService }   from '../../core/services/toast.service';
import { ClientResponse, ClientSearchCriteria } from '../../core/models/client.model';
import { OrderResponse }  from '../../core/models/order.model';
import { BadgeComponent } from '../../shared/badge/badge.component';

@Component({
  selector: 'app-clients',
  imports: [ReactiveFormsModule, DatePipe, BadgeComponent],
  templateUrl: './clients.component.html',
  styleUrl: './clients.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ClientsComponent implements OnInit {
  private readonly layout     = inject(LayoutService);
  private readonly clientSvc  = inject(ClientService);
  private readonly orderSvc   = inject(OrderService);
  private readonly toast      = inject(ToastService);
  private readonly fb         = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  /* ── Data (recherche paginée côté serveur) ─────────── */
  pageItems      = signal<ClientResponse[]>([]);
  totalElements  = signal(0);
  totalPages     = signal(0);
  loading        = signal(true);
  page           = signal(0);
  readonly pageSize = 15;

  /* ── Data KPIs (liste complète, indépendante de la pagination) ── */
  allClients = signal<ClientResponse[]>([]);

  /* ── Column filters ───────────────────────────────── */
  fName      = signal('');
  fEmail     = signal('');
  fPhone     = signal('');
  fCity      = signal('');
  fState     = signal('');
  fCountry   = signal('');
  fStatus    = signal<'all' | 'active' | 'inactive'>('all');
  fDateFrom  = signal('');
  fDateTo    = signal('');

  /* ── KPIs ─────────────────────────────────────────── */
  kpiTotal    = computed(() => this.allClients().length);
  kpiActive   = computed(() => this.allClients().filter(c => c.active).length);
  kpiInactive = computed(() => this.allClients().filter(c => !c.active).length);
  kpiMonth    = computed(() => {
    const now = new Date();
    return this.allClients().filter(c => {
      if (!c.createdAt) return false;
      const d = new Date(c.createdAt);
      return d.getMonth() === now.getMonth() && d.getFullYear() === now.getFullYear();
    }).length;
  });

  /* ── Critères combinés → déclenchent la recherche serveur ── */
  private readonly searchParams = computed(() => ({
    criteria: {
      name:    this.fName(),
      email:   this.fEmail(),
      phone:   this.fPhone(),
      city:    this.fCity(),
      state:   this.fState(),
      country: this.fCountry(),
      status:  this.fStatus(),
      dateFrom: this.fDateFrom(),
      dateTo:   this.fDateTo(),
    } satisfies ClientSearchCriteria,
    page: this.page(),
  }));

  /* ── Observable des critères — créé en champ de classe (contexte d'injection
     valide via le constructeur), consommé dans ngOnInit() ── */
  private readonly searchParams$ = toObservable(this.searchParams);

  total      = computed(() => this.totalElements());
  hasFilters = computed(() =>
    !!this.fName() || !!this.fEmail() || !!this.fPhone() ||
    !!this.fCity() || !!this.fState() || !!this.fCountry() ||
    this.fStatus() !== 'all' || !!this.fDateFrom() || !!this.fDateTo()
  );

  /* ── Sélection bulk ──────────────────────────────── */
  selectedIds   = signal<Set<number>>(new Set());
  bulkActing    = signal(false);
  bulkDeleteConfirm = signal(false);

  allSelected = computed(() => {
    const items = this.pageItems();
    return items.length > 0 && items.every(c => this.selectedIds().has(c.id));
  });
  selectedCount = computed(() => this.selectedIds().size);

  /* ── Modals ───────────────────────────────────────── */
  formMode      = signal<'create' | 'edit' | null>(null);
  editingId     = signal<number | null>(null);
  detailClient  = signal<ClientResponse | null>(null);
  detailOrders  = signal<OrderResponse[]>([]);
  detailLoading = signal(false);
  deleteId      = signal<number | null>(null);
  saving        = signal(false);
  deleting      = signal(false);
  toggling      = signal<number | null>(null);
  lastSavedId   = signal<number | null>(null);

  /* ── Form ─────────────────────────────────────────── */
  form = this.fb.group({
    fullName:    ['', [Validators.required, Validators.minLength(2)]],
    email:       ['', [Validators.required, Validators.email]],
    phone:       [''],
    address:     [''],
    city:        [''],
    state:       ['', [Validators.pattern(/^[A-Z]{0,2}$/)]],
    zipCode:     [''],
    countryCode: ['US', [Validators.required, Validators.pattern(/^[A-Z]{2}$/)]],
  });

  /* ── Lifecycle ────────────────────────────────────── */
  ngOnInit(): void {
    this.layout.setPage('Clients');
    this.loadKpiData();

    this.searchParams$
      .pipe(
        debounceTime(300),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        switchMap(({ criteria, page }) => {
          this.loading.set(true);
          return this.clientSvc.search(criteria, page, this.pageSize);
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
    this.clientSvc.getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: clients => this.allClients.set(clients),
        error: () => {},
      });
  }

  /* ── Rafraîchit la page courante après une mutation (create/edit/delete/…) ── */
  private reloadCurrentPage(): void {
    const { criteria, page } = this.searchParams();
    this.loading.set(true);
    this.clientSvc.search(criteria, page, this.pageSize)
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

  /* ── Filters ──────────────────────────────────────── */
  setFilter(field: 'name' | 'email' | 'phone' | 'city' | 'state' | 'country', val: string): void {
    this.page.set(0);
    if (field === 'name')    this.fName.set(val);
    if (field === 'email')   this.fEmail.set(val);
    if (field === 'phone')   this.fPhone.set(val);
    if (field === 'city')    this.fCity.set(val);
    if (field === 'state')   this.fState.set(val);
    if (field === 'country') this.fCountry.set(val);
  }

  setStatus(s: 'all' | 'active' | 'inactive'): void { this.fStatus.set(s); this.page.set(0); }

  clearFilters(): void {
    this.fName.set(''); this.fEmail.set(''); this.fPhone.set('');
    this.fCity.set(''); this.fState.set(''); this.fCountry.set('');
    this.fStatus.set('all'); this.fDateFrom.set(''); this.fDateTo.set('');
    this.page.set(0);
  }

  /* ── Create / Edit ────────────────────────────────── */
  openCreate(): void {
    this.form.reset({ countryCode: 'US' });
    this.editingId.set(null);
    this.formMode.set('create');
  }

  openEdit(c: ClientResponse): void {
    this.form.reset({
      fullName:    c.fullName,
      email:       c.email      ?? '',
      phone:       c.phone      ?? '',
      address:     c.address    ?? '',
      city:        c.city       ?? '',
      state:       c.state      ?? '',
      zipCode:     c.zipCode    ?? '',
      countryCode: c.country?.code ?? 'US',
    });
    this.editingId.set(c.id);
    this.formMode.set('edit');
  }

  closeForm(): void { this.formMode.set(null); }

  saveForm(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const v = this.form.getRawValue();
    const req = {
      fullName:    v.fullName!,
      email:       v.email!,
      phone:       v.phone      || undefined,
      address:     v.address    || undefined,
      city:        v.city       || undefined,
      state:       v.state      || undefined,
      zipCode:     v.zipCode    || undefined,
      countryCode: v.countryCode!,
    };
    const obs = this.editingId()
      ? this.clientSvc.update(this.editingId()!, req)
      : this.clientSvc.create(req);

    obs.pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: (saved) => {
        this.saving.set(false);
        this.formMode.set(null);
        const isEdit = !!this.editingId();
        this.toast.success(isEdit ? 'Client modifié avec succès.' : 'Client créé avec succès.');
        if (!isEdit) this.page.set(0);
        this.reloadCurrentPage();
        this.lastSavedId.set(saved.id);
        setTimeout(() => this.lastSavedId.set(null), 1800);
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

  /* ── Detail ───────────────────────────────────────── */
  openDetail(c: ClientResponse): void {
    this.detailClient.set(c);
    this.detailOrders.set([]);
    this.detailLoading.set(true);
    this.orderSvc.getByClient(c.id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: orders => { this.detailOrders.set(orders); this.detailLoading.set(false); },
        error: ()     => this.detailLoading.set(false),
      });
  }

  closeDetail(): void { this.detailClient.set(null); }

  /* ── Toggle actif ─────────────────────────────────── */
  toggleActive(c: ClientResponse): void {
    this.toggling.set(c.id);
    this.clientSvc.patch(c.id, { active: !c.active })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.toggling.set(null);
          this.reloadCurrentPage();
          this.toast.success(updated.active ? 'Client activé.' : 'Client désactivé.');
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
    this.clientSvc.delete(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deleting.set(false);
          this.deleteId.set(null);
          this.reloadCurrentPage();
          this.toast.success('Client supprimé.');
        },
        error: (err: any) => { this.deleting.set(false); this.toast.error(err?.error?.message || 'Erreur lors de la suppression.'); },
      });
  }

  /* ── Sélection bulk ──────────────────────────────── */
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
        this.pageItems().forEach(c => next.delete(c.id));
        return next;
      });
    } else {
      this.selectedIds.update(s => {
        const next = new Set(s);
        this.pageItems().forEach(c => next.add(c.id));
        return next;
      });
    }
  }

  clearSelection(): void { this.selectedIds.set(new Set()); }

  openBulkDeleteConfirm(): void { this.bulkDeleteConfirm.set(true); }
  closeBulkDeleteConfirm(): void { this.bulkDeleteConfirm.set(false); }

  executeBulkDelete(): void {
    const ids = [...this.selectedIds()];
    this.bulkActing.set(true);
    this.clientSvc.bulkDelete(ids)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.bulkActing.set(false);
          this.bulkDeleteConfirm.set(false);
          this.selectedIds.set(new Set());
          this.reloadCurrentPage();
          this.toast.success(`${res.deleted} client(s) supprimé(s).`);
        },
        error: (err: any) => { this.bulkActing.set(false); this.toast.error(err?.error?.message || 'Erreur lors de la suppression en masse.'); },
      });
  }

  executeBulkActivate(): void {
    const ids = [...this.selectedIds()];
    this.bulkActing.set(true);
    this.clientSvc.bulkActivate(ids)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.bulkActing.set(false);
          this.selectedIds.set(new Set());
          this.reloadCurrentPage();
          this.toast.success(`${res.activated} client(s) activé(s).`);
        },
        error: () => { this.bulkActing.set(false); this.toast.error('Erreur activation en masse.'); },
      });
  }

  executeBulkDeactivate(): void {
    const ids = [...this.selectedIds()];
    this.bulkActing.set(true);
    this.clientSvc.bulkDeactivate(ids)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.bulkActing.set(false);
          this.selectedIds.set(new Set());
          this.reloadCurrentPage();
          this.toast.success(`${res.deactivated} client(s) désactivé(s).`);
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

  countryLabel(c: ClientResponse): string {
    return c.country?.code ?? '—';
  }

  orderStatusLabel(s: string): string {
    const m: Record<string, string> = {
      CREATED: 'Créé', EMAIL_SENT: 'Email envoyé',
      PAID: 'Payé', DELIVERED: 'Livré', CANCELLED: 'Annulé',
    };
    return m[s] ?? s;
  }

  orderStatusClass(s: string): string {
    const m: Record<string, string> = {
      CREATED: 'cl-os-created', EMAIL_SENT: 'cl-os-email',
      PAID: 'cl-os-paid', DELIVERED: 'cl-os-delivered', CANCELLED: 'cl-os-cancelled',
    };
    return m[s] ?? '';
  }
}
