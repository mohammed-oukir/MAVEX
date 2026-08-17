import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../models/api.model';
import { PaymentTransactionResponse, PaymentTransactionSearchParams, ReceiptSendResponse } from '../models/payment.model';

@Injectable({ providedIn: 'root' })
export class PaymentHistoryService {
  private readonly http = inject(HttpClient);

  search(params: PaymentTransactionSearchParams = {}, page = 0, size = 20): Observable<Page<PaymentTransactionResponse>> {
    const p = this.buildSearchParams(params)
      .set('page', page)
      .set('size', size)
      .set('sort', 'createdAt,desc');
    return this.http.get<Page<PaymentTransactionResponse>>('/api/payments', { params: p });
  }

  exportPdf(params: PaymentTransactionSearchParams = {}): Observable<Blob> {
    const p = this.buildSearchParams(params);
    return this.http.get('/api/payments/export/pdf', { params: p, responseType: 'blob' });
  }

  exportExcel(params: PaymentTransactionSearchParams = {}): Observable<Blob> {
    const p = this.buildSearchParams(params);
    return this.http.get('/api/payments/export/excel', { params: p, responseType: 'blob' });
  }

  exportExcelSelection(ids: number[]): Observable<Blob> {
    return this.http.post('/api/payments/export/excel/selection', { ids }, { responseType: 'blob' });
  }

  private buildSearchParams(params: PaymentTransactionSearchParams): HttpParams {
    let p = new HttpParams();
    if (params.hawb)               p = p.set('hawb', params.hawb);
    if (params.client)             p = p.set('client', params.client);
    if (params.gateway)            p = p.set('gateway', params.gateway);
    if (params.status)             p = p.set('status', params.status);
    if (params.amountMin != null)  p = p.set('amountMin', params.amountMin);
    if (params.amountMax != null)  p = p.set('amountMax', params.amountMax);
    if (params.from)               p = p.set('from', params.from);
    if (params.to)                 p = p.set('to', params.to);
    return p;
  }

  getTotalCollected(): Observable<number> {
    return this.http.get<number>('/api/payments/total-collected');
  }

  sendReceipt(transactionId: number): Observable<ReceiptSendResponse> {
    return this.http.post<ReceiptSendResponse>(
      `/api/payments/${transactionId}/send-receipt`, {});
  }
}
