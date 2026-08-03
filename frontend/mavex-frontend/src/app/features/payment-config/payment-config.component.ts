import {
  ChangeDetectionStrategy,
  Component,
  DestroyRef,
  OnInit,
  computed,
  inject,
  signal,
} from '@angular/core';
import { DatePipe } from '@angular/common';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { LayoutService } from '../../core/services/layout.service';
import { ToastService } from '../../core/services/toast.service';
import { PaymentConfigService } from '../../core/services/payment-config.service';
import {
  GatewayConfigResponse,
  ManualPaymentConfigResponse,
  PaymentGatewayType,
} from '../../core/models/payment-config.model';

@Component({
  selector: 'app-payment-config',
  imports: [DatePipe],
  templateUrl: './payment-config.component.html',
  styleUrl: './payment-config.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentConfigComponent implements OnInit {

  private readonly layout     = inject(LayoutService);
  private readonly toast      = inject(ToastService);
  private readonly svc        = inject(PaymentConfigService);
  private readonly destroyRef = inject(DestroyRef);

  /* ── State ─────────────────────────────────────────────── */
  configs      = signal<GatewayConfigResponse[]>([]);
  currentRib   = signal<ManualPaymentConfigResponse | null>(null);
  loading      = signal(true);
  saving       = signal(false);
  selectedType = signal<PaymentGatewayType>('MANUEL');

  /* ── Upload RIB ────────────────────────────────────────── */
  ribFile     = signal<File | null>(null);
  ribDragOver = signal(false);

  readonly MAX_FILE_SIZE = 10 * 1024 * 1024; // 10 MB

  /* ── Derived ────────────────────────────────────────────── */
  activeConfig = computed(() =>
    this.configs().find(c => c.active) ?? null
  );

  manuelConfig = computed(() =>
    this.configs().find(c => c.type === 'MANUEL') ?? null
  );

  isManuelActive = computed(() =>
    this.activeConfig()?.type === 'MANUEL'
  );

  /* ── Init ───────────────────────────────────────────────── */
  ngOnInit(): void {
    this.layout.setPage('Configuration Paiement');
    this.loadAll();
  }

  /* ── Chargement ─────────────────────────────────────────── */
  private loadAll(): void {
    this.loading.set(true);
    this.svc.getAll()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: configs => {
          this.configs.set(configs);
          this.loading.set(false);
        },
        error: () => {
          this.loading.set(false);
          this.toast.error('Erreur lors du chargement des configurations de paiement.');
        },
      });

    this.svc.getCurrentRib()
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: rib => this.currentRib.set(rib),
        error: () => this.currentRib.set(null),
      });
  }

  /* ── Sélection d'une carte ──────────────────────────────── */
  selectProvider(type: PaymentGatewayType): void {
    if (type === 'PAYPAL') return; // désactivée pour l'instant
    this.selectedType.set(type);
  }

  /* ── Upload RIB — drag & drop ────────────────────────────── */
  onRibDragOver(event: DragEvent): void {
    event.preventDefault();
    this.ribDragOver.set(true);
  }

  onRibDragLeave(): void {
    this.ribDragOver.set(false);
  }

  onRibDrop(event: DragEvent): void {
    event.preventDefault();
    this.ribDragOver.set(false);
    const files = Array.from(event.dataTransfer?.files ?? []);
    this.setRibFile(files[0] ?? null);
  }

  onRibFileInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    const files = Array.from(input.files ?? []);
    this.setRibFile(files[0] ?? null);
    input.value = '';
  }

  private setRibFile(file: File | null): void {
    if (!file) return;
    if (file.type !== 'application/pdf') {
      this.toast.error('Le RIB doit être un fichier PDF.');
      return;
    }
    if (file.size > this.MAX_FILE_SIZE) {
      this.toast.error('Fichier trop volumineux (max 10 MB).');
      return;
    }
    this.ribFile.set(file);
  }

  clearRibFile(): void {
    this.ribFile.set(null);
  }

  /* ── Appliquer et activer ───────────────────────────────── */
  apply(): void {
    this.saving.set(true);
    const file = this.ribFile();

    if (file) {
      this.svc.uploadRib(file)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: rib => {
            this.currentRib.set(rib);
            this.ribFile.set(null);
            this.activateManuel();
          },
          error: err => {
            this.saving.set(false);
            this.toast.error(err?.error?.message || 'Erreur lors de l\'upload du RIB.');
          },
        });
    } else {
      this.activateManuel();
    }
  }

  private activateManuel(): void {
    const existing = this.manuelConfig();

    if (existing) {
      this.svc.activate(existing.id)
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: () => this.onActivated(),
          error: err => this.onActivateError(err),
        });
    } else {
      this.svc.create({
        type: 'MANUEL',
        name: 'Paiement manuel (virement)',
        mode: 'PRODUCTION',
      })
        .pipe(takeUntilDestroyed(this.destroyRef))
        .subscribe({
          next: created => {
            this.svc.activate(created.id)
              .pipe(takeUntilDestroyed(this.destroyRef))
              .subscribe({
                next: () => this.onActivated(),
                error: err => this.onActivateError(err),
              });
          },
          error: err => this.onActivateError(err),
        });
    }
  }

  private onActivated(): void {
    this.saving.set(false);
    this.toast.success('Paiement manuel activé avec succès.');
    this.loadAll();
  }

  private onActivateError(err: any): void {
    this.saving.set(false);
    this.toast.error(err?.error?.message || 'Erreur lors de l\'activation.');
  }
}
