import { ChangeDetectionStrategy, Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { LayoutService } from '../../core/services/layout.service';
import { NotificationTypesComponent } from './notification-types/notification-types.component';
import { EmailTemplatesListComponent } from './email-templates/email-templates-list.component';

@Component({
  selector: 'app-email-settings',
  imports: [CommonModule, RouterModule, NotificationTypesComponent, EmailTemplatesListComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  styleUrl: './email-settings.component.css',
  template: `
    <div class="es-tabs">
      <button type="button" (click)="activeTab.set('types')"
              [class.active]="activeTab() === 'types'">Types de notification</button>
      <button type="button" (click)="activeTab.set('templates')"
              [class.active]="activeTab() === 'templates'">Templates email</button>
    </div>

    @if (activeTab() === 'types') {
      <app-notification-types />
    }
    @if (activeTab() === 'templates') {
      <app-email-templates-list />
    }
  `,
})
export class EmailSettingsComponent implements OnInit {
  private readonly layout = inject(LayoutService);

  readonly activeTab = signal<'types' | 'templates'>('types');

  ngOnInit(): void {
    this.layout.setPage('Email Settings');
  }
}
