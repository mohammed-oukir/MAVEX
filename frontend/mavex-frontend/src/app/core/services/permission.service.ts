import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { tap } from 'rxjs';
import { PermissionAction, PermissionModule, PermissionsMap } from '../models/auth.model';
import { ApiResponse } from '../models/api.model';

@Injectable({ providedIn: 'root' })
export class PermissionService {
  private readonly http = inject(HttpClient);

  private readonly _permissions = signal<PermissionsMap | null>(null);

  /** Map module → actions accordées (null = pas encore chargé) */
  readonly permissions = this._permissions.asReadonly();

  /** true si l'utilisateur est ADMIN (toutes permissions implicites) */
  private _isAdmin = signal(false);
  readonly isAdmin = this._isAdmin.asReadonly();

  // ── Initialisation ─────────────────────────────────────────────

  setPermissions(map: PermissionsMap, isAdmin: boolean): void {
    this._isAdmin.set(isAdmin);
    this._permissions.set(map);
  }

  clearPermissions(): void {
    this._permissions.set(null);
    this._isAdmin.set(false);
  }

  /** Appelle /me/permissions et met à jour le signal. */
  loadFromApi() {
    return this.http
      .get<ApiResponse<PermissionsMap>>('/api/auth/me/permissions')
      .pipe(tap(res => this._permissions.set(res.data)));
  }

  // ── Vérifications ──────────────────────────────────────────────

  hasPermission(module: PermissionModule, action: PermissionAction): boolean {
    if (this._isAdmin()) return true;
    const map = this._permissions();
    if (!map) return false;
    return (map[module] ?? []).includes(action);
  }

  /** Au moins une action sur ce module (pour masquer/afficher le menu) */
  hasAnyPermission(module: PermissionModule): boolean {
    if (this._isAdmin()) return true;
    const map = this._permissions();
    if (!map) return false;
    return (map[module] ?? []).length > 0;
  }

  canView(module: PermissionModule):   boolean { return this.hasPermission(module, 'VIEW'); }
  canCreate(module: PermissionModule): boolean { return this.hasPermission(module, 'CREATE'); }
  canEdit(module: PermissionModule):   boolean { return this.hasPermission(module, 'EDIT'); }
  canDelete(module: PermissionModule): boolean { return this.hasPermission(module, 'DELETE'); }

  // ── Signals computed réutilisables par composant ───────────────

  readonly menuModules = computed(() => {
    const map = this._permissions();
    if (this._isAdmin() || !map) return null;
    return map;
  });
}
