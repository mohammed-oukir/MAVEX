import { inject } from '@angular/core';
import { CanActivateFn, GuardResult, Router } from '@angular/router';
import { Observable, map } from 'rxjs';
import { AuthService } from './auth.service';
import { MyPermissionsService } from '../services/my-permissions.service';

/**
 * Guard paramétrable par module (via data: { module: 'AIRLINES' } sur la route) :
 * - ADMIN     -> toujours autorisé
 * - AGENT     -> autorisé si au moins une permission existe pour ce module
 * - autre rôle -> refusé
 */
export const moduleAccessGuard: CanActivateFn = (route): GuardResult | Observable<GuardResult> => {
  const auth   = inject(AuthService);
  const perms  = inject(MyPermissionsService);
  const router = inject(Router);

  if (!auth.isAuthenticated()) return router.createUrlTree(['/login']);
  if (auth.isAdmin()) return true;

  const module = route.data['module'] as string | undefined;

  if (!auth.isAgent() || !module) {
    return router.createUrlTree(['/403']);
  }

  // Attend la fin du chargement initial des permissions avant d'évaluer —
  // évite de lire un état encore vide si loadMyPermissions() est en cours.
  return perms.whenReady().pipe(
    map(() => perms.hasAnyPermissionInModule(module) ? true : router.createUrlTree(['/403'])),
  );
};
