import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  inject, signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { AuthService }  from '../../core/auth/auth.service';
import { UserService }  from '../../core/services/user.service';
import { LayoutService } from '../../core/services/layout.service';
import { UserResponse } from '../../core/models/user.model';

@Component({
  selector: 'app-profile',
  templateUrl: './profile.component.html',
  styleUrl: './profile.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ProfileComponent implements OnInit {
  private readonly auth       = inject(AuthService);
  private readonly userSvc    = inject(UserService);
  private readonly layout     = inject(LayoutService);
  private readonly destroyRef = inject(DestroyRef);

  profile = signal<UserResponse | null>(null);
  loading = signal(true);
  error   = signal(false);

  ngOnInit(): void {
    this.layout.setPage('Mon profil');

    const id = this.auth.currentUserId();
    if (id == null) { this.loading.set(false); this.error.set(true); return; }

    this.userSvc.getById(id)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: u  => { this.profile.set(u); this.loading.set(false); },
        error: () => { this.error.set(true); this.loading.set(false); },
      });
  }

  get avatar(): string {
    const u = this.profile();
    const name = u?.fullName || u?.email || '?';
    return name.charAt(0).toUpperCase();
  }

  fmtDate(d?: string): string {
    if (!d) return '—';
    return new Date(d).toLocaleDateString('fr-FR', { day: '2-digit', month: 'long', year: 'numeric' });
  }

  fmtRole(role?: string): string {
    return role === 'ADMIN' ? 'Administrateur' : 'Agent';
  }
}
