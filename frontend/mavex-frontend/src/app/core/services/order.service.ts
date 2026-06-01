import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../models/api.model';
import {
  BulkEmailResult, OrderPatch, OrderRequest, OrderResponse,
  OrderSearchParams, OrderStatus, OrderStatusUpdate,
} from '../models/order.model';

@Injectable({ providedIn: 'root' })
export class OrderService {
  private readonly http = inject(HttpClient);

  search(params: OrderSearchParams = {}, page = 0, size = 20): Observable<Page<OrderResponse>> {
    let p = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'createdAt,desc');
    if (params.q)          p = p.set('q', params.q);
    if (params.status)     p = p.set('status', params.status);
    if (params.shipmentId) p = p.set('shipmentId', params.shipmentId);
    if (params.from)       p = p.set('from', params.from);
    if (params.to)         p = p.set('to', params.to);
    return this.http.get<Page<OrderResponse>>('/api/orders', { params: p });
  }

  getById(id: number): Observable<OrderResponse> {
    return this.http.get<OrderResponse>(`/api/orders/${id}`);
  }

  getByShipment(shipmentId: number): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>(`/api/orders/shipment/${shipmentId}`);
  }

  getByClient(clientId: number): Observable<OrderResponse[]> {
    return this.http.get<OrderResponse[]>(`/api/orders/client/${clientId}`);
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

  bulkEmail(ids: number[]): Observable<BulkEmailResult> {
    return this.http.post<BulkEmailResult>('/api/orders/bulk/email', { ids });
  }

  bulkStatus(ids: number[], newStatus: OrderStatus, note?: string): Observable<void> {
    return this.http.post<void>('/api/orders/bulk/status', { ids, newStatus, note });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/orders/${id}`);
  }
}
