import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { ReactiveFormsModule, FormBuilder, Validators } from '@angular/forms';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService } from '../../core/services/layout.service';
import { ToastService } from '../../core/services/toast.service';
import { EmailProviderService } from '../../core/services/email-provider.service';
import {
  EmailProviderConfigResponse,
  EmailProviderType,
} from '../../core/models/email-provider.model';

@Component({
  selector: 'app-email-provider',
  imports: [ReactiveFormsModule],
  templateUrl: './email-provider.component.html',
  styleUrl: './email-provider.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class EmailProviderComponent implements OnInit {

  private readonly layout     = inject(LayoutService);
  private readonly toast      = inject(ToastService);
  private readonly svc        = inject(EmailProviderService);
  private readonly fb         = inject(FormBuilder);
  private readonly destroyRef = inject(DestroyRef);

  /* ── State ─────────────────────────────────────────────── */
  configs      = signal<EmailProviderConfigResponse[]>([]);
  loading      = signal(true);
  saving       = signal(false);
  testing      = signal(false);
  showSecrets  = signal(false);
  selectedType = signal<EmailProviderType>('BREVO');

  /* ── Derived ────────────────────────────────────────────── */
  activeConfig = computed(() =>
    this.configs().find(c => c.active) ?? null
  );

  selectedConfig = computed(() =>
    this.configs().find(c => c.providerType === this.selectedType()) ?? null
  );

  isSelectedActive = computed(() =>
    this.activeConfig()?.providerType === this.selectedType()
  );

  secretPlaceholder = computed(() =>
    this.showSecrets() ? '' : '••••••• (inchangé)'
  );

  isBrevo = computed(() => this.selectedType() === 'BREVO');
  isSmtp  = computed(() => this.selectedType() === 'SMTP');

  /* ── Forms ──────────────────────────────────────────────── */
  brevoForm = this.fb.group({
    fromEmail:         ['', [Validators.required, Validators.email]],
    fromName:          ['', Validators.required],
    brevoApiKey:       [''],
    brevoWebhookToken: [''],
  });

  smtpForm = this.fb.group({
    fromEmail:      ['', [Validators.required, Validators.email]],
    fromName:       ['', Validators.required],
    smtpHost:       ['', Validators.required],
    smtpPort:       [587, [Validators.required, Validators.min(1), Validators.max(65535)]],
    smtpUsername:   ['', Validators.required],
    smtpPassword:   [''],
    smtpSslEnabled: [false],
    smtpTlsEnabled: [true],
  });

  testEmailControl = this.fb.control('', [Validators.required, Validators.email]);

  get activeForm() {
    return this.selectedType() === 'BREVO' ? this.brevoForm : this.smtpForm;
  }

  /* ── Init ───────────────────────────────────────────────── */
  ngOnInit(): void {
    this.layout.setPage('Provider Email');
    this.loadConfigs();
  }

  /* ── Chargement ─────────────────────────────────────────── */
  private loadConfigs(): void {
    this.loading.set(true);
    this.svc.getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: configs => {
          this.configs.set(configs);
          this.loading.set(false);
          const active = configs.find(c => c.active);
          if (active) this.selectedType.set(active.providerType);
          this.patchForm(this.selectedType());
        },
        error: () => {
          this.loading.set(false);
          this.toast.error('Erreur lors du chargement des providers.');
        },
      });
  }

  /* ── Sélection d'une carte ──────────────────────────────── */
  selectProvider(type: EmailProviderType): void {
    this.selectedType.set(type);
    this.showSecrets.set(false);
    this.patchForm(type);
  }

  /* ── Pré-remplissage du formulaire ──────────────────────── */
  private patchForm(type: EmailProviderType): void {
    const config = this.configs().find(c => c.providerType === type);
    if (!config) return;

    if (type === 'BREVO') {
      this.brevoForm.patchValue({
        fromEmail: config.fromEmail ?? '',
        fromName:  config.fromName ?? '',
        brevoApiKey:       '',
        brevoWebhookToken: '',
      });
    } else {
      this.smtpForm.patchValue({
        fromEmail:      config.fromEmail ?? '',
        fromName:       config.fromName ?? '',
        smtpHost:       config.smtpHost ?? '',
        smtpPort:       config.smtpPort ?? 587,
        smtpUsername:   config.smtpUsername ?? '',
        smtpPassword:   '',
        smtpSslEnabled: config.smtpSslEnabled,
        smtpTlsEnabled: config.smtpTlsEnabled,
      });
    }
  }

  /* ── Afficher / masquer les secrets ─────────────────────── */
  toggleSecrets(): void {
    this.showSecrets.update(v => !v);
  }

  /* ── Sauvegarder et activer ─────────────────────────────── */
  apply(): void {
    const form = this.activeForm;
    if (form.invalid) {
      form.markAllAsTouched();
      return;
    }

    const type = this.selectedType();
    const v = form.getRawValue();
    this.saving.set(true);

    this.svc.save({ providerType: type, ...v } as any)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: saved => {
          this.svc.activate(saved.id)
            .pipe(takeUntilDestroyed(this.destroyRef))
            .subscribe({
              next: () => {
                this.saving.set(false);
                this.toast.success('Provider activé avec succès.');
                this.loadConfigs();
              },
              error: err => {
                this.saving.set(false);
                this.toast.error(err?.error?.message || 'Erreur lors de l\'activation.');
              },
            });
        },
        error: err => {
          this.saving.set(false);
          this.toast.error(err?.error?.message || 'Erreur lors de la sauvegarde.');
        },
      });
  }

  /* ── Tester la connexion ────────────────────────────────── */
  testConnection(): void {
    if (this.testEmailControl.invalid) {
      this.testEmailControl.markAsTouched();
      return;
    }

    const config = this.selectedConfig();
    if (!config) {
      this.toast.error('Sauvegardez d\'abord la configuration avant de tester.');
      return;
    }

    this.testing.set(true);
    this.svc.testConnection(config.id, this.testEmailControl.value!)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.testing.set(false);
          this.toast.success(res.message);
        },
        error: err => {
          this.testing.set(false);
          this.toast.error(err?.error?.message || 'Échec du test de connexion.');
        },
      });
  }

  /* ── Helpers template ───────────────────────────────────── */
  hasSecret(field: 'brevoApiKey' | 'brevoWebhookToken' | 'smtpPassword'): boolean {
    const c = this.selectedConfig();
    if (!c) return false;
    if (field === 'brevoApiKey')       return c.hasBrevoApiKey;
    if (field === 'brevoWebhookToken') return c.hasBrevoWebhookToken;
    if (field === 'smtpPassword')      return c.hasSmtpPassword;
    return false;
  }
}
