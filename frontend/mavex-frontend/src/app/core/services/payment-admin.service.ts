import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import {
  GatewayConfigRequest, GatewayConfigResponse,
  PaymentTransactionResponse,
} from '../models/payment.model';
import { ApiResponse } from '../models/api.model';

@Injectable({ providedIn: 'root' })
export class PaymentAdminService {
  private readonly http = inject(HttpClient);

  /* ── Transactions ─────────────────────────────────── */
  getTransactionsByOrder(orderId: number): Observable<PaymentTransactionResponse[]> {
    return this.http.get<PaymentTransactionResponse[]>(
      `/api/payments/transactions/order/${orderId}`
    );
  }

  /* ── Gateways ─────────────────────────────────────── */
  getGateways(): Observable<GatewayConfigResponse[]> {
    return this.http.get<GatewayConfigResponse[]>('/api/gateway-configs');
  }

  createGateway(req: GatewayConfigRequest): Observable<GatewayConfigResponse> {
    return this.http.post<ApiResponse<GatewayConfigResponse>>('/api/gateway-configs', req)
      .pipe(map(r => r.data));
  }

  updateGateway(id: number, req: GatewayConfigRequest): Observable<GatewayConfigResponse> {
    return this.http.put<ApiResponse<GatewayConfigResponse>>(`/api/gateway-configs/${id}`, req)
      .pipe(map(r => r.data));
  }

  activateGateway(id: number): Observable<GatewayConfigResponse> {
    return this.http.patch<ApiResponse<GatewayConfigResponse>>(
      `/api/gateway-configs/${id}/activate`, {}
    ).pipe(map(r => r.data));
  }

  deactivateGateway(id: number): Observable<GatewayConfigResponse> {
    return this.http.patch<ApiResponse<GatewayConfigResponse>>(
      `/api/gateway-configs/${id}/deactivate`, {}
    ).pipe(map(r => r.data));
  }

  deleteGateway(id: number): Observable<void> {
    return this.http.delete<void>(`/api/gateway-configs/${id}`);
  }
}
