import { ChangeDetectionStrategy, Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SidebarComponent } from '../sidebar/sidebar.component';
import { TopbarComponent } from '../topbar/topbar.component';
import { ToastService, Toast } from '../../core/services/toast.service';

@Component({
  selector: 'app-main-layout',
  imports: [RouterOutlet, SidebarComponent, TopbarComponent],
  templateUrl: './main-layout.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class MainLayoutComponent {
  protected readonly toast = inject(ToastService);

  toastIcon(type: Toast['type']): string {
    return type === 'success' ? '✓' : type === 'error' ? '✕' : 'i';
  }

  toastClass(type: Toast['type']): string {
    const map: Record<Toast['type'], string> = {
      success: 'text-bg-success',
      error:   'text-bg-danger',
      info:    'text-bg-info',
    };
    return map[type];
  }
}
