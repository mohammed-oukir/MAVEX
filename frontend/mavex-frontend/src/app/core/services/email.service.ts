import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class EmailService {
  private readonly http = inject(HttpClient);

  sendToOrder(orderId: number, files: File[] = []): Observable<unknown> {
    const form = this.buildForm(files);
    return this.http.post(`/api/emails/orders/${orderId}/send`, form);
  }

  sendToShipment(shipmentId: number, files: File[] = []): Observable<unknown> {
    const form = this.buildForm(files);
    return this.http.post(`/api/emails/shipments/${shipmentId}/send-all`, form);
  }

  sendBulkEmail(ids: number[], files: File[] = []): Observable<unknown> {
    const form = this.buildForm(files);
    form.append('request', new Blob([JSON.stringify({ ids })], { type: 'application/json' }));
    return this.http.post(`/api/orders/bulk/email`, form);
  }

  private buildForm(files: File[]): FormData {
    const form = new FormData();
    files.forEach(f => form.append('files', f, f.name));
    return form;
  }
}
