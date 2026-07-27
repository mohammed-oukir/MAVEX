import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GatewayConfigRequest, GatewayConfigResponse, ManualPaymentConfigResponse } from '../models/payment-config.model';

const GATEWAY_BASE = '/api/gateway-configs';
const MANUAL_BASE  = '/api/manual-payment';

@Injectable({ providedIn: 'root' })
export class PaymentConfigService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<GatewayConfigResponse[]> {
    return this.http.get<GatewayConfigResponse[]>(GATEWAY_BASE);
  }

  create(request: GatewayConfigRequest): Observable<GatewayConfigResponse> {
    return this.http.post<GatewayConfigResponse>(GATEWAY_BASE, request);
  }

  update(id: number, request: GatewayConfigRequest): Observable<GatewayConfigResponse> {
    return this.http.put<GatewayConfigResponse>(`${GATEWAY_BASE}/${id}`, request);
  }

  activate(id: number): Observable<GatewayConfigResponse> {
    return this.http.patch<GatewayConfigResponse>(`${GATEWAY_BASE}/${id}/activate`, {});
  }

  deactivate(id: number): Observable<GatewayConfigResponse> {
    return this.http.patch<GatewayConfigResponse>(`${GATEWAY_BASE}/${id}/deactivate`, {});
  }

  uploadRib(file: File): Observable<ManualPaymentConfigResponse> {
    const form = new FormData();
    form.append('file', file, file.name);
    return this.http.post<ManualPaymentConfigResponse>(`${MANUAL_BASE}/rib`, form);
  }

  getCurrentRib(): Observable<ManualPaymentConfigResponse | null> {
    return this.http.get<ManualPaymentConfigResponse | null>(`${MANUAL_BASE}/rib`);
  }
}
