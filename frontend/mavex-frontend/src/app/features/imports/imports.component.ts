import { ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DatePipe } from '@angular/common';
import { LayoutService } from '../../core/services/layout.service';
import { ImportService } from '../../core/services/import.service';
import { ToastService } from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/badge/badge.component';
import { ImportLogResponse } from '../../core/models/import.model';

@Component({
  selector: 'app-imports',
  imports: [FormsModule, DatePipe, BadgeComponent],
  templateUrl: './imports.component.html',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportsComponent implements OnInit {
  private readonly layout     = inject(LayoutService);
  private readonly importSvc  = inject(ImportService);
  private readonly toast      = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  dragOver       = signal(false);
  selectedFile   = signal<File | null>(null);
  mode           = signal<'PARTIAL' | 'STRICT'>('PARTIAL');
  uploading      = signal(false);
  history        = signal<ImportLogResponse[]>([]);
  historyLoading = signal(true);
  deleteId       = signal<number | null>(null);
  deleting       = signal(false);

  ngOnInit(): void {
    this.layout.setPage('Imports');
    this.loadHistory();
  }

  onDragOver(e: DragEvent): void { e.preventDefault(); this.dragOver.set(true); }
  onDragLeave(): void             { this.dragOver.set(false); }

  onDrop(e: DragEvent): void {
    e.preventDefault();
    this.dragOver.set(false);
    const file = e.dataTransfer?.files[0];
    if (file) this.setFile(file);
  }

  onFileChange(e: Event): void {
    const file = (e.target as HTMLInputElement).files?.[0];
    if (file) this.setFile(file);
  }

  private setFile(f: File): void {
    if (!f.name.match(/\.(xlsx|xls)$/i)) { this.toast.error('Fichier Excel requis (.xlsx ou .xls)'); return; }
    this.selectedFile.set(f);
  }

  upload(): void {
    const file = this.selectedFile();
    if (!file) return;
    this.uploading.set(true);
    this.importSvc.upload(file, this.mode()).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.uploading.set(false);
        this.selectedFile.set(null);
        this.toast.success('Import terminé avec succès.');
        this.loadHistory();
      },
      error: err => {
        this.uploading.set(false);
        this.toast.error(err?.error?.message || 'Erreur lors de l\'import.');
      },
    });
  }

  confirmDelete(id: number): void { this.deleteId.set(id); }
  cancelDelete(): void            { this.deleteId.set(null); }

  executeDelete(): void {
    const id = this.deleteId();
    if (!id) return;
    this.deleting.set(true);
    this.importSvc.delete(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.deleting.set(false);
        this.deleteId.set(null);
        this.toast.success('Import supprimé.');
        this.loadHistory();
      },
      error: () => { this.deleting.set(false); this.toast.error('Erreur lors de la suppression.'); },
    });
  }

  private loadHistory(): void {
    this.historyLoading.set(true);
    this.importSvc.getHistory().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: p => { this.history.set(p.content); this.historyLoading.set(false); },
      error: () => this.historyLoading.set(false),
    });
  }

  formatSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
  }
}
