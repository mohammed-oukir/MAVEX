import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../models/api.model';
import { PaymentTransactionResponse, PaymentTransactionSearchParams } from '../models/payment.model';

@Injectable({ providedIn: 'root' })
export class PaymentHistoryService {
  private readonly http = inject(HttpClient);

  search(params: PaymentTransactionSearchParams = {}, page = 0, size = 20): Observable<Page<PaymentTransactionResponse>> {
    let p = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'createdAt,desc');
    if (params.hawb)               p = p.set('hawb', params.hawb);
    if (params.client)             p = p.set('client', params.client);
    if (params.gateway)            p = p.set('gateway', params.gateway);
    if (params.status)             p = p.set('status', params.status);
    if (params.amountMin != null)  p = p.set('amountMin', params.amountMin);
    if (params.amountMax != null)  p = p.set('amountMax', params.amountMax);
    if (params.from)               p = p.set('from', params.from);
    if (params.to)                 p = p.set('to', params.to);
    return this.http.get<Page<PaymentTransactionResponse>>('/api/payments', { params: p });
  }
}
