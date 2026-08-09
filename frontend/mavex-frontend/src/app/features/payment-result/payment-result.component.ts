import {
  ChangeDetectionStrategy, Component, DestroyRef, OnInit, inject, signal,
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { ActivatedRoute } from '@angular/router';
import { PaymentPageService } from '../../core/services/payment-page.service';

type ResultType = 'success' | 'error' | 'cancelled';

interface ResultConfig {
  type:    ResultType;
  icon:    string;
  title:   string;
  message: string;
  color:   string;
  bg:      string;
}

@Component({
  selector: 'app-payment-result',
  imports: [],
  templateUrl: './payment-result.component.html',
  styleUrl: './payment-result.component.css',
  changeDetection: ChangeDetectionStrategy.OnPush,
})
export class PaymentResultComponent implements OnInit {
  private readonly route      = inject(ActivatedRoute);
  private readonly svc        = inject(PaymentPageService);
  private readonly destroyRef = inject(DestroyRef);

  config    = signal<ResultConfig>(this.successConfig());
  verifying = signal(false);

  ngOnInit(): void {
    const type = this.route.snapshot.data['type'] as ResultType ?? 'success';

    if (type === 'success') {
      const paypalOrderId = this.route.snapshot.queryParamMap.get('token');
      if (paypalOrderId) {
        this.verifying.set(true);
        this.svc.capturePayment(paypalOrderId)
          .pipe(takeUntilDestroyed(this.destroyRef))
          .subscribe({
            next: res => {
              this.verifying.set(false);
              this.config.set(res.success ? this.successConfig() : this.errorConfig());
            },
            error: () => {
              this.verifying.set(false);
              this.config.set(this.errorConfig());
            },
          });
        return;
      }
      // type === 'success' mais aucun token PayPal dans l'URL — cas anormal
      this.config.set(this.errorConfig());
      return;
    }

    this.config.set(this.getConfig(type));
  }

  private getConfig(type: ResultType): ResultConfig {
    if (type === 'error')     return this.errorConfig();
    if (type === 'cancelled') return this.cancelledConfig();
    return this.successConfig();
  }

  private successConfig(): ResultConfig {
    return {
      type:    'success',
      icon:    'success',
      title:   'Payment Confirmed!',
      message: 'Your payment has been successfully recorded. Your shipment will be processed and delivered soon. You will receive an email confirmation.',
      color:   '#16A34A',
      bg:      '#DCFCE7',
    };
  }

  private errorConfig(): ResultConfig {
    return {
      type:    'error',
      icon:    'error',
      title:   'Payment Error',
      message: 'An error occurred while processing your payment. Please try again using the link received by email, or contact our customer service.',
      color:   '#DC2626',
      bg:      '#FEE2E2',
    };
  }

  private cancelledConfig(): ResultConfig {
    return {
      type:    'cancelled',
      icon:    'cancelled',
      title:   'Payment Cancelled',
      message: 'You cancelled the payment. Your shipment remains on hold. You can try again at any time using the link in your email.',
      color:   '#D97706',
      bg:      '#FEF3C7',
    };
  }
}
