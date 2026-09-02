import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../models/api.model';
import { ShipmentResponse, ShipmentRequest, ShipmentPatch, ShipmentStatus, ShipmentStatusUpdate, DutyRateUpdate, DutyChangeHistoryResponse } from '../models/shipment.model';

export interface DutyHistoryShipmentParams {
  changedByName?: string;
  from?: string;
  to?: string;
}

export interface DutyHistoryOrdersParams {
  hawb?: string;
  changedByName?: string;
  from?: string;
  to?: string;
}

export interface ShipmentSearchParams {
  mawb?:         string;
  shipper?:      string;
  importFrom?:   string;
  importTo?:     string;
  carrier?:      string;
  mode?:         string;
  totalOrders?:  number;
  dutyRateMin?:  number;
  dutyRateMax?:  number;
  status?:       ShipmentStatus | '';
}

@Injectable({ providedIn: 'root' })
export class ShipmentService {
  private readonly http = inject(HttpClient);

  search(params: ShipmentSearchParams = {}, page = 0, size = 15): Observable<Page<ShipmentResponse>> {
    const p = this.buildSearchParams(params)
      .set('page', page)
      .set('size', size)
      .set('sort', 'createdAt,desc');
    return this.http.get<Page<ShipmentResponse>>('/api/shipments', { params: p });
  }

  exportPdf(params: ShipmentSearchParams = {}): Observable<Blob> {
    const p = this.buildSearchParams(params);
    return this.http.get('/api/shipments/export/pdf', { params: p, responseType: 'blob' });
  }

  exportExcel(params: ShipmentSearchParams = {}): Observable<Blob> {
    const p = this.buildSearchParams(params);
    return this.http.get('/api/shipments/export/excel', { params: p, responseType: 'blob' });
  }

  exportExcelSelection(ids: number[]): Observable<Blob> {
    return this.http.post('/api/shipments/export/excel/selection', { ids }, { responseType: 'blob' });
  }

  private buildSearchParams(params: ShipmentSearchParams): HttpParams {
    let p = new HttpParams();
    if (params.mawb)       p = p.set('mawb', params.mawb);
    if (params.shipper)    p = p.set('shipper', params.shipper);
    if (params.importFrom) p = p.set('importFrom', params.importFrom);
    if (params.importTo)   p = p.set('importTo', params.importTo);
    if (params.carrier)    p = p.set('carrier', params.carrier);
    if (params.mode)       p = p.set('mode', params.mode);
    if (params.totalOrders != null) p = p.set('totalOrders', params.totalOrders);
    if (params.dutyRateMin != null) p = p.set('dutyRateMin', params.dutyRateMin);
    if (params.dutyRateMax != null) p = p.set('dutyRateMax', params.dutyRateMax);
    if (params.status)     p = p.set('status', params.status);
    return p;
  }

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

  getShipmentDutyHistory(
    shipmentId: number, params: DutyHistoryShipmentParams = {}, page = 0, size = 20
  ): Observable<Page<DutyChangeHistoryResponse>> {
    let p = new HttpParams().set('page', page).set('size', size);
    if (params.changedByName) p = p.set('changedByName', params.changedByName);
    if (params.from)          p = p.set('from', params.from);
    if (params.to)            p = p.set('to', params.to);
    return this.http.get<Page<DutyChangeHistoryResponse>>(
      `/api/shipments/${shipmentId}/duty-history/shipment`, { params: p });
  }

  getOrderDutyHistory(
    shipmentId: number, params: DutyHistoryOrdersParams = {}, page = 0, size = 20
  ): Observable<Page<DutyChangeHistoryResponse>> {
    let p = new HttpParams().set('page', page).set('size', size);
    if (params.hawb)           p = p.set('hawb', params.hawb);
    if (params.changedByName)  p = p.set('changedByName', params.changedByName);
    if (params.from)           p = p.set('from', params.from);
    if (params.to)             p = p.set('to', params.to);
    return this.http.get<Page<DutyChangeHistoryResponse>>(
      `/api/shipments/${shipmentId}/duty-history/orders`, { params: p });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/shipments/${id}`);
  }
}
