import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService } from '../../core/services/layout.service';
import { UserService }   from '../../core/services/user.service';
import { ToastService }  from '../../core/services/toast.service';
import { AuthService }   from '../../core/auth/auth.service';
import { UserResponse, UserRole } from '../../core/models/user.model';

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
  allUsers = signal<UserResponse[]>([]);
  loading  = signal(true);

  /* ── Compte connecté ──────────────────────────────── */
  isSelf = (u: UserResponse) => u.email === this.authSvc.currentEmail();

  /* ── Filtres ──────────────────────────────────────── */
  searchQ     = signal('');
  roleFilter  = signal<UserRole | 'ALL' | 'INACTIVE'>('ALL');

  /* ── Stats ────────────────────────────────────────── */
  statTotal    = computed(() => this.allUsers().length);
  statAdmins   = computed(() => this.allUsers().filter(u => u.role === 'ADMIN').length);
  statAgents   = computed(() => this.allUsers().filter(u => u.role === 'AGENT').length);
  statActive   = computed(() => this.allUsers().filter(u => u.active).length);

  /* ── Filtered ─────────────────────────────────────── */
  filtered = computed(() => {
    const q = this.searchQ().toLowerCase();
    const r = this.roleFilter();
    return this.allUsers().filter(u => {
      if (q && !u.fullName.toLowerCase().includes(q) && !u.email.toLowerCase().includes(q)) return false;
      if (r === 'ADMIN')    return u.role === 'ADMIN';
      if (r === 'AGENT')    return u.role === 'AGENT';
      if (r === 'INACTIVE') return !u.active;
      return true;
    });
  });

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
    fullName: ['', [Validators.required, Validators.minLength(2)]],
    email:    ['', [Validators.required, Validators.email]],
    password: [''],
    role:     ['AGENT' as UserRole, Validators.required],
  });

  /* ── Lifecycle ────────────────────────────────────── */
  ngOnInit(): void {
    this.layout.setPage('Utilisateurs');
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading.set(true);
    this.userSvc.getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: users => { this.allUsers.set(users); this.loading.set(false); },
        error: ()    => this.loading.set(false),
      });
  }

  /* ── Filtres ──────────────────────────────────────── */
  setSearch(val: string): void  { this.searchQ.set(val); }
  setRole(r: UserRole | 'ALL' | 'INACTIVE'): void { this.roleFilter.set(r); }

  /* ── Create / Edit ────────────────────────────────── */
  openCreate(): void {
    this.form.reset({ role: 'AGENT' });
    this.form.get('password')!.setValidators([Validators.required, Validators.minLength(8)]);
    this.form.get('password')!.updateValueAndValidity();
    this.editingId.set(null);
    this.showPwd.set(false);
    this.formMode.set('create');
  }

  openEdit(u: UserResponse): void {
    this.form.reset({ fullName: u.fullName, email: u.email, password: '', role: u.role });
    this.form.get('password')!.clearValidators();
    this.form.get('password')!.updateValueAndValidity();
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
          next: () => { this.saving.set(false); this.formMode.set(null); this.toast.success('Utilisateur créé.'); this.loadUsers(); },
          error: err => { this.saving.set(false); this.toast.error(err?.error?.message || 'Erreur.'); },
        });
    } else {
      const patch: Record<string, unknown> = { fullName: v.fullName!, email: v.email!, role: v.role };
      if (v.password) patch['password'] = v.password;
      this.userSvc.patch(this.editingId()!, patch)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => { this.saving.set(false); this.formMode.set(null); this.toast.success('Utilisateur modifié.'); this.loadUsers(); },
          error: err => { this.saving.set(false); this.toast.error(err?.error?.message || 'Erreur.'); },
        });
    }
  }

  fieldInvalid(f: string): boolean {
    const c = this.form.get(f);
    return !!(c?.invalid && c?.touched);
  }

  toggleShowPwd(): void { this.showPwd.update(v => !v); }

  /* ── Toggle actif ─────────────────────────────────── */
  toggleActive(u: UserResponse): void {
    if (this.isSelf(u)) return;
    this.toggling.set(u.id);
    const onNext = () => {
      this.toggling.set(null);
      this.allUsers.update(list => list.map(x => x.id === u.id ? { ...x, active: !u.active } : x));
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
    const u = this.allUsers().find(x => x.id === id);
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
          this.allUsers.update(list => list.filter(u => u.id !== id));
          this.toast.success('Utilisateur supprimé.');
        },
        error: () => { this.deleting.set(false); this.toast.error('Erreur.'); },
      });
  }

  /* ── Helpers ──────────────────────────────────────── */
  fmtDate(d?: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' });
  }

  fmtRole(role?: string): string {
    return role === 'ADMIN' ? 'Administrateur' : 'Agent';
  }

  getInitials(name: string): string {
    return name.split(' ').slice(0, 2).map(w => w[0] ?? '').join('').toUpperCase();
  }

  getAvatarColor(name: string): string {
    const p = ['#7C3AED','#2563EB','#059669','#D97706','#DC2626','#0891B2','#DB2777'];
    let h = 0;
    for (let i = 0; i < name.length; i++) h = name.charCodeAt(i) + ((h << 5) - h);
    return p[Math.abs(h) % p.length];
  }
}
