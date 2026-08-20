import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { forkJoin } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService }   from '../../core/services/layout.service';
import { ShipmentService, ShipmentSearchParams } from '../../core/services/shipment.service';
import { ShipperService }  from '../../core/services/shipper.service';
import { OrderService }    from '../../core/services/order.service';
import { ToastService }    from '../../core/services/toast.service';
import { AirlineService }  from '../../core/services/airline.service';
import { AuthService }     from '../../core/auth/auth.service';
import { MyPermissionsService } from '../../core/services/my-permissions.service';
import { BadgeComponent }  from '../../shared/badge/badge.component';
import { ShipmentResponse, ShipmentRequest, ShipmentPatch, ShipmentStatus } from '../../core/models/shipment.model';
import { ShipperResponse } from '../../core/models/shipper.model';
import { OrderResponse }   from '../../core/models/order.model';
import { Airline }         from '../../core/models/airline.model';

@Component({
  selector: 'app-shipments',
  imports: [RouterLink, ReactiveFormsModule, BadgeComponent],
  templateUrl: './shipments.component.html',
  styleUrl:    './shipments.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: {
    '(document:click)': 'onDocumentClick($event)',
  },
})
export class ShipmentsComponent implements OnInit {
  private readonly router      = inject(Router);
  private readonly layout      = inject(LayoutService);
  private readonly shipSvc     = inject(ShipmentService);
  private readonly shipperSvc  = inject(ShipperService);
  private readonly orderSvc    = inject(OrderService);
  private readonly toast       = inject(ToastService);
  private readonly airlineSvc  = inject(AirlineService);
  private readonly fb          = inject(FormBuilder);
  private readonly destroyRef  = inject(DestroyRef);
  protected readonly auth          = inject(AuthService);
  protected readonly myPermissions = inject(MyPermissionsService);

  /* ── Airline detection ────────────────────────────────── */
  detectedAirline = signal<Airline | null>(null);
  detectingAirline = signal(false);

  /* ── Data ─────────────────────────────────────────────── */
  shipments    = signal<ShipmentResponse[]>([]);
  total        = signal(0);
  shippers     = signal<ShipperResponse[]>([]);
  loading      = signal(true);

  /* ── Sélection bulk ───────────────────────────────────── */
  selectedIds  = signal<Set<number>>(new Set());
  someSelected = computed(() => this.selectedIds().size > 0);
  allSelected  = computed(() => this.shipments().length > 0 && this.selectedIds().size === this.shipments().length);
  selCount     = computed(() => this.selectedIds().size);

  /* ── Export ───────────────────────────────────────────── */
  exportOpen = signal(false);

  /* ── Panneau de filtres ───────────────────────────────── */
  filtersCollapsed = signal(false);
  flMawb        = signal('');
  flShipper     = signal('');
  flImportFrom  = signal('');
  flImportTo    = signal('');
  flCarrier     = signal('');
  flMode        = signal('');
  flTotalOrders = signal('');
  flDutyMin     = signal('');
  flDutyMax     = signal('');
  flStatus      = signal('');

  /* ── Combobox Shipper du panneau de filtres ──────────── */
  shipperDropdownOpen = signal(false);

  /* ── Filtres avancés (repliés par défaut) ────────────── */
  advancedFiltersOpen = signal(false);

  /** Tous les shippers, triés alphabétiquement */
  sortedShippers = computed(() =>
    [...this.shippers()].sort((a, b) =>
      (a.companyName ?? '').localeCompare(b.companyName ?? '', 'fr', { sensitivity: 'base' })
    )
  );

  /* ── Pagination ───────────────────────────────────────── */
  readonly PAGE_SIZE = 15;
  currentPage = signal(0);

  /* ── UI state ─────────────────────────────────────────── */
  expandedOrders   = signal<Map<number, OrderResponse[]>>(new Map());
  loadingOrders    = signal<Set<number>>(new Set());
  editingShipment  = signal<ShipmentResponse | null>(null);
  saving           = signal(false);
  deleteId         = signal<number | null>(null);
  deleting         = signal(false);
  creatingShipment = signal(false);
  creating         = signal(false);
  previewShipment  = signal<ShipmentResponse | null>(null);

  /* ── Shipper autocomplete (create) ───────────────────── */
  shipperSearch       = signal('');
  showShipperDropdown = signal(false);
  selectedShipperName = signal('');

  filteredShippers = computed(() => {
    const q = this.shipperSearch().toLowerCase().trim();
    if (!q) return this.shippers().slice(0, 50);
    return this.shippers().filter(s =>
      s.companyName?.toLowerCase().includes(q) ||
      s.email?.toLowerCase().includes(q) ||
      s.city?.toLowerCase().includes(q)
    ).slice(0, 50);
  });

  /* ── Shipper autocomplete (edit) ─────────────────────── */
  editShipperSearch       = signal('');
  showEditShipperDropdown = signal(false);
  selectedEditShipperName = signal('');

  filteredEditShippers = computed(() => {
    const q = this.editShipperSearch().toLowerCase().trim();
    if (!q) return this.shippers().slice(0, 50);
    return this.shippers().filter(s =>
      s.companyName?.toLowerCase().includes(q) ||
      s.email?.toLowerCase().includes(q) ||
      s.city?.toLowerCase().includes(q)
    ).slice(0, 50);
  });

  /* ── Computed (pagination serveur) ────────────────────── */
  totalPages = computed(() => Math.ceil(this.total() / this.PAGE_SIZE));
  pages      = computed(() => Array.from({ length: this.totalPages() }, (_, i) => i));

  hasFilters = computed(() =>
    !!this.flMawb()    || !!this.flShipper() || !!this.flImportFrom() || !!this.flImportTo() ||
    !!this.flCarrier() || !!this.flMode()    || !!this.flTotalOrders() ||
    !!this.flDutyMin() || !!this.flDutyMax() || !!this.flStatus()
  );

  /* ── KPIs (compteurs des stat-cards) ─────────────────── */
  kpiAll        = signal(0);
  kpiDraft      = signal(0);
  kpiImported   = signal(0);
  kpiProcessing = signal(0);
  kpiClosed     = signal(0);

  /* ── Forms ────────────────────────────────────────────── */
  createForm = this.fb.group({
    mawb:             ['', [Validators.required, Validators.minLength(3)]],
    shipperId:        [null as number | null],
    exportDate:       [''],
    importDate:       [''],
    importingCarrier: [''],
    modeOfTransport:  [''],
    portCode:         [''],
  });

  editForm = this.fb.group({
    mawb:             ['', [Validators.required, Validators.minLength(3)]],
    shipperId:        [null as number | null],
    exportDate:       [''],
    importDate:       [''],
    importingCarrier: [''],
    modeOfTransport:  [''],
    portCode:         [''],
    status:           ['DRAFT' as ShipmentStatus, Validators.required],
  });

  /* ── Lifecycle ────────────────────────────────────────── */
  ngOnInit(): void {
    this.layout.setPage('Shipments', this.canCreate() ? {
      label:   'Nouveau Shipment',
      onClick: () => this.openCreate(),
    } : undefined);
    this.loadShipments();
    this.loadKpis();
    this.loadShippers();
  }

  /* ── Load ─────────────────────────────────────────────── */
  private loadShipments(): void {
    this.loading.set(true);
    this.shipSvc.search(this.buildParams(), this.currentPage(), this.PAGE_SIZE)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next:  p  => {
          this.shipments.set(p.content);
          this.total.set(p.totalElements);
          this.loading.set(false);
        },
        error: () => this.loading.set(false),
      });
  }

  /** Lit les 10 signals de filtre. Duty : le backend stocke une fraction → on divise par 100. */
  private buildParams(): ShipmentSearchParams {
    const orders = this.flTotalOrders().trim();
    const dMin   = this.flDutyMin().trim();
    const dMax   = this.flDutyMax().trim();
    return {
      mawb:        this.flMawb().trim()    || undefined,
      shipper:     this.flShipper().trim() || undefined,
      importFrom:  this.flImportFrom()     || undefined,
      importTo:    this.flImportTo()       || undefined,
      carrier:     this.flCarrier().trim() || undefined,
      mode:        this.flMode().trim()    || undefined,
      totalOrders: orders !== '' ? Number(orders)     : undefined,
      dutyRateMin: dMin   !== '' ? Number(dMin) / 100 : undefined,
      dutyRateMax: dMax   !== '' ? Number(dMax) / 100 : undefined,
      status:      (this.flStatus() as ShipmentStatus) || undefined,
    };
  }

  private loadKpis(): void {
    forkJoin({
      all:        this.shipSvc.search({},                       0, 1),
      draft:      this.shipSvc.search({ status: 'DRAFT' },      0, 1),
      imported:   this.shipSvc.search({ status: 'IMPORTED' },   0, 1),
      processing: this.shipSvc.search({ status: 'PROCESSING' }, 0, 1),
      closed:     this.shipSvc.search({ status: 'CLOSED' },     0, 1),
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: ({ all, draft, imported, processing, closed }) => {
        this.kpiAll.set(all.totalElements);
        this.kpiDraft.set(draft.totalElements);
        this.kpiImported.set(imported.totalElements);
        this.kpiProcessing.set(processing.totalElements);
        this.kpiClosed.set(closed.totalElements);
      },
    });
  }

  private loadShippers(): void {
    this.shipperSvc.getAllActive().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: list => this.shippers.set(list),
      error: () => {},
    });
  }

  /* ── Filtres ──────────────────────────────────────────── */
  onSearchClick(): void {
    this.currentPage.set(0);
    this.loadShipments();
  }

  onResetClick(): void {
    this.flMawb.set('');
    this.flShipper.set('');
    this.flImportFrom.set('');
    this.flImportTo.set('');
    this.flCarrier.set('');
    this.flMode.set('');
    this.flTotalOrders.set('');
    this.flDutyMin.set('');
    this.flDutyMax.set('');
    this.flStatus.set('');
    this.currentPage.set(0);
    this.loadShipments();
  }

  /** Stat-cards : flStatus est la seule source de vérité du filtre statut. */
  setStatus(value: string): void {
    this.flStatus.set(value);
    this.onSearchClick();
  }

  selectFilterShipper(companyName: string): void {
    this.flShipper.set(companyName);
    this.shipperDropdownOpen.set(false);
  }

  /** Ferme le dropdown du filtre Shipper au clic extérieur. */
  onDocumentClick(event: MouseEvent): void {
    if (!this.shipperDropdownOpen()) return;
    const target = event.target as HTMLElement;
    if (!target.closest('.sp-field-combo')) {
      this.shipperDropdownOpen.set(false);
    }
  }

  /* ── Pagination serveur ───────────────────────────────── */
  goToPage(p: number): void {
    this.currentPage.set(p);
    this.loadShipments();
  }

  /* ── Navigation ───────────────────────────────────────── */
  goToDetail(id: number): void {
    this.router.navigate(['/shipments', id]);
  }

  /* ── Expand orders ────────────────────────────────────── */
  toggleExpand(id: number): void {
    const map = this.expandedOrders();
    if (map.has(id)) {
      const n = new Map(map);
      n.delete(id);
      this.expandedOrders.set(n);
    } else {
      this.loadingOrders.update(s => new Set([...s, id]));
      this.orderSvc.getByShipment(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: list => {
          this.expandedOrders.update(m => new Map([...m, [id, list]]));
          this.loadingOrders.update(s => { const n = new Set(s); n.delete(id); return n; });
        },
        error: () => this.loadingOrders.update(s => { const n = new Set(s); n.delete(id); return n; }),
      });
    }
  }

  isExpanded(id: number):        boolean         { return this.expandedOrders().has(id); }
  isLoadingExpand(id: number):   boolean         { return this.loadingOrders().has(id); }
  getExpandedOrders(id: number): OrderResponse[] { return this.expandedOrders().get(id) ?? []; }

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
      : this.selectedIds.set(new Set(this.shipments().map(s => s.id)));
  }

  isSelected(id: number): boolean { return this.selectedIds().has(id); }
  clearSelection(): void          { this.selectedIds.set(new Set()); }

  /* ── Export ───────────────────────────────────────────── */
  exportPdf(): void {
    this.exportOpen.set(false);
    this.shipSvc.exportPdf(this.buildParams())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => this.triggerDownload(blob, 'shipments.pdf'),
        error: () => this.toast.error('Erreur lors de l\'export PDF.'),
      });
  }

  exportExcel(): void {
    this.exportOpen.set(false);
    this.shipSvc.exportExcel(this.buildParams())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => this.triggerDownload(blob, 'shipments.xlsx'),
        error: () => this.toast.error('Erreur lors de l\'export Excel.'),
      });
  }

  exportSelection(): void {
    if (this.selectedIds().size === 0) {
      this.toast.error('Veuillez sélectionner au moins un envoi.');
      return;
    }
    this.exportOpen.set(false);
    this.shipSvc.exportExcelSelection(Array.from(this.selectedIds()))
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: blob => this.triggerDownload(blob, 'shipments_selection.xlsx'),
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

  /* ── Mini formulaire nouveau shipper ─────────────────── */
  showNewShipperForm  = signal(false);
  savingNewShipper    = signal(false);
  newShipperForm = this.fb.group({
    companyName: ['', [Validators.required, Validators.minLength(2)]],
    contactName: [''],
    email:       ['', [Validators.required, Validators.email]],
    phone:       [''],
    city:        [''],
    address:     [''],
    countryCode: ['MA', Validators.pattern(/^[A-Z]{0,2}$/)],
  });

  openNewShipperForm(): void {
    this.showShipperDropdown.set(false);
    this.newShipperForm.reset({ countryCode: 'MA' });
    if (this.shipperSearch()) {
      this.newShipperForm.patchValue({ companyName: this.shipperSearch() });
    }
    this.showNewShipperForm.set(true);
  }

  closeNewShipperForm(): void { this.showNewShipperForm.set(false); }

  newShipperFieldInvalid(f: string): boolean {
    const c = this.newShipperForm.get(f);
    return !!(c?.invalid && c?.touched);
  }

  createAndSelectShipper(): void {
    if (this.newShipperForm.invalid) { this.newShipperForm.markAllAsTouched(); return; }
    this.savingNewShipper.set(true);
    const v = this.newShipperForm.getRawValue();
    this.shipperSvc.create({
      companyName: v.companyName!,
      email:       v.email!,
      contactName: v.contactName || undefined,
      phone:       v.phone       || undefined,
      city:        v.city        || undefined,
      address:     v.address     || undefined,
      countryCode: v.countryCode || undefined,
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.savingNewShipper.set(false);
        this.showNewShipperForm.set(false);
        const created = res.data;
        this.shippers.update(list => [...list, created]);
        this.createForm.patchValue({ shipperId: created.id });
        this.selectedShipperName.set(created.companyName);
        this.shipperSearch.set('');
        this.toast.success(`Shipper "${created.companyName}" créé et sélectionné.`);
      },
      error: err => {
        this.savingNewShipper.set(false);
        this.toast.error(err?.error?.message || 'Erreur lors de la création du shipper.');
      },
    });
  }

  /* ── Shipper autocomplete helpers ────────────────────── */
  selectShipper(s: ShipperResponse): void {
    this.createForm.patchValue({ shipperId: s.id });
    this.selectedShipperName.set(s.companyName);
    this.shipperSearch.set('');
    this.showShipperDropdown.set(false);
  }

  clearShipper(): void {
    this.createForm.patchValue({ shipperId: null });
    this.selectedShipperName.set('');
    this.shipperSearch.set('');
  }

  onShipperSearchInput(val: string): void {
    this.shipperSearch.set(val);
    this.showShipperDropdown.set(true);
    if (!val) this.clearShipper();
  }

  selectEditShipper(s: ShipperResponse): void {
    this.editForm.patchValue({ shipperId: s.id });
    this.selectedEditShipperName.set(s.companyName);
    this.editShipperSearch.set('');
    this.showEditShipperDropdown.set(false);
  }

  clearEditShipper(): void {
    this.editForm.patchValue({ shipperId: null });
    this.selectedEditShipperName.set('');
    this.editShipperSearch.set('');
  }

  onEditShipperSearchInput(val: string): void {
    this.editShipperSearch.set(val);
    this.showEditShipperDropdown.set(true);
    if (!val) this.clearEditShipper();
  }

  /* ── Create ───────────────────────────────────────────── */
  openCreate(): void {
    this.createForm.reset({
      mawb: '', shipperId: null,
      exportDate: '', importDate: '',
      importingCarrier: '', modeOfTransport: '', portCode: '',
    });
    this.detectedAirline.set(null);
    this.selectedShipperName.set('');
    this.shipperSearch.set('');
    this.showShipperDropdown.set(false);
    this.creatingShipment.set(true);
  }
  closeCreate(): void {
    this.creatingShipment.set(false);
    this.detectedAirline.set(null);
    this.selectedShipperName.set('');
  }

  onMawbInput(mawb: string): void {
    this.createForm.patchValue({ mawb });
    const prefix = mawb.replace(/[^0-9]/g, '').substring(0, 3);
    if (prefix.length < 3) { this.detectedAirline.set(null); return; }

    this.detectingAirline.set(true);
    this.airlineSvc.getByMawb(prefix)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: airline => {
          this.detectedAirline.set(airline);
          this.detectingAirline.set(false);
          // Pré-remplir Carrier et Mode SEULEMENT si les champs sont vides
          if (!this.createForm.value.importingCarrier) {
            this.createForm.patchValue({ importingCarrier: airline.name });
          }
          if (!this.createForm.value.modeOfTransport) {
            this.createForm.patchValue({ modeOfTransport: airline.mode ?? 'AIR' });
          }
        },
        error: () => {
          this.detectedAirline.set(null);
          this.detectingAirline.set(false);
        },
      });
  }

  create(): void {
    if (this.createForm.invalid) { this.createForm.markAllAsTouched(); return; }
    this.creating.set(true);
    const v = this.createForm.value;
    const req: ShipmentRequest = {
      mawb:             v.mawb!,
      shipperId:        v.shipperId ?? undefined,
      exportDate:       v.exportDate || undefined,
      importDate:       v.importDate || undefined,
      importingCarrier: v.importingCarrier || undefined,
      modeOfTransport:  v.modeOfTransport || undefined,
      portCode:         v.portCode || undefined,
    };
    this.shipSvc.create(req).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: created => {
        this.creating.set(false);
        this.creatingShipment.set(false);
        this.toast.success('Shipment créé ! Ajoutez maintenant vos orders.');
        this.router.navigate(['/shipments', created.id]);
      },
      error: err => {
        this.creating.set(false);
        this.toast.error(err?.error?.message || 'Erreur lors de la création.');
      },
    });
  }

  createFieldInvalid(name: string): boolean {
    const c = this.createForm.get(name);
    return !!(c?.invalid && c?.touched);
  }

  /* ── Edit ─────────────────────────────────────────────── */
  openEdit(s: ShipmentResponse): void {
    this.editingShipment.set(s);
    this.editForm.patchValue({
      mawb:             s.mawb,
      shipperId:        s.shipper?.id ?? null,
      exportDate:       s.exportDate ?? '',
      importDate:       s.importDate ?? '',
      importingCarrier: s.importingCarrier ?? '',
      modeOfTransport:  s.modeOfTransport ?? '',
      portCode:         s.portCode ?? '',
      status:           s.status,
    });
    this.selectedEditShipperName.set(s.shipper?.companyName ?? '');
    this.editShipperSearch.set('');
    this.showEditShipperDropdown.set(false);
  }
  closeEdit(): void {
    this.editingShipment.set(null);
    this.selectedEditShipperName.set('');
  }

  saveEdit(): void {
    const s = this.editingShipment();
    if (!s || this.editForm.invalid) { this.editForm.markAllAsTouched(); return; }
    this.saving.set(true);
    const v = this.editForm.value;
    const req: ShipmentPatch = {
      mawb:             v.mawb!,
      shipperId:        v.shipperId ?? null,
      exportDate:       v.exportDate || undefined,
      importDate:       v.importDate || undefined,
      importingCarrier: v.importingCarrier || undefined,
      modeOfTransport:  v.modeOfTransport || undefined,
      portCode:         v.portCode || undefined,
      status:           v.status as ShipmentStatus,
    };
    this.shipSvc.patch(s.id, req).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.saving.set(false);
        this.editingShipment.set(null);
        this.toast.success('Shipment mis à jour.');
        this.loadShipments();
        this.loadKpis();
      },
      error: err => {
        this.saving.set(false);
        this.toast.error(err?.error?.message || 'Erreur.');
      },
    });
  }

  editFieldInvalid(name: string): boolean {
    const c = this.editForm.get(name);
    return !!(c?.invalid && c?.touched);
  }

  openPreview(s: ShipmentResponse): void { this.previewShipment.set(s); }
  closePreview(): void { this.previewShipment.set(null); }

  fmtDuty(rate: number | null | undefined): string {
    if (rate == null) return '—';
    return Math.round(rate * 10000) / 100 + ' %';
  }

  fmtDate(d: string | null | undefined): string {
    if (!d) return '—';
    const date = new Date(d);
    if (isNaN(date.getTime())) return d;
    return date.toLocaleDateString('fr-FR', { day: 'numeric', month: 'short', year: 'numeric' });
  }

  /* ── Delete ───────────────────────────────────────────── */
  confirmDelete(id: number): void { this.deleteId.set(id); }
  cancelDelete():             void { this.deleteId.set(null); }

  executeDelete(): void {
    const id = this.deleteId();
    if (!id) return;
    this.deleting.set(true);
    this.shipSvc.delete(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleteId.set(null);
        this.toast.success('Shipment supprimé.');
        this.loadShipments();
        this.loadKpis();
      },
      error: err => {
        this.deleting.set(false);
        this.toast.error(err?.error?.message || 'Erreur.');
      },
    });
  }

  /* ── Permissions ──────────────────────────────────── */
  canCreate(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('SHIPMENTS', 'CREATE');
  }

  canUpdate(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('SHIPMENTS', 'UPDATE');
  }

  canDelete(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('SHIPMENTS', 'DELETE');
  }

  canExport(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('SHIPMENTS', 'EXPORT');
  }
}
