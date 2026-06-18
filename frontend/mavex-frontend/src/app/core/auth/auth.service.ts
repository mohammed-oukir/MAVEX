import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { ApiResponse } from '../models/api.model';
import { LoginResponse, ForgotPasswordRequest, ResetPasswordRequest } from '../models/auth.model';
import { PermissionService } from '../services/permission.service';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly http        = inject(HttpClient);
  private readonly router      = inject(Router);
  private readonly permissions = inject(PermissionService);

  private readonly _token      = signal<string | null>(localStorage.getItem('accessToken'));
  private readonly _refresh    = signal<string | null>(localStorage.getItem('refreshToken'));
  private readonly _email      = signal<string | null>(localStorage.getItem('userEmail'));
  private readonly _fullName   = signal<string | null>(localStorage.getItem('userFullName'));
  private readonly _role       = signal<string | null>(localStorage.getItem('userRole'));
  private readonly _userId     = signal<number | null>(
    localStorage.getItem('userId') ? Number(localStorage.getItem('userId')) : null
  );

  readonly isAuthenticated = computed(() => !!this._token());

  readonly currentUser = computed(() => {
    const name = this._fullName() || this._email() || '?';
    return {
      email:    this._email(),
      fullName: this._fullName(),
      role:     this._role(),
      avatar:   name.charAt(0).toUpperCase(),
    };
  });

  getToken(): string | null        { return this._token(); }
  getRefreshToken(): string | null { return this._refresh(); }
  isAdmin(): boolean               { return this._role() === 'ADMIN'; }
  currentEmail(): string | null    { return this._email(); }
  currentUserId(): number | null   { return this._userId(); }
  isRefreshing = false;

  login(email: string, password: string): Observable<ApiResponse<LoginResponse>> {
    return this.http.post<ApiResponse<LoginResponse>>('/api/auth/login', { email, password }).pipe(
      tap(res => this.storeSession(res.data)),
    );
  }

  forgotPassword(email: string): Observable<unknown> {
    return this.http.post<unknown>('/api/auth/forgot-password', { email } satisfies ForgotPasswordRequest);
  }

  resetPassword(email: string, otp: string, newPassword: string): Observable<unknown> {
    return this.http.post<unknown>('/api/auth/reset-password', { email, otp, newPassword } satisfies ResetPasswordRequest);
  }

  updateTokens(accessToken: string, refreshToken: string): void {
    this._token.set(accessToken);
    this._refresh.set(refreshToken);
    localStorage.setItem('accessToken',  accessToken);
    localStorage.setItem('refreshToken', refreshToken);
  }

  logout(): void {
    const rt = this._refresh();
    if (rt) {
      this.http.post('/api/auth/logout', { refreshToken: rt }).subscribe({ error: () => {} });
    }
    this.clearSession();
  }

  private storeSession(data: LoginResponse): void {
    this._token.set(data.accessToken);
    this._refresh.set(data.refreshToken);
    this._email.set(data.email);
    this._fullName.set(data.fullName);
    this._role.set(data.role);
    this._userId.set(data.userId);
    localStorage.setItem('accessToken',  data.accessToken);
    localStorage.setItem('refreshToken', data.refreshToken);
    localStorage.setItem('userEmail',    data.email);
    localStorage.setItem('userFullName', data.fullName);
    localStorage.setItem('userRole',     data.role);
    localStorage.setItem('userId',       String(data.userId));
    // Hydrate le signal de permissions dès le login (frontend display uniquement)
    this.permissions.setPermissions(data.permissions, data.role === 'ADMIN');
  }

  private clearSession(): void {
    this._token.set(null);
    this._refresh.set(null);
    this._email.set(null);
    this._fullName.set(null);
    this._role.set(null);
    this._userId.set(null);
    this.permissions.clearPermissions();
    localStorage.clear();
    this.router.navigate(['/login']);
  }
}
