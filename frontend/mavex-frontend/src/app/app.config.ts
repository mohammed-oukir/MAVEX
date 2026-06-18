import { APP_INITIALIZER, ApplicationConfig, inject, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { authInterceptor } from './core/auth/auth.interceptor';
import { AuthService } from './core/auth/auth.service';
import { PermissionService } from './core/services/permission.service';
import { catchError, of } from 'rxjs';

/**
 * Au démarrage de l'app (F5, rechargement), si l'utilisateur a déjà un token
 * stocké, on recharge les permissions depuis l'API avant que le router n'évalue
 * les guards. En cas d'erreur (token expiré, réseau), on continue silencieusement.
 */
function initPermissions() {
  return () => {
    const auth        = inject(AuthService);
    const permissions = inject(PermissionService);

    if (!auth.isAuthenticated()) return of(null);

    // Si ADMIN, on hydrate directement depuis le rôle stocké (pas besoin d'API)
    if (auth.isAdmin()) {
      permissions.setPermissions({} as any, true);
      return of(null);
    }

    return permissions.loadFromApi().pipe(catchError(() => of(null)));
  };
}

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    provideHttpClient(withInterceptors([authInterceptor])),
    {
      provide: APP_INITIALIZER,
      useFactory: initPermissions,
      multi: true,
    },
  ],
};
