import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { PermissionModule } from '../models/auth.model';
import { PermissionService } from '../services/permission.service';
import { AuthService } from './auth.service';

/**
 * Factory guard : protège une route par module.
 * Usage : canActivate: [permissionGuard('SHIPMENTS')]
 *
 * - ADMIN → toujours autorisé
 * - Agent sans VIEW sur le module → redirigé vers /403
 * - Non authentifié → laissé à authGuard (toujours chaîné en amont)
 */
export function permissionGuard(module: PermissionModule): CanActivateFn {
  return () => {
    const auth        = inject(AuthService);
    const permissions = inject(PermissionService);
    const router      = inject(Router);

    if (!auth.isAuthenticated()) return router.createUrlTree(['/login']);
    if (auth.isAdmin())          return true;

    return permissions.canView(module)
      ? true
      : router.createUrlTree(['/403']);
  };
}
