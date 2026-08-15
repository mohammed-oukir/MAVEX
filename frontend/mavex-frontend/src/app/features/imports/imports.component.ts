import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  computed, inject, signal,
} from '@angular/core';
import { RouterLink } from '@angular/router';
import { DatePipe } from '@angular/common';
import { FormControl, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService }   from '../../core/services/layout.service';
import { ImportService }   from '../../core/services/import.service';
import { ToastService }    from '../../core/services/toast.service';
import { BadgeComponent }  from '../../shared/badge/badge.component';
import {
  ImportLogResponse, ImportPreviewResponse, ImportPreviewRow, ImportStatsResponse,
} from '../../core/models/import.model';

@Component({
  selector: 'app-imports',
  imports: [RouterLink, DatePipe, BadgeComponent, ReactiveFormsModule],
  templateUrl: './imports.component.html',
  styleUrl:    './imports.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class ImportsComponent implements OnInit {
  private readonly layout     = inject(LayoutService);
  private readonly importSvc  = inject(ImportService);
  private readonly toast      = inject(ToastService);
  private readonly destroyRef = inject(DestroyRef);

  /* ── Upload ───────────────────────────────────────────────── */
  dragOver     = signal(false);
  selectedFile = signal<File | null>(null);

  /* ── Preview (analyse sans import) ───────────────────────── */
  previewing   = signal(false);
  preview      = signal<ImportPreviewResponse | null>(null);
  previewRows  = signal<ImportPreviewRow[]>([]);   // mutable : l'user peut corriger

  /* ── Confirm (import final) ───────────────────────────────── */
  confirming = signal(false);
  result     = signal<ImportLogResponse | null>(null);
  showRows   = signal(false);

  /* ── Stats (KPI) ──────────────────────────────────────────── */
  stats = signal<ImportStatsResponse | null>(null);

  /* ── Édition d'une ligne invalide ─────────────────────────── */
  editingRow = signal<ImportPreviewRow | null>(null);
  revalidating = signal(false);

  /* ── Suppression d'une ligne ──────────────────────────────── */
  deletingRow = signal<ImportPreviewRow | null>(null);

  editForm = new FormGroup({
    mawb:               new FormControl('',   [Validators.required]),
    hawb:               new FormControl('',   [Validators.required]),
    alternateReference: new FormControl(''),
    senderName:         new FormControl(''),
    senderCountry:      new FormControl(''),
    senderAddress:      new FormControl(''),
    senderCity:         new FormControl(''),
    senderState:        new FormControl(''),
    senderPostcode:     new FormControl(''),
    senderContact:      new FormControl(''),
    senderPhone:        new FormControl(''),
    senderEmail:        new FormControl(''),
    receiverName:       new FormControl('',   [Validators.required]),
    receiverCountry:    new FormControl(''),
    receiverAddress:    new FormControl(''),
    receiverCity:       new FormControl(''),
    receiverState:      new FormControl(''),
    receiverPostcode:   new FormControl(''),
    receiverContact:    new FormControl(''),
    receiverPhone:      new FormControl(''),
    receiverEmail:      new FormControl('',   [Validators.required, Validators.pattern(/^[^@\s]+@[^@\s]+\.[^@\s]+$/)]),
    numberOfItems:      new FormControl<number | null>(null),
    goodsDescription:   new FormControl(''),
    shipmentWeight:     new FormControl<number | null>(null, [Validators.required, Validators.min(0.01)]),
    customsValue:       new FormControl<number | null>(null, [Validators.required, Validators.min(0.01)]),
    customsCurrency:    new FormControl(''),
  });

  /* ── Historique ───────────────────────────────────────────── */
  history        = signal<ImportLogResponse[]>([]);
  historyLoading = signal(true);
  historyFilter  = signal('');
  historyPage    = signal(0);
  historyTotal   = signal(0);
  readonly historyPageSize = 10;

  /* ── Suppression ──────────────────────────────────────────── */
  deleteId = signal<number | null>(null);
  deleting = signal(false);

  /* ── Détail modal ─────────────────────────────────────────── */
  detail        = signal<ImportLogResponse | null>(null);
  loadingDetail = signal(false);

  /* ── Computed ─────────────────────────────────────────────── */
  validRows   = computed(() => this.previewRows().filter(r => r.previewStatus === 'VALID'));
  invalidRows = computed(() => this.previewRows().filter(r => r.previewStatus === 'INVALID'));
  skippedRows = computed(() => this.previewRows().filter(r => r.previewStatus === 'SKIPPED'));
  canConfirm  = computed(() => this.validRows().length > 0);

  currentStep = computed<1 | 2 | 3>(() => {
    if (this.result())                       return 3;
    if (this.preview() || this.confirming())  return 2;
    return 1;
  });

  filteredHistory = computed(() => {
    const q = this.historyFilter().toLowerCase().trim();
    if (!q) return this.history();
    return this.history().filter(imp =>
      imp.fileName.toLowerCase().includes(q) ||
      (imp.mawb ?? '').toLowerCase().includes(q) ||
      imp.status.toLowerCase().includes(q)
    );
  });

  resultRate = computed(() => {
    const r = this.result();
    if (!r || r.totalRows === 0) return 0;
    return Math.round(((r.successRows ?? 0) / r.totalRows) * 100);
  });
  failedRowsResult  = computed(() => (this.result()?.rows ?? []).filter(r => r.status === 'FAILED'));
  skippedRowsResult = computed(() => (this.result()?.rows ?? []).filter(r => r.status === 'SKIPPED'));

  /* ── Lifecycle ────────────────────────────────────────────── */
  ngOnInit(): void {
    this.layout.setPage('Imports');
    this.loadHistory();
    this.loadStats();
  }

  /* ── Drag & Drop ──────────────────────────────────────────── */
  onDragOver(e: DragEvent): void { e.preventDefault(); this.dragOver.set(true); }
  onDragLeave(): void            { this.dragOver.set(false); }

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
    this.preview.set(null);
    this.result.set(null);
    this.previewRows.set([]);
  }

  /* ── Preview ──────────────────────────────────────────────── */
  analyseFile(): void {
    const file = this.selectedFile();
    if (!file) return;
    this.previewing.set(true);
    this.importSvc.preview(file).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.previewing.set(false);
        this.preview.set(res);
        this.previewRows.set(res.rows);
      },
      error: err => {
        this.previewing.set(false);
        this.toast.error(err?.error?.message || 'Erreur lors de l\'analyse.');
      },
    });
  }

  private lockScroll():   void { document.documentElement.classList.add('im-modal-open');    }
  private unlockScroll(): void { document.documentElement.classList.remove('im-modal-open'); }

  /* ── Édition d'une ligne ──────────────────────────────────── */
  openEdit(row: ImportPreviewRow): void {
    this.lockScroll();
    this.editingRow.set(row);
    this.editForm.patchValue({
      mawb:               row.mawb               ?? '',
      hawb:               row.hawb               ?? '',
      alternateReference: row.alternateReference  ?? '',
      senderName:         row.senderName          ?? '',
      senderCountry:      row.senderCountry       ?? '',
      senderAddress:      row.senderAddress       ?? '',
      senderCity:         row.senderCity          ?? '',
      senderState:        row.senderState         ?? '',
      senderPostcode:     row.senderPostcode      ?? '',
      senderContact:      row.senderContact       ?? '',
      senderPhone:        row.senderPhone         ?? '',
      senderEmail:        row.senderEmail         ?? '',
      receiverName:       row.receiverName        ?? '',
      receiverCountry:    row.receiverCountry     ?? '',
      receiverAddress:    row.receiverAddress     ?? '',
      receiverCity:       row.receiverCity        ?? '',
      receiverState:      row.receiverState       ?? '',
      receiverPostcode:   row.receiverPostcode    ?? '',
      receiverContact:    row.receiverContact     ?? '',
      receiverPhone:      row.receiverPhone       ?? '',
      receiverEmail:      row.receiverEmail       ?? '',
      numberOfItems:      row.numberOfItems       ?? null,
      goodsDescription:   row.goodsDescription    ?? '',
      shipmentWeight:     row.shipmentWeight      ?? null,
      customsValue:       row.customsValue        ?? null,
      customsCurrency:    row.customsCurrency     ?? '',
    });
  }

  closeEdit(): void {
    this.unlockScroll();
    this.editingRow.set(null);
    this.editForm.markAsUntouched();
  }

  saveEdit(): void {
    const original = this.editingRow();
    if (!original) return;

    // Force tous les champs à "touched" → les bordures rouges apparaissent immédiatement
    this.editForm.markAllAsTouched();
    if (this.editForm.invalid) return;

    const v = this.editForm.getRawValue();

    const corrected: ImportPreviewRow = {
      ...original,
      mawb:               v.mawb               ?? undefined,
      hawb:               v.hawb               ?? undefined,
      alternateReference: v.alternateReference  ?? undefined,
      senderName:         v.senderName          ?? undefined,
      senderCountry:      v.senderCountry       ?? undefined,
      senderAddress:      v.senderAddress       ?? undefined,
      senderCity:         v.senderCity          ?? undefined,
      senderState:        v.senderState         ?? undefined,
      senderPostcode:     v.senderPostcode      ?? undefined,
      senderContact:      v.senderContact       ?? undefined,
      senderPhone:        v.senderPhone         ?? undefined,
      senderEmail:        v.senderEmail         ?? undefined,
      receiverName:       v.receiverName        ?? undefined,
      receiverCountry:    v.receiverCountry     ?? undefined,
      receiverAddress:    v.receiverAddress     ?? undefined,
      receiverCity:       v.receiverCity        ?? undefined,
      receiverState:      v.receiverState       ?? undefined,
      receiverPostcode:   v.receiverPostcode    ?? undefined,
      receiverContact:    v.receiverContact     ?? undefined,
      receiverPhone:      v.receiverPhone       ?? undefined,
      receiverEmail:      v.receiverEmail       ?? undefined,
      numberOfItems:      v.numberOfItems       ?? undefined,
      goodsDescription:   v.goodsDescription    ?? undefined,
      shipmentWeight:     v.shipmentWeight      ?? undefined,
      customsValue:       v.customsValue        ?? undefined,
      customsCurrency:    v.customsCurrency     ?? undefined,
    };

    const candidateRows = this.previewRows().map(r =>
      r.rowNumber === original.rowNumber ? corrected : r
    );

    this.revalidating.set(true);
    this.importSvc.revalidate({ rows: candidateRows })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.previewRows.set(res.rows);
          this.revalidating.set(false);
          this.unlockScroll();
          this.editingRow.set(null);

          const updatedRow = res.rows.find(r => r.rowNumber === original.rowNumber);
          if (updatedRow?.previewStatus === 'INVALID') {
            this.toast.error(`${updatedRow.errors.length} erreur(s) : ${updatedRow.errors[0]}`);
          } else {
            this.toast.success('Ligne corrigée — prête à être importée.');
          }
        },
        error: () => {
          this.revalidating.set(false);
          this.toast.error('Impossible de revalider la ligne — vérifiez votre connexion et réessayez.');
        },
      });
  }

  /* ── Suppression d'une ligne ──────────────────────────────── */
  confirmRowDelete(row: ImportPreviewRow): void {
    this.lockScroll();
    this.deletingRow.set(row);
  }

  cancelRowDelete(): void {
    this.unlockScroll();
    this.deletingRow.set(null);
  }

  executeRowDelete(): void {
    const target = this.deletingRow();
    if (!target) return;

    const candidateRows = this.previewRows().filter(r => r.rowNumber !== target.rowNumber);

    // Plus aucune ligne : pas besoin d'aller-retour serveur, rien à revalider.
    if (candidateRows.length === 0) {
      this.previewRows.set([]);
      this.unlockScroll();
      this.deletingRow.set(null);
      this.toast.success('Ligne supprimée.');
      return;
    }

    this.revalidating.set(true);
    this.importSvc.revalidate({ rows: candidateRows })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: (res) => {
          this.previewRows.set(res.rows);
          this.revalidating.set(false);
          this.unlockScroll();
          this.deletingRow.set(null);
          this.toast.success('Ligne supprimée.');
        },
        error: () => {
          this.revalidating.set(false);
          this.toast.error('Impossible de supprimer la ligne — vérifiez votre connexion et réessayez.');
        },
      });
  }

  /* ── Confirm ──────────────────────────────────────────────── */
  confirmImport(): void {
    const preview = this.preview();
    if (!preview || !this.canConfirm()) return;

    // On envoie uniquement les lignes VALID (les INVALID restantes sont ignorées)
    const rowsToSend = this.validRows();

    this.confirming.set(true);
    this.importSvc.confirm({
      fileName: preview.fileName,
      fileHash: preview.fileHash,
      rows:     rowsToSend,
    }).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: res => {
        this.confirming.set(false);
        this.result.set(res);
        this.preview.set(null);
        this.previewRows.set([]);
        this.selectedFile.set(null);
        this.loadHistory();
        this.loadStats();
        this.toast.success('Import confirmé avec succès.');
      },
      error: err => {
        this.confirming.set(false);
        this.toast.error(err?.error?.message || 'Erreur lors de la confirmation.');
      },
    });
  }

  resetUpload(): void {
    this.selectedFile.set(null);
    this.preview.set(null);
    this.previewRows.set([]);
    this.result.set(null);
    this.showRows.set(false);
  }

  /* ── Template download ───────────────────────────────────── */
  downloadTemplate(): void {
    this.importSvc.downloadTemplate().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: blob => {
        const url = URL.createObjectURL(blob);
        const a   = document.createElement('a');
        a.href    = url;
        a.download = 'template_manifeste.xlsx';
        a.click();
        URL.revokeObjectURL(url);
      },
      error: () => this.toast.error('Impossible de télécharger le template.'),
    });
  }

  /* ── Détail modal ─────────────────────────────────────────── */
  openDetail(id: number): void {
    this.lockScroll();
    this.detail.set(null);
    this.loadingDetail.set(true);
    this.importSvc.getById(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:  d  => { this.detail.set(d);  this.loadingDetail.set(false); },
      error: () => { this.unlockScroll(); this.loadingDetail.set(false); },
    });
  }
  closeDetail(): void { this.unlockScroll(); this.detail.set(null); }

  /* ── Suppression ──────────────────────────────────────────── */
  confirmDelete(id: number): void { this.lockScroll();   this.deleteId.set(id);   }
  cancelDelete(): void            { this.unlockScroll(); this.deleteId.set(null); }

  executeDelete(): void {
    const id = this.deleteId();
    if (!id) return;
    this.deleting.set(true);
    this.importSvc.delete(id).pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next: () => {
        this.unlockScroll();
        this.deleting.set(false); this.deleteId.set(null);
        this.toast.success('Import supprimé.'); this.loadHistory();
      },
      error: (err) => { this.unlockScroll(); this.deleting.set(false); this.toast.error(err?.error?.message || 'Erreur.'); },
    });
  }

  /* ── Helpers ──────────────────────────────────────────────── */
  private loadHistory(): void {
    this.historyLoading.set(true);
    this.importSvc.getHistory(this.historyPage(), this.historyPageSize)
      .pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
        next: p => {
          this.history.set(p.content);
          this.historyTotal.set(p.totalElements);
          this.historyLoading.set(false);
        },
        error: () => this.historyLoading.set(false),
      });
  }

  private loadStats(): void {
    this.importSvc.getStats().pipe(takeUntilDestroyed(this.destroyRef)).subscribe({
      next:  s => this.stats.set(s),
      error: () => this.stats.set(null),
    });
  }

  goToPage(page: number): void {
    this.historyPage.set(page);
    this.loadHistory();
  }

  get historyTotalPages(): number {
    return Math.ceil(this.historyTotal() / this.historyPageSize);
  }

  historyRate(imp: ImportLogResponse): number {
    if (!imp.totalRows) return 0;
    return Math.round(((imp.successRows ?? 0) / imp.totalRows) * 100);
  }

  fmtSize(bytes: number): string {
    if (bytes < 1024)    return `${bytes} B`;
    if (bytes < 1048576) return `${(bytes / 1024).toFixed(1)} KB`;
    return `${(bytes / 1048576).toFixed(1)} MB`;
  }
}
