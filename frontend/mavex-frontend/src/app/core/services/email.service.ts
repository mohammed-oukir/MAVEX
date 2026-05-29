import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class EmailService {
  private readonly http = inject(HttpClient);

  sendToOrder(orderId: number): Observable<unknown> {
    return this.http.post(`/api/emails/orders/${orderId}/send`, {});
  }

  sendToShipment(shipmentId: number): Observable<unknown> {
    return this.http.post(`/api/emails/shipments/${shipmentId}/send-all`, {});
  }
}
