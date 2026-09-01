import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit,
  inject, input, signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { DecimalPipe } from '@angular/common';
import { PaymentPageService } from '../../core/services/payment-page.service';
import { OrderResponse } from '../../core/models/order.model';

type PageState = 'loading' | 'expired' | 'paid' | 'ready' | 'initiating' | 'redirecting' | 'error';

@Component({
  selector: 'app-payment-page',
  imports: [DecimalPipe],
  templateUrl: './payment-page.component.html',
  styleUrl: './payment-page.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentPageComponent implements OnInit {
  readonly token = input.required<string>();

  private readonly svc        = inject(PaymentPageService);
  private readonly destroyRef = inject(DestroyRef);

  state  = signal<PageState>('loading');
  order  = signal<OrderResponse | null>(null);
  errMsg = signal('');

  ngOnInit(): void {
    this.svc.getOrderByToken(this.token())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: o => {
          this.order.set(o);
          this.state.set(o.status === 'PAID' ? 'paid' : 'ready');
        },
        error: err => {
          const msg: string = err?.error?.message ?? '';
          if (msg.includes('expiré') || msg.includes('invalide')) {
            this.state.set('expired');
          } else {
            this.state.set('error');
            this.errMsg.set(msg || 'An error occurred.');
          }
        },
      });
  }

  pay(): void {
    this.state.set('initiating');
    this.svc.initiatePayment(this.token())
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe({
        next: res => {
          this.state.set('redirecting');
          setTimeout(() => { window.location.href = res.redirectUrl; }, 800);
        },
        error: err => {
          this.state.set('ready');
          this.errMsg.set(err?.error?.message || 'Error initiating the payment.');
        },
      });
  }

  get fmtTotal(): string {
    const o = this.order();
    if (!o?.totalAmount) return '—';
    return o.totalAmount.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
      + ' ' + (o.customsCurrency ?? 'USD');
  }

  get fmtDuty(): string {
    const o = this.order();
    if (!o?.dutyAmount) return '—';
    return o.dutyAmount.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
      + ' ' + (o.customsCurrency ?? 'USD');
  }

  get fmtCustoms(): string {
    const o = this.order();
    if (!o?.customsValue) return '—';
    return o.customsValue.toLocaleString('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
      + ' ' + (o.customsCurrency ?? 'USD');
  }
}
