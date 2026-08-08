import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

const STATUS_MAP: Record<string, string> = {
  CREATED:         'bs-created',
  ACTIVE:          'bs-active',
  INACTIVE:        'bs-inactive',
  DRAFT:           'bs-draft',
  IMPORTED:        'bs-imported',
  EMAIL_SENT:      'bs-email_sent',
  EMAIL_OUTDATED:  'bs-email_outdated',
  PAID:            'bs-paid',
  DELIVERED:       'bs-delivered',
  SUCCESS:         'bs-success',
  PARTIAL:         'bs-partial',
  FAILED:          'bs-failed',
  SKIPPED:         'bs-skipped',
  PROCESSING:      'bs-processing',
  CLOSED:          'bs-closed',
  CANCELLED:       'bs-cancelled',
  INITIATED:       'bs-initiated',
  MANUEL:          'bs-manuel',
  PAYPAL:          'bs-paypal',
};

const LABEL_MAP: Record<string, string> = {
  CREATED:         'Créé',
  ACTIVE:          'Actif',
  INACTIVE:        'Inactif',
  DRAFT:           'Draft',
  IMPORTED:        'Imported',
  EMAIL_SENT:      'Email envoyé',
  EMAIL_OUTDATED:  'Email obsolète',
  PAID:            'Payé',
  DELIVERED:       'Livré',
  PROCESSING:      'Processing',
  CLOSED:          'Closed',
  SUCCESS:         'Succès',
  PARTIAL:         'Partiel',
  FAILED:          'Échoué',
  SKIPPED:         'Ignoré',
  CANCELLED:       'Annulé',
  INITIATED:       'Initié',
  MANUEL:          'Manuel',
  PAYPAL:          'PayPal',
};

@Component({
  selector: 'app-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span [class]="cls()">{{ label() }}</span>`,
})
export class BadgeComponent {
  status = input.required<string>();
  cls    = computed(() => `badge-status ${STATUS_MAP[this.status()] ?? 'bs-created'}`);
  label  = computed(() => LABEL_MAP[this.status()] ?? this.status());
}
