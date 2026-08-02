import { ChangeDetectionStrategy, Component, computed, inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../../core/auth/auth.service';

@Component({
  selector: 'app-forbidden',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <div class="forbidden-page">
      <div class="forbidden-card">
        <div class="forbidden-icon">
          <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 64 64" fill="none"
               aria-hidden="true" width="80" height="80">
            <circle cx="32" cy="32" r="30" stroke="#F97316" stroke-width="3"/>
            <line x1="14" y1="14" x2="50" y2="50" stroke="#F97316" stroke-width="3"
                  stroke-linecap="round"/>
          </svg>
        </div>
        <h1 class="forbidden-code">403</h1>
        <p class="forbidden-title">Accès refusé</p>
        <p class="forbidden-desc">
          Vous ne disposez pas des permissions nécessaires pour accéder à cette page.
          Contactez votre administrateur si vous pensez qu'il s'agit d'une erreur.
        </p>

        <div class="forbidden-actions">
          @if (canGoBack()) {
            <button class="btn-back" (click)="goBack()" type="button">
              <svg xmlns="http://www.w3.org/2000/svg" width="16" height="16"
                   viewBox="0 0 24 24" fill="none" stroke="currentColor"
                   stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
                   aria-hidden="true">
                <path d="M19 12H5M12 5l-7 7 7 7"/>
              </svg>
              Retour
            </button>
          }

          @if (canAccessDashboard()) {
            <button class="btn-dashboard" (click)="goDashboard()" type="button">
              Aller au tableau de bord
            </button>
          } @else if (noAccessAtAll()) {
            <p class="forbidden-no-access">
              Aucune section n'est accessible avec votre compte. Contactez votre administrateur.
            </p>
          }

          <button class="btn-logout" (click)="logout()" type="button">
            <svg xmlns="http://www.w3.org/2000/svg" width="15" height="15"
                 viewBox="0 0 24 24" fill="none" stroke="currentColor"
                 stroke-width="2" stroke-linecap="round" stroke-linejoin="round"
                 aria-hidden="true">
              <path d="M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4"/>
              <polyline points="16 17 21 12 16 7"/>
              <line x1="21" y1="12" x2="9" y2="12"/>
            </svg>
            Se déconnecter
          </button>
        </div>
      </div>
    </div>
  `,
  styles: [`
    .forbidden-page {
      min-height: 100vh;
      display: flex;
      align-items: center;
      justify-content: center;
      background: linear-gradient(135deg, #0f172a 0%, #1e293b 100%);
      padding: 1.5rem;
      font-family: 'DM Sans', sans-serif;
    }

    .forbidden-card {
      background: rgba(255,255,255,0.04);
      border: 1px solid rgba(255,255,255,0.08);
      border-radius: 1.5rem;
      padding: 3rem 2.5rem;
      text-align: center;
      max-width: 480px;
      width: 100%;
      backdrop-filter: blur(12px);
    }

    .forbidden-icon {
      display: flex;
      justify-content: center;
      margin-bottom: 1.5rem;
      opacity: 0.9;
    }

    .forbidden-code {
      font-size: 5rem;
      font-weight: 800;
      color: #F97316;
      margin: 0 0 0.5rem;
      line-height: 1;
      font-family: 'Inter Tight', sans-serif;
    }

    .forbidden-title {
      font-size: 1.5rem;
      font-weight: 600;
      color: #f1f5f9;
      margin: 0 0 1rem;
    }

    .forbidden-desc {
      color: #94a3b8;
      font-size: 0.9375rem;
      line-height: 1.7;
      margin: 0 0 2rem;
    }

    .forbidden-actions {
      display: flex;
      flex-direction: column;
      align-items: center;
      gap: 0.75rem;
    }

    /* Ligne horizontale pour Retour + Dashboard */
    .forbidden-actions button.btn-back,
    .forbidden-actions button.btn-dashboard {
      display: inline-flex;
    }
    .forbidden-actions:has(.btn-back):has(.btn-dashboard) {
      flex-direction: row;
      flex-wrap: wrap;
      justify-content: center;
    }

    .btn-back {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.625rem 1.25rem;
      border: 1px solid rgba(255,255,255,0.15);
      border-radius: 0.5rem;
      background: transparent;
      color: #cbd5e1;
      font-size: 0.875rem;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;
      font-family: inherit;
    }
    .btn-back:hover {
      background: rgba(255,255,255,0.06);
      color: #f1f5f9;
    }

    .btn-dashboard {
      padding: 0.625rem 1.25rem;
      border: none;
      border-radius: 0.5rem;
      background: #F97316;
      color: white;
      font-size: 0.875rem;
      font-weight: 600;
      cursor: pointer;
      transition: background 0.2s;
      font-family: inherit;
    }
    .btn-dashboard:hover { background: #ea6c0f; }

    .forbidden-no-access {
      font-size: 0.875rem;
      color: #64748b;
      margin: 0;
      padding: 0.5rem 0;
    }

    .btn-logout {
      display: inline-flex;
      align-items: center;
      gap: 0.5rem;
      padding: 0.5rem 1rem;
      border: 1px solid rgba(239,68,68,0.25);
      border-radius: 0.5rem;
      background: transparent;
      color: #f87171;
      font-size: 0.8125rem;
      font-weight: 500;
      cursor: pointer;
      transition: all 0.2s;
      font-family: inherit;
      margin-top: 0.25rem;
    }
    .btn-logout:hover {
      background: rgba(239,68,68,0.1);
      border-color: rgba(239,68,68,0.4);
    }
  `],
})
export class ForbiddenComponent {
  private readonly router = inject(Router);
  private readonly auth   = inject(AuthService);

  readonly canAccessDashboard = computed(() => this.auth.isAdmin());

  readonly noAccessAtAll = computed(() =>
    !this.auth.isAdmin(),
  );

  readonly canGoBack = computed(() => {
    const ref = document.referrer;
    if (!ref) return false;
    try {
      const refUrl = new URL(ref);
      // Pas de retour si la page précédente était déjà /403 ou hors de l'app
      return refUrl.origin === window.location.origin && !refUrl.pathname.startsWith('/403');
    } catch {
      return false;
    }
  });

  goBack(): void      { window.history.back(); }
  goDashboard(): void { this.router.navigate(['/dashboard']); }
  logout(): void      { this.auth.logout(); this.router.navigate(['/login']); }
}
