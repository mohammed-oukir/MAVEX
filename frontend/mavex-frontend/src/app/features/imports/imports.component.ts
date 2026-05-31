import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe }   from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService }  from '../../core/services/layout.service';
import { ImportService }  from '../../core/services/import.service';
import { ToastService }   from '../../core/services/toast.service';
import { BadgeComponent } from '../../shared/badge/badge.component';
import { ImportLogResponse } from '../../core/models/import.model';

@Component({
  selector: 'app-imports',
  imports: [RouterLink, DatePipe, BadgeComponent],
  templateUrl: './imports.component.html',
  styleUrl:    './imports.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportsComponent implements OnInit {
  private readonly layout     = inject(LayoutService);
  private readonly importSvc  = inject(ImportService);
  private readonly toast      = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  /* ── Upload ───────────────────────────────────────────── */
  dragOver     = signal(false);
  selectedFile = signal<File | null>(null);
  uploading    = signal(false);
  result       = signal<ImportLogResponse | null>(null);
  showRows     = signal(false);

  /* ── History ──────────────────────────────────────────── */
  history        = signal<ImportLogResponse[]>([]);
  historyLoading = signal(true);

  /* ── Delete ───────────────────────────────────────────── */
  deleteId = signal<number | null>(null);
  deleting = signal(false);

  /* ── Detail modal ─────────────────────────────────────── */
  detail        = signal<ImportLogResponse | null>(null);
  loadingDetail = signal(false);

  /* ── Computed ─────────────────────────────────────────── */
  resultRate = computed(() => {
    const r = this.result();
    if (!r || r.totalRows === 0) return 0;
    return Math.round(((r.successRows ?? 0) / r.totalRows) * 100);
  });

  failedRows  = computed(() => (this.result()?.rows ?? []).filter(r => r.status === 'FAILED'));
  skippedRows = computed(() => (this.result()?.rows ?? []).filter(r => r.status === 'SKIPPED'));
  problemRows = computed(() => [...this.failedRows(), ...this.skippedRows()]);

  /* ── Lifecycle ────────────────────────────────────────── */
  ngOnInit(): void {
    this.layout.setPage('Imports');
    this.loadHistory();
  }

  /* ── Drag & Drop ──────────────────────────────────────── */
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
    (e.target as HTMLInputElement).value = '';
  }

  private setFile(f: File): void {
    if (!f.name.match(/\.(xlsx|xls)$/i)) {
      this.toast.error('Format non supporté. Utilisez .xlsx ou .xls'); return;
    }
    if (f.size > 10 * 1024 * 1024) {
      this.toast.error('Fichier trop volumineux. Maximum 10 MB.'); return;
    }
    this.selectedFile.set(f);
    this.result.set(null);
  }

  /* ── Upload ───────────────────────────────────────────── */
  upload(): void {
    const file = this.selectedFile();
    if (!file) return;
    this.uploading.set(true);
    this.result.set(null);
    this.importSvc.upload(file).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.uploading.set(false);
        this.result.set(res);
        this.selectedFile.set(null);
        this.loadHistory();
      },
      error: err => {
        this.uploading.set(false);
        this.toast.error(err?.error?.message || 'Erreur lors de l\'import.');
      },
    });
  }

  resetUpload(): void {
    this.selectedFile.set(null);
    this.result.set(null);
    this.showRows.set(false);
  }

  /* ── Detail modal ─────────────────────────────────────── */
  openDetail(id: number): void {
    this.detail.set(null);
    this.loadingDetail.set(true);
    this.importSvc.getById(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:  d  => { this.detail.set(d);  this.loadingDetail.set(false); },
      error: () => this.loadingDetail.set(false),
    });
  }

  closeDetail(): void { this.detail.set(null); }

  /* ── Delete ───────────────────────────────────────────── */
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
      error: () => { this.deleting.set(false); this.toast.error('Erreur.'); },
    });
  }

  /* ── Helpers ──────────────────────────────────────────── */
  private loadHistory(): void {
    this.historyLoading.set(true);
    this.importSvc.getHistory().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:  p  => { this.history.set(p.content); this.historyLoading.set(false); },
      error: () => this.historyLoading.set(false),
    });
  }

  historyRate(imp: ImportLogResponse): number {
    if (!imp.totalRows) return 0;
    return Math.round(((imp.successRows ?? 0) / imp.totalRows) * 100);
  }

  fmtSize(bytes: number): string {
    if (bytes < 1024) return `${bytes} B`;
    if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1048576).toFixed(1)} MB`;
  }
}
