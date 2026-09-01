import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed, toObservable } from '@angular/core/rxjs-interop';
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { LayoutService } from '../../core/services/layout.service';
import { UserService }   from '../../core/services/user.service';
import { ToastService }  from '../../core/services/toast.service';
import { AuthService }   from '../../core/auth/auth.service';
import { UserResponse, UserRole, UserSearchCriteria, UserStats } from '../../core/models/user.model';

/**
 * Validator de groupe : compare password et confirmPassword.
 * Ne s'applique que si password contient une valeur — en mode edit,
 * laisser les deux champs vides (= ne pas changer le mot de passe) est valide.
 */
function passwordsMatchValidator(group: AbstractControl): ValidationErrors | null {
  const password        = group.get('password')?.value;
  const confirmPassword = group.get('confirmPassword')?.value;
  if (!password) return null;
  return password === confirmPassword ? null : { passwordMismatch: true };
}

@Component({
  selector: 'app-users',
  imports: [ReactiveFormsModule, DatePipe],
  templateUrl: './users.component.html',
  styleUrl: './users.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class UsersComponent implements OnInit {
  private readonly layout     = inject(LayoutService);
  private readonly userSvc    = inject(UserService);
  private readonly toast      = inject(ToastService);
  private readonly authSvc    = inject(AuthService);
  private readonly fb         = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  /* ── Data ─────────────────────────────────────────── */
  loading  = signal(true);

  /* ── Data (recherche paginée côté serveur) ─────────── */
  pageItems      = signal<UserResponse[]>([]);
  totalElements  = signal(0);
  totalPages     = signal(0);
  page           = signal(0);
  pageSize       = signal(15);

  /* ── Compte connecté ──────────────────────────────── */
  isSelf = (u: UserResponse) => u.email === this.authSvc.currentEmail();

  /* ── Filtres ──────────────────────────────────────── */
  searchName  = signal('');
  searchEmail = signal('');
  roleFilter   = signal<UserRole | 'ALL'>('ALL');
  statusFilter = signal<'all' | 'active' | 'inactive'>('all');

  /* ── Stats ────────────────────────────────────────── */
  userStats      = signal<UserStats | null>(null);
  statTotal      = computed(() => this.userStats()?.totalUsers ?? 0);
  statAdmins     = computed(() => this.userStats()?.adminCount ?? 0);
  statAgents     = computed(() => this.userStats()?.agentCount ?? 0);
  statComptables = computed(() => this.userStats()?.comptableCount ?? 0);
  statActive     = computed(() => this.userStats()?.activeUsers ?? 0);

  /* ── Critères combinés → déclenchent la recherche serveur ── */
  private readonly searchParams = computed(() => {
    const rf = this.roleFilter();
    return {
      criteria: {
        fullName: this.searchName(),
        email:    this.searchEmail(),
        role:     rf === 'ALL' ? undefined : rf,
        status:   this.statusFilter(),
      } satisfies UserSearchCriteria,
      page: this.page(),
      size: this.pageSize(),
    };
  });

  /* ── "Un filtre est actif" — affiche le bouton "Effacer filtres" ── */
  hasFilters = computed(() =>
    !!this.searchName() || !!this.searchEmail() || this.roleFilter() !== 'ALL' || this.statusFilter() !== 'all'
  );

  /* ── Observable des critères — créé en champ de classe (contexte d'injection
     valide via le constructeur), consommé dans ngOnInit() ── */
  private readonly searchParams$ = toObservable(this.searchParams);

  /* ── Modals ───────────────────────────────────────── */
  formMode    = signal<'create' | 'edit' | null>(null);
  editingId   = signal<number | null>(null);
  deleteId    = signal<number | null>(null);
  saving      = signal(false);
  deleting    = signal(false);
  toggling    = signal<number | null>(null);
  showPwd     = signal(false);

  /* ── Drawer profil ────────────────────────────────── */
  profileUser = signal<UserResponse | null>(null);
  openProfile(u: UserResponse): void  { this.profileUser.set(u); }
  closeProfile(): void                { this.profileUser.set(null); }

  /* ── Form ─────────────────────────────────────────── */
  form = this.fb.group({
    fullName:        ['', [Validators.required, Validators.minLength(2)]],
    email:            ['', [Validators.required, Validators.email]],
    password:         [''],
    confirmPassword:  [''],
    role:             ['AGENT' as UserRole, Validators.required],
  }, { validators: passwordsMatchValidator });

  /* ── Lifecycle ────────────────────────────────────── */
  ngOnInit(): void {
    this.layout.setPage('Utilisateurs');
    this.loadStats();

    this.searchParams$
      .pipe(
        debounceTime(300),
        distinctUntilChanged((a, b) => JSON.stringify(a) === JSON.stringify(b)),
        switchMap(({ criteria, page, size }) => {
          this.loading.set(true);
          return this.userSvc.search(criteria, page, size);
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

  private loadStats(): void {
    this.userSvc.getStats()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: stats => this.userStats.set(stats),
        error: () => {},
      });
  }

  /* ── Rafraîchit la page courante après une mutation (toggle/delete/…) ── */
  private reloadCurrentPage(): void {
    const { criteria, page, size } = this.searchParams();
    this.loading.set(true);
    this.userSvc.search(criteria, page, size)
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
  }

  /* ── Filtres ──────────────────────────────────────── */
  setSearchName(val: string): void  { this.searchName.set(val); }
  setSearchEmail(val: string): void { this.searchEmail.set(val); }
  setRole(r: UserRole | 'ALL'): void { this.roleFilter.set(r); this.page.set(0); }
  setStatus(s: 'all' | 'active' | 'inactive'): void { this.statusFilter.set(s); this.page.set(0); }

  clearFilters(): void {
    this.searchName.set('');
    this.searchEmail.set('');
    this.roleFilter.set('ALL');
    this.statusFilter.set('all');
    this.page.set(0);
  }

  /* ── Create / Edit ────────────────────────────────── */
  openCreate(): void {
    this.form.reset({ role: 'AGENT' });
    this.form.get('password')!.setValidators([Validators.required, Validators.minLength(8)]);
    this.form.get('password')!.updateValueAndValidity();
    this.form.get('confirmPassword')!.setValidators([Validators.required, Validators.minLength(8)]);
    this.form.get('confirmPassword')!.updateValueAndValidity();
    this.editingId.set(null);
    this.showPwd.set(false);
    this.formMode.set('create');
  }

  openEdit(u: UserResponse): void {
    this.form.reset({ fullName: u.fullName, email: u.email, password: '', confirmPassword: '', role: u.role });
    this.form.get('password')!.clearValidators();
    this.form.get('password')!.updateValueAndValidity();
    this.form.get('confirmPassword')!.clearValidators();
    this.form.get('confirmPassword')!.updateValueAndValidity();
    this.editingId.set(u.id);
    this.showPwd.set(false);
    this.formMode.set('edit');
  }

  closeForm(): void { this.formMode.set(null); }

  saveForm(): void {
    if (this.form.invalid) { this.form.markAllAsTouched(); return; }
    this.saving.set(true);
    const v = this.form.getRawValue();

    if (this.formMode() === 'create') {
      this.userSvc.create({ fullName: v.fullName!, email: v.email!, password: v.password!, role: v.role as UserRole })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => { this.saving.set(false); this.formMode.set(null); this.toast.success('Utilisateur créé.'); this.reloadCurrentPage(); this.loadStats(); },
          error: err => { this.saving.set(false); this.toast.error(err?.error?.message || 'Erreur.'); },
        });
    } else {
      const patch: Record<string, unknown> = { fullName: v.fullName!, email: v.email!, role: v.role };
      if (v.password) patch['password'] = v.password;
      this.userSvc.patch(this.editingId()!, patch)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => { this.saving.set(false); this.formMode.set(null); this.toast.success('Utilisateur modifié.'); this.reloadCurrentPage(); this.loadStats(); },
          error: err => { this.saving.set(false); this.toast.error(err?.error?.message || 'Erreur.'); },
        });
    }
  }

  fieldInvalid(f: string): boolean {
    const c = this.form.get(f);
    return !!(c?.invalid && c?.touched);
  }

  passwordMismatch(): boolean {
    return !!(this.form.errors?.['passwordMismatch'] && this.form.get('confirmPassword')?.touched);
  }

  toggleShowPwd(): void { this.showPwd.update(v => !v); }

  /* ── Toggle actif ─────────────────────────────────── */
  toggleActive(u: UserResponse): void {
    if (this.isSelf(u)) return;
    this.toggling.set(u.id);
    const onNext = () => {
      this.toggling.set(null);
      this.reloadCurrentPage();
      this.loadStats();
      this.toast.success(u.active ? 'Utilisateur désactivé.' : 'Utilisateur activé.');
    };
    const onError = () => { this.toggling.set(null); this.toast.error('Erreur.'); };

    if (u.active) {
      this.userSvc.delete(u.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({ next: onNext, error: onError });
    } else {
      this.userSvc.activate(u.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({ next: onNext, error: onError });
    }
  }

  /* ── Delete ───────────────────────────────────────── */
  confirmDelete(id: number): void {
    const u = this.pageItems().find(x => x.id === id);
    if (u && this.isSelf(u)) return;
    this.deleteId.set(id);
  }
  cancelDelete(): void            { this.deleteId.set(null); }

  executeDelete(): void {
    const id = this.deleteId();
    if (!id) return;
    this.deleting.set(true);
    this.userSvc.delete(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: () => {
          this.deleting.set(false); this.deleteId.set(null);
          this.reloadCurrentPage();
          this.loadStats();
          this.toast.success('Utilisateur supprimé.');
        },
        error: () => { this.deleting.set(false); this.toast.error('Erreur.'); },
      });
  }

  /* ── Pagination ───────────────────────────────────── */
  goToPage(p: number): void { this.page.set(p); }
  pages(): number[]         { return Array.from({ length: this.totalPages() }, (_, i) => i); }
  onPageSizeChange(size: number): void { this.pageSize.set(size); this.page.set(0); }

  /* ── Helpers ──────────────────────────────────────── */
  fmtDate(d?: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' });
  }

  fmtRole(role?: string): string {
    if (role === 'ADMIN')     return 'Administrateur';
    if (role === 'COMPTABLE') return 'Comptable';
    return 'Agent';
  }

  getInitials(name: string): string {
    return name.split(' ').slice(0, 2).map(w => w[0] ?? '').join('').toUpperCase();
  }

}

