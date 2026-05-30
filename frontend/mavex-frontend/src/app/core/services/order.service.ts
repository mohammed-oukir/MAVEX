import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { OrderResponse, OrderRequest, OrderPatch, OrderStatusUpdate } from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>('/api/orders');
  }

  getByShipment(shipmentId: number): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>(`/api/orders/shipment/${shipmentId}`);
  }

  create(req: OrderRequest): Observable<OrderResponse> {
    return this.http.post<OrderResponse>('/api/orders', req);
  }

  update(id: number, req: OrderRequest): Observable<OrderResponse> {
    return this.http.put<OrderResponse>(`/api/orders/${id}`, req);
  }

  patch(id: number, req: OrderPatch): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(`/api/orders/${id}`, req);
  }

  updateStatus(id: number, req: OrderStatusUpdate): Observable<OrderResponse> {
    return this.http.patch<OrderResponse>(`/api/orders/${id}/status`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/orders/${id}`);
  }
}
