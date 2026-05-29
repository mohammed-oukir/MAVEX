import { ChangeDetectionStrategy, Component, computed, input } from '@angular/core';

const STATUS_MAP: Record<string, string> = {
  CREATED:         'bs-created',
  DRAFT:           'bs-draft',
  IMPORTED:        'bs-imported',
  EMAIL_SENT:      'bs-email_sent',
  PENDING_PAYMENT: 'bs-pending_payment',
  PAID:            'bs-paid',
  IN_DELIVERY:     'bs-in_delivery',
  DELIVERED:       'bs-delivered',
  SUCCESS:         'bs-success',
  PARTIAL:         'bs-partial',
  FAILED:          'bs-failed',
  SKIPPED:         'bs-skipped',
  PROCESSING:      'bs-processing',
  CLOSED:          'bs-closed',
};

@Component({
  selector: 'app-badge',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `<span [class]="cls()">{{ status() }}</span>`,
})
export class BadgeComponent {
  status = input.required<string>();
  cls    = computed(() => `badge-status ${STATUS_MAP[this.status()] ?? 'bs-created'}`);
}
