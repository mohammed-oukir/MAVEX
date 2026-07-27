import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface SendEmailResult {
  success:      boolean;
  message:      string;
  toEmail?:     string;
  hawb?:        string;
  orderStatus?: string;
}

export interface BulkEmailSummary {
  total:  number;
  sent:   number;
  failed: number;
}

@Injectable({ providedIn: 'root' })
export class EmailService {
  private readonly http = inject(HttpClient);

  sendToOrder(orderId: number): Observable<SendEmailResult> {
    return this.http.post<SendEmailResult>(`/api/emails/orders/${orderId}/send`, new FormData());
  }

  sendToShipment(shipmentId: number, files: File[] = []): Observable<BulkEmailSummary> {
    const form = this.buildForm(files);
    return this.http.post<BulkEmailSummary>(`/api/emails/shipments/${shipmentId}/send-all`, form);
  }

  sendBulkEmail(ids: number[]): Observable<BulkEmailSummary> {
    const form = new FormData();
    form.append('request', new Blob([JSON.stringify({ ids })], { type: 'application/json' }));
    return this.http.post<BulkEmailSummary>(`/api/orders/bulk/email`, form);
  }

  private buildForm(files: File[]): FormData {
    const form = new FormData();
    files.forEach(f => form.append('files', f, f.name));
    return form;
  }
}
