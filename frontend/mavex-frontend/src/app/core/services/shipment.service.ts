import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../models/api.model';
import { ShipmentResponse, ShipmentRequest, ShipmentPatch, ShipmentStatusUpdate, DutyRateUpdate } from '../models/shipment.model';

@Injectable({ providedIn: 'root' })
export class ShipmentService {
  private readonly http = inject(HttpClient);

  getAll(page = 0, size = 15, sort = 'createdAt,desc'): Observable<Page<ShipmentResponse>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', sort);
    return this.http.get<Page<ShipmentResponse>>('/api/shipments', { params });
  }

  getAllUnpaged(size = 200): Observable<Page<ShipmentResponse>> {
    const params = new HttpParams().set('size', size);
    return this.http.get<Page<ShipmentResponse>>('/api/shipments', { params });
  }

  getById(id: number): Observable<ShipmentResponse> {
    return this.http.get<ShipmentResponse>(`/api/shipments/${id}`);
  }

  create(req: ShipmentRequest): Observable<ShipmentResponse> {
    return this.http.post<ShipmentResponse>('/api/shipments', req);
  }

  patch(id: number, req: ShipmentPatch): Observable<ShipmentResponse> {
    return this.http.patch<ShipmentResponse>(`/api/shipments/${id}`, req);
  }

  updateStatus(id: number, status: ShipmentStatusUpdate): Observable<ShipmentResponse> {
    return this.http.patch<ShipmentResponse>(`/api/shipments/${id}/status`, status);
  }

  updateDutyRate(id: number, update: DutyRateUpdate): Observable<ShipmentResponse> {
    return this.http.patch<ShipmentResponse>(`/api/shipments/${id}/duty-rate`, update);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/shipments/${id}`);
  }
}
