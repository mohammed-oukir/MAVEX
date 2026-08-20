import {
  AfterViewInit, ChangeDetectionStrategy, Component, DestroyRef, OnDestroy, inject, signal,
} from '@angular/core';
import { CommonModule } from '@angular/common';
import { ActivatedRoute, Router } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import grapesjs, { Editor } from 'grapesjs';
import gjsNewsletter from 'grapesjs-preset-newsletter';
import { EmailTemplateService } from '../../../core/services/email-template.service';
import { ToastService } from '../../../core/services/toast.service';
import { AuthService } from '../../../core/auth/auth.service';
import { MyPermissionsService } from '../../../core/services/my-permissions.service';
import { EmailTemplate } from '../../../core/models/email-template.model';

@Component({
  selector: 'app-email-templates-editor',
  imports: [CommonModule],
  templateUrl: './email-templates-editor.component.html',
  styleUrl: './email-templates-editor.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmailTemplatesEditorComponent implements AfterViewInit, OnDestroy {
  private readonly route   = inject(ActivatedRoute);
  private readonly router  = inject(Router);
  private readonly service = inject(EmailTemplateService);
  private readonly toast   = inject(ToastService);
  private readonly destroy = inject(DestroyRef);
  protected readonly auth          = inject(AuthService);
  protected readonly myPermissions = inject(MyPermissionsService);

  readonly typeName = signal<string>('');
  readonly template  = signal<EmailTemplate | null>(null);
  readonly saving    = signal(false);

  private editor!: Editor;

  ngAfterViewInit(): void {
    this.typeName.set(this.route.snapshot.paramMap.get('type') ?? '');
    this.loadTemplate();
  }

  loadTemplate(): void {
    this.service.getByType(this.typeName()).pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: result => { this.template.set(result); this.initEditor(); },
      error: ()    => { this.template.set(null); this.initEditor(); },
    });
  }

  private initEditor(): void {
    if (this.editor) {
      this.editor.destroy();
    }

    this.editor = grapesjs.init({
      container: '#gjs',
      height: '100%',
      storageManager: false,
      plugins: [gjsNewsletter],
      pluginsOpts: {},
      blockManager: {
        appendTo: '#blocks-container',
      },
      layerManager: {
        appendTo: '#layers-container',
      },
      selectorManager: {
        appendTo: '#selector-container',
      },
      styleManager: {
        appendTo: '#style-container',
      },
      panels: { defaults: [] },
    });

    this.editor.Panels.addPanel({
      id: 'toolbar-basic',
      el: '#toolbar-container',
      buttons: [
        { id: 'device-desktop', command: 'set-device-desktop', label: '🖥' },
        { id: 'device-tablet', command: 'set-device-tablet', label: '📱' },
        { id: 'device-mobile', command: 'set-device-mobile', label: '📲' },
        { id: 'undo', command: 'core:undo', label: '↺' },
        { id: 'redo', command: 'core:redo', label: '↻' },
        { id: 'sw-visibility', command: 'sw-visibility', label: '👁' },
        { id: 'export-template', command: 'export-template', label: '</>' },
        { id: 'fullscreen', command: 'fullscreen', label: '⛶' },
      ],
    });

    const t = this.template();
    if (t?.builderJson) {
      try {
        this.editor.loadProjectData(JSON.parse(t.builderJson));
      } catch {
        this.editor.setComponents(t.htmlContent ?? '');
      }
    } else if (t?.htmlContent) {
      this.editor.setComponents(t.htmlContent);
    } else {
      this.editor.setComponents('');
    }

    this.addMavexBlocks();
  }

  private addMavexBlocks(): void {
    this.editor.BlockManager.add('mavex-receiver', {
      label: 'Nom Client',
      category: 'MAVEX Variables',
      media: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75"><circle cx="12" cy="8" r="4"/><path d="M4 20c0-4 4-6 8-6s8 2 8 6"/></svg>',
      content: '<span>{{receiverName}}</span>',
    });
    this.editor.BlockManager.add('mavex-payment-btn', {
      label: 'Bouton Paiement',
      category: 'MAVEX Variables',
      media: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75"><rect x="2" y="6" width="20" height="12" rx="2"/><line x1="2" y1="10" x2="22" y2="10"/></svg>',
      content: '<a href="{{paymentLink}}" style="background:#F97316;color:#fff;padding:12px 24px;border-radius:8px;text-decoration:none;display:inline-block;">Payer maintenant</a>',
    });
    this.editor.BlockManager.add('mavex-hawb', {
      label: 'N° Colis',
      category: 'MAVEX Variables',
      media: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75"><path d="M21 8l-9-5-9 5v8l9 5 9-5V8z"/><path d="M3.3 7l8.7 5 8.7-5M12 22V12"/></svg>',
      content: '<span>{{hawb}}</span>',
    });
    this.editor.BlockManager.add('mavex-duty', {
      label: 'Montant Douane',
      category: 'MAVEX Variables',
      media: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75"><circle cx="12" cy="12" r="9"/><path d="M12 7v10M9.5 9.5c0-1.5 1.2-2 2.5-2s2.5.7 2.5 2-1 1.8-2.5 2-2.5.5-2.5 2 1.2 2 2.5 2 2.5-.5 2.5-2"/></svg>',
      content: '<span>{{dutyAmount}} USD</span>',
    });
    this.editor.BlockManager.add('mavex-shipper', {
      label: 'Expéditeur',
      category: 'MAVEX Variables',
      media: '<svg viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="1.75"><path d="M3 21V8l9-5 9 5v13"/><path d="M9 21v-6h6v6"/></svg>',
      content: '<span>{{shipperName}}</span>',
    });
  }

  save(): void {
    this.saving.set(true);
    const builderJson = JSON.stringify(this.editor.getProjectData());
    const htmlContent  = this.editor.getHtml() + '<style>' + (this.editor.getCss() ?? '') + '</style>';
    const current = this.template();
    const dto: EmailTemplate = {
      type: this.typeName(),
      subject: current?.subject ?? '',
      htmlContent,
      builderJson,
      name: current?.name ?? this.typeName(),
    };
    const obs = current?.id
      ? this.service.update(current.id, dto)
      : this.service.create(dto);

    obs.pipe(takeUntilDestroyed(this.destroy)).subscribe({
      next: () => {
        this.saving.set(false);
        this.toast.success('Template sauvegardé avec succès.');
        this.router.navigate(['/settings/email-settings']);
      },
      error: (err: any) => {
        this.saving.set(false);
        this.toast.error(err?.error?.message || 'Erreur lors de la sauvegarde.');
      },
    });
  }

  goBack(): void {
    this.router.navigate(['/settings/email-settings']);
  }

  /* ── Permissions ──────────────────────────────────── */
  canCreateTemplate(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('EMAIL_SETTINGS', 'CREATE_TEMPLATE');
  }

  canUpdateTemplate(): boolean {
    return this.auth.isAdmin() || this.myPermissions.hasPermission('EMAIL_SETTINGS', 'UPDATE_TEMPLATE');
  }

  /** Le bouton Sauvegarder couvre create() OU update() selon si un template existe déjà pour ce type. */
  canSave(): boolean {
    return this.template()?.id ? this.canUpdateTemplate() : this.canCreateTemplate();
  }

  ngOnDestroy(): void {
    this.editor?.destroy();
  }
}
