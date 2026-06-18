import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService }  from '../../core/services/layout.service';
import { ClientService }  from '../../core/services/client.service';
import { OrderService }   from '../../core/services/order.service';
import { ToastService }   from '../../core/services/toast.service';
import { ClientResponse } from '../../core/models/client.model';
import { OrderResponse }  from '../../core/models/order.model';
import { RequiresPermissionDirective } from '../../core/directives/requires-permission.directive';

@Component({
  selector: 'app-clients',
  imports: [ReactiveFormsModule, DatePipe, RequiresPermissionDirective],
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

  /* ── Data ─────────────────────────────────────────── */
  allClients = signal<ClientResponse[]>([]);
  loading    = signal(true);
  page       = signal(0);
  readonly pageSize = 15;

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

  /* ── Filtered + paginated ─────────────────────────── */
  filtered = computed(() => {
    const n    = this.fName().toLowerCase();
    const e    = this.fEmail().toLowerCase();
    const p    = this.fPhone().toLowerCase();
    const ci   = this.fCity().toLowerCase();
    const st   = this.fState().toLowerCase();
    const co   = this.fCountry().toLowerCase();
    const s    = this.fStatus();
    const from = this.fDateFrom() ? new Date(this.fDateFrom()) : null;
    const to   = this.fDateTo()   ? new Date(this.fDateTo())   : null;
    if (to) to.setHours(23, 59, 59, 999);

    return this.allClients().filter(c => {
      if (n  && !c.fullName?.toLowerCase().includes(n))  return false;
      if (e  && !c.email?.toLowerCase().includes(e))    return false;
      if (p  && !c.phone?.toLowerCase().includes(p))    return false;
      if (ci && !c.city?.toLowerCase().includes(ci))    return false;
      if (st && !c.state?.toLowerCase().includes(st))   return false;
      if (co) {
        const cc = c.country?.code?.toLowerCase() ?? '';
        const cn = c.country?.name?.toLowerCase() ?? '';
        if (!cc.includes(co) && !cn.includes(co)) return false;
      }
      if (s === 'active'   && !c.active)  return false;
      if (s === 'inactive' && c.active)   return false;
      if (from || to) {
        const created = c.createdAt ? new Date(c.createdAt) : null;
        if (!created) return false;
        if (from && created < from) return false;
        if (to   && created > to)   return false;
      }
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
    this.loadClients();
  }

  /* ── Load ─────────────────────────────────────────── */
  loadClients(): void {
    this.loading.set(true);
    this.clientSvc.getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: clients => { this.allClients.set(clients); this.loading.set(false); },
        error: ()      => this.loading.set(false),
      });
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
        if (isEdit) {
          this.allClients.update(list => list.map(c => c.id === saved.id ? saved : c));
        } else {
          this.allClients.update(list => [saved, ...list]);
          this.page.set(0);
        }
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
          this.allClients.update(list => list.map(x => x.id === updated.id ? updated : x));
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
          this.allClients.update(list => list.filter(c => c.id !== id));
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
          this.allClients.update(list => list.filter(c => !ids.includes(c.id)));
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
          this.allClients.update(list => list.map(c => ids.includes(c.id) ? { ...c, active: true } : c));
          this.selectedIds.set(new Set());
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
          this.allClients.update(list => list.map(c => ids.includes(c.id) ? { ...c, active: false } : c));
          this.selectedIds.set(new Set());
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

  getAvatarColor(name: string): string {
    const palette = ['#F97316','#3B82F6','#22C55E','#8B5CF6','#EC4899','#14B8A6','#F59E0B'];
    let h = 0;
    for (let i = 0; i < name.length; i++) h = name.charCodeAt(i) + ((h << 5) - h);
    return palette[Math.abs(h) % palette.length];
  }

  countryLabel(c: ClientResponse): string {
    return c.country?.code ?? '—';
  }

  orderStatusLabel(s: string): string {
    const m: Record<string, string> = {
      CREATED: 'Créé', EMAIL_SENT: 'Email envoyé',
      PENDING_PAYMENT: 'En attente', PAID: 'Payé',
      IN_DELIVERY: 'En livraison', DELIVERED: 'Livré', CANCELLED: 'Annulé',
    };
    return m[s] ?? s;
  }

  orderStatusClass(s: string): string {
    const m: Record<string, string> = {
      CREATED: 'cl-os-created', EMAIL_SENT: 'cl-os-email',
      PENDING_PAYMENT: 'cl-os-pending', PAID: 'cl-os-paid',
      IN_DELIVERY: 'cl-os-delivery', DELIVERED: 'cl-os-delivered', CANCELLED: 'cl-os-cancelled',
    };
    return m[s] ?? '';
  }
}
