import { Injectable, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, forkJoin, map, of, shareReplay, tap } from 'rxjs';
import { AgentPermissionService } from './agent-permission.service';
import { AgentPermissionsResponse, PermissionCatalogItem } from '../models/agent-permission.model';

@Injectable({ providedIn: 'root' })
export class MyPermissionsService {
  private readonly http    = inject(HttpClient);
  private readonly permSvc = inject(AgentPermissionService);

  /** IDs des permissions accordées à l'utilisateur connecté. */
  private readonly _permissionIds = signal<Set<number>>(new Set());
  /** Catalogue complet (module/action par id), pour résoudre hasPermission(module, action). */
  private readonly _catalog = signal<PermissionCatalogItem[]>([]);

  private catalogLoaded = false;
  /** Chargement initial en cours (partagé) — permet aux guards d'attendre sa fin sans le redéclencher. */
  private initialLoad$: Observable<void> | null = null;

  constructor() {
    // Restauration au démarrage de l'app, si un token existe déjà —
    // même logique minimale que AuthService (_token) pour éviter tout cycle DI.
    if (localStorage.getItem('accessToken')) {
      this.initialLoad$ = this.loadMyPermissions().pipe(shareReplay(1));
      this.initialLoad$.subscribe();
    }
  }

  /** Charge (ou recharge) les permissions de l'utilisateur connecté + le catalogue si besoin. */
  loadMyPermissions(): Observable<void> {
    const catalog$ = this.catalogLoaded
      ? new Observable<PermissionCatalogItem[]>(sub => { sub.next(this._catalog()); sub.complete(); })
      : this.permSvc.getCatalog().pipe(tap(items => { this._catalog.set(items); this.catalogLoaded = true; }));

    const me$ = this.http.get<AgentPermissionsResponse>('/api/agent-permissions/me');

    return forkJoin([me$, catalog$]).pipe(
      tap(([me]) => this._permissionIds.set(new Set(me.permissionIds))),
      map(() => void 0),
    );
  }

  /**
   * Complète quand le chargement initial (déclenché au démarrage si un token
   * existait déjà) est terminé. Si aucun chargement n'a été déclenché
   * (pas de token au démarrage), complète immédiatement — rien à attendre.
   */
  whenReady(): Observable<void> {
    return this.initialLoad$ ?? of(void 0);
  }

  /** Vérifie si l'utilisateur connecté a la permission (module, action) donnée. */
  hasPermission(module: string, action: string): boolean {
    const ids = this._permissionIds();
    if (ids.size === 0) return false;

    const permission = this._catalog().find(p => p.module === module && p.action === action);
    return permission ? ids.has(permission.id) : false;
  }

  /** Vérifie si l'utilisateur connecté a AU MOINS UNE permission dans ce module, quelle qu'en soit l'action. */
  hasAnyPermissionInModule(module: string): boolean {
    const ids = this._permissionIds();
    if (ids.size === 0) return false;

    return this._catalog().some(p => p.module === module && ids.has(p.id));
  }

  /** Vide l'état — à appeler au logout. */
  clear(): void {
    this._permissionIds.set(new Set());
    // Le catalogue reste en cache volontairement : il ne dépend pas de l'utilisateur
    // connecté (mêmes 50 permissions pour tout le monde), pas besoin de le recharger
    // au prochain login. Seul _permissionIds (propre à la personne) est vidé.
  }
}
