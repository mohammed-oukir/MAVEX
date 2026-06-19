import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  EmailProviderConfigRequest,
  EmailProviderConfigResponse,
  TestConnectionResponse,
} from '../models/email-provider.model';

const BASE = '/api/settings/email-provider';

@Injectable({ providedIn: 'root' })
export class EmailProviderService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<EmailProviderConfigResponse[]> {
    return this.http.get<EmailProviderConfigResponse[]>(BASE);
  }

  getActive(): Observable<EmailProviderConfigResponse> {
    return this.http.get<EmailProviderConfigResponse>(`${BASE}/active`);
  }

  save(request: EmailProviderConfigRequest): Observable<EmailProviderConfigResponse> {
    return this.http.post<EmailProviderConfigResponse>(BASE, request);
  }

  activate(id: number): Observable<EmailProviderConfigResponse> {
    return this.http.put<EmailProviderConfigResponse>(`${BASE}/${id}/activate`, {});
  }

  testConnection(id: number, testEmail: string): Observable<TestConnectionResponse> {
    return this.http.post<TestConnectionResponse>(`${BASE}/${id}/test`, { testEmail });
  }
}
