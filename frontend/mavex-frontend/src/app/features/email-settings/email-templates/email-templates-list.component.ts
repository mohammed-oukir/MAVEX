import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { EmailTemplateService } from '../../../core/services/email-template.service';
import { ToastService } from '../../../core/services/toast.service';
import { EmailTemplate } from '../../../core/models/email-template.model';

@Component({
  selector: 'app-email-templates-list',
  imports: [CommonModule, FormsModule],
  templateUrl: './email-templates-list.component.html',
  styleUrl: './email-templates-list.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmailTemplatesListComponent implements OnInit {
  private readonly service = inject(EmailTemplateService);
  private readonly router  = inject(Router);
  private readonly toast   = inject(ToastService);
  private readonly destroy = inject(DestroyRef);

  readonly templates       = signal<EmailTemplate[]>([]);
  readonly loading         = signal(false);
  readonly selectedType    = signal<string>('');
  readonly subject         = signal<string>('');
  readonly currentTemplate = signal<EmailTemplate | null>(null);

  ngOnInit(): void {
    this.loadTemplates();
  }

  loadTemplates(): void {
    this.loading.set(true);
    this.service.getAll()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: templates => { this.templates.set(templates); this.loading.set(false); },
        error: ()       => { this.loading.set(false); this.toast.error('Erreur lors du chargement.'); },
      });
  }

  onTypeChange(type: string): void {
    this.selectedType.set(type);
    const template = this.templates().find(t => t.type === type) ?? null;
    this.currentTemplate.set(template);
    this.subject.set(template?.subject ?? '');
  }

  openEditor(): void {
    this.router.navigate(['/settings/email-templates', this.selectedType()]);
  }

  save(): void {
    const current = this.currentTemplate();
    const dto: EmailTemplate = { ...current, type: this.selectedType(), subject: this.subject() };
    const obs = current?.id
      ? this.service.update(current.id, dto)
      : this.service.create(dto);

    obs.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: () => { this.toast.success('Template sauvegardé avec succès.'); this.loadTemplates(); },
      error: (err: any) => this.toast.error(err?.error?.message || 'Erreur lors de la sauvegarde.'),
    });
  }

  cancel(): void {
    this.selectedType.set('');
    this.subject.set('');
    this.currentTemplate.set(null);
  }
}
