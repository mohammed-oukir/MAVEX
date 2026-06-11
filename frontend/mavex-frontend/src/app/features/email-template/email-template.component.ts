import {
  ChangeDetectionStrategy, Component, DestroyRef,
  ElementRef, OnInit, ViewChild, inject, signal,
} from '@angular/core';
import { FormsModule } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService } from '../../core/services/layout.service';
import { ToastService }  from '../../core/services/toast.service';
import { EmailTemplateService, EmailTemplateDTO } from '../../core/services/email-template.service';
import Quill from 'quill';

@Component({
  selector: 'app-email-template',
  imports: [FormsModule],
  templateUrl: './email-template.component.html',
  styleUrl: './email-template.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmailTemplateComponent implements OnInit {

  @ViewChild('editorContainer') editorContainer!: ElementRef<HTMLDivElement>;
  @ViewChild('previewFrame')    previewFrame!:    ElementRef<HTMLIFrameElement>;

  private readonly layout      = inject(LayoutService);
  private readonly toast       = inject(ToastService);
  private readonly templateSvc = inject(EmailTemplateService);
  private readonly destroyRef  = inject(DestroyRef);

  private quill!: Quill;

  /* ── State ────────────────────────────────────────────── */
  template   = signal<EmailTemplateDTO | null>(null);
  loading    = signal(true);
  saving     = signal(false);
  previewing = signal(false);
  subject    = signal('');
  hasChanges = signal(false);

  /* ── Variables disponibles ───────────────────────────── */
  readonly VARIABLES: { label: string; key: string }[] = [
    { label: 'Nom client',        key: 'receiverName'    },
    { label: 'N° tracking',       key: 'hawb'            },
    { label: 'Montant total',     key: 'totalAmount'     },
    { label: 'Devise',            key: 'customsCurrency' },
    { label: 'Adresse livraison', key: 'deliveryAddress' },
    { label: 'Téléphone',        key: 'clientPhone'     },
    { label: 'Description',       key: 'goodsDescription'},
    { label: 'Shipper',           key: 'shipperName'     },
  ];

  /* ── Fausses données pour l'aperçu ───────────────────── */
  private readonly PREVIEW_DATA: Record<string, string> = {
    receiverName:     'Sarah Johnson',
    hawb:             'MX-2024-001-01',
    totalAmount:      '245.00',
    customsCurrency:  'USD',
    deliveryAddress:  '123 Main Street, New York, NY 10001',
    clientPhone:      '+1 202 555 0147',
    goodsDescription: 'Electronics — Laptop',
    shipperName:      'Med Africa Logistics',
  };

  ngOnInit(): void {
    this.layout.setPage('Email Template');
    this.loadTemplate();
  }

  private loadTemplate(): void {
    this.loading.set(true);
    this.templateSvc.getByType('PAYMENT_INVOICE_WITH_AMOUNT')
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: t => {
          this.template.set(t);
          this.subject.set(t.subject);
          this.loading.set(false);
          setTimeout(() => this.initQuill(t.bodyContent ?? ''), 0);
        },
        error: () => {
          this.loading.set(false);
          this.toast.error('Erreur lors du chargement du template.');
        },
      });
  }

  private initQuill(bodyContent: string): void {
    this.quill = new Quill(this.editorContainer.nativeElement, {
      theme: 'snow',
      placeholder: 'Rédigez le contenu de l\'email...',
      modules: {
        toolbar: [
          [{ header: [1, 2, 3, false] }],
          ['bold', 'italic', 'underline'],
          [{ color: [] }, { background: [] }],
          [{ align: [] }],
          [{ list: 'ordered' }, { list: 'bullet' }],
          ['link'],
          ['clean'],
        ],
      },
    });

    this.quill.clipboard.dangerouslyPasteHTML(bodyContent ?? '');

    this.quill.on('text-change', () => {
      this.hasChanges.set(true);
    });
  }

  insertVariable(key: string): void {
    const range = this.quill.getSelection(true);
    this.quill.insertText(range.index, `{{${key}}}`);
    this.quill.setSelection(range.index + key.length + 4);
    this.hasChanges.set(true);
  }

  /* ── Aperçu — injecte le bodyContent modifié dans le htmlContent complet */
  showPreview(): void {
    this.previewing.set(true);
    setTimeout(() => {
      const t = this.template();
      let html = t?.htmlContent ?? '';
      if (html) {
        // Remplacer le bodyContent dans le htmlContent complet
        const newBody = this.quill.root.innerHTML;
        html = html.replace(
          /(<!-- BODY_START -->)([\s\S]*?)(<!-- BODY_END -->)/,
          `$1\n${newBody}\n$3`
        );
      } else {
        html = this.quill.root.innerHTML;
      }
      // Remplacer les variables par les fausses données
      for (const [key, val] of Object.entries(this.PREVIEW_DATA)) {
        html = html.replaceAll(`{{${key}}}`, val);
      }
      this.previewFrame.nativeElement.srcdoc = html;
    }, 50);
  }

  closePreview(): void {
    this.previewing.set(false);
  }

  save(): void {
    const t = this.template();
    if (!t) return;
    this.saving.set(true);
    this.templateSvc.update(t.id, {
      id:          t.id,
      subject:     this.subject(),
      bodyContent: this.quill.root.innerHTML,
    })
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: updated => {
          this.template.set(updated);
          this.saving.set(false);
          this.hasChanges.set(false);
          this.toast.success('Template enregistré avec succès.');
        },
        error: () => {
          this.saving.set(false);
          this.toast.error('Erreur lors de l\'enregistrement.');
        },
      });
  }

  reset(): void {
    const t = this.template();
    if (!t) return;
    this.quill.clipboard.dangerouslyPasteHTML(t.bodyContent ?? '');
    this.subject.set(t.subject);
    this.hasChanges.set(false);
    this.toast.success('Contenu réinitialisé.');
  }
}
