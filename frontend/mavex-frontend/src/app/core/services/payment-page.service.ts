import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OrderResponse } from '../models/order.model';

export interface PaymentInitiateResponse {
  redirectUrl: string;
  transactionId: string;
  gateway: string;
}

@Injectable({ providedIn: 'root' })
export class PaymentPageService {
  private readonly http = inject(HttpClient);

  /** Charge les détails de l'order via le token de paiement (public, sans JWT) */
  getOrderByToken(token: string): Observable<OrderResponse> {
    return this.http.get<OrderResponse>(`/api/orders/pay/${token}`);
  }

  /** Initie le paiement — retourne l'URL de redirection vers le gateway (public) */
  initiatePayment(token: string): Observable<PaymentInitiateResponse> {
    return this.http.post<PaymentInitiateResponse>(`/api/payments/pay/${token}/initiate`, {});
  }
}
