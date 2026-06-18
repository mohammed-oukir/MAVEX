import { Directive, ElementRef, OnInit, inject, input } from '@angular/core';
import { PermissionAction, PermissionModule } from '../models/auth.model';
import { PermissionService } from '../services/permission.service';
import { AuthService } from '../auth/auth.service';

/**
 * Désactive (disabled + aria-disabled) un bouton si l'agent n'a pas la permission requise.
 * ADMIN → jamais désactivé.
 *
 * Usage : <button appRequiresPermission module="SHIPMENTS" action="CREATE">...</button>
 */
@Directive({
  selector: '[appRequiresPermission]',
  host: {},
})
export class RequiresPermissionDirective implements OnInit {
  private readonly el   = inject<ElementRef<HTMLElement>>(ElementRef);
  private readonly perm = inject(PermissionService);
  private readonly auth = inject(AuthService);

  readonly module = input.required<PermissionModule>();
  readonly action = input.required<PermissionAction>();

  ngOnInit(): void {
    if (this.auth.isAdmin()) return;

    const allowed = this.perm.hasPermission(this.module(), this.action());
    if (!allowed) {
      const el = this.el.nativeElement;
      (el as HTMLButtonElement).disabled = true;
      el.setAttribute('aria-disabled', 'true');
      el.setAttribute('title', 'Permission insuffisante');
    }
  }
}
