import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { PaypalConfigResponse, PaymentGatewayMode } from '../models/payment-config.model';

const PAYPAL_CONFIG_BASE = '/api/paypal-config';

export interface PaypalConfigRequest {
  clientId: string;
  clientSecret?: string;
  webhookId?: string;
  mode: PaymentGatewayMode;
}

@Injectable({ providedIn: 'root' })
export class PaypalConfigService {
  private readonly http = inject(HttpClient);

  saveConfig(request: PaypalConfigRequest): Observable<PaypalConfigResponse> {
    return this.http.post<PaypalConfigResponse>(PAYPAL_CONFIG_BASE, request);
  }

  getCurrentConfig(): Observable<PaypalConfigResponse | null> {
    return this.http.get<PaypalConfigResponse | null>(PAYPAL_CONFIG_BASE);
  }
}
