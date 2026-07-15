import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ToastService } from '../../../core/services/toast.service';
import { NotificationTypeService } from '../../../core/services/notification-type.service';
import { NotificationType } from '../../../core/models/notification-type.model';

@Component({
  selector: 'app-notification-types',
  imports: [CommonModule],
  templateUrl: './notification-types.component.html',
  styleUrl: './notification-types.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class NotificationTypesComponent implements OnInit {
  private readonly service = inject(NotificationTypeService);
  private readonly toast   = inject(ToastService);
  private readonly destroy = inject(DestroyRef);

  readonly types      = signal<NotificationType[]>([]);
  readonly loading    = signal(false);
  readonly showForm   = signal(false);
  readonly editingId  = signal<number | null>(null);
  readonly formName   = signal('');

  ngOnInit(): void {
    this.loadTypes();
  }

  loadTypes(): void {
    this.loading.set(true);
    this.service.getAll()
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: types => { this.types.set(types); this.loading.set(false); },
        error: ()   => { this.loading.set(false); this.toast.error('Erreur lors du chargement.'); },
      });
  }

  openCreate(): void {
    this.editingId.set(null);
    this.formName.set('');
    this.showForm.set(true);
  }

  openEdit(t: NotificationType): void {
    this.editingId.set(t.id);
    this.formName.set(t.name);
    this.showForm.set(true);
  }

  save(): void {
    const id = this.editingId();
    const name = this.formName();
    const obs = id ? this.service.update(id, name) : this.service.create(name);
    obs.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: () => {
        this.toast.success(id ? 'Type modifié avec succès.' : 'Type créé avec succès.');
        this.loadTypes();
        this.showForm.set(false);
      },
      error: (err: any) => this.toast.error(err?.error?.message || 'Erreur lors de la sauvegarde.'),
    });
  }

  delete(id: number): void {
    this.service.delete(id)
      .pipe(takeUntilDestroyed(this.destroy))
      .subscribe({
        next: () => { this.toast.success('Type supprimé.'); this.loadTypes(); },
        error: (err: any) => this.toast.error(err?.error?.message || 'Erreur lors de la suppression.'),
      });
  }

  cancel(): void { this.showForm.set(false); }
}
