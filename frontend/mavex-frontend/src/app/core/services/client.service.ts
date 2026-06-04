import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ClientResponse, ClientRequest, ClientPatch } from '../models/client.model';

@Injectable({ providedIn: 'root' })
export class ClientService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<ClientResponse[]> {
    return this.http.get<ClientResponse[]>('/api/clients');
  }

  getAllActive(): Observable<ClientResponse[]> {
    return this.http.get<ClientResponse[]>('/api/clients/active');
  }

  getById(id: number): Observable<ClientResponse> {
    return this.http.get<ClientResponse>(`/api/clients/${id}`);
  }

  create(req: ClientRequest): Observable<ClientResponse> {
    return this.http.post<ClientResponse>('/api/clients', req);
  }

  update(id: number, req: ClientRequest): Observable<ClientResponse> {
    return this.http.put<ClientResponse>(`/api/clients/${id}`, req);
  }

  patch(id: number, req: ClientPatch): Observable<ClientResponse> {
    return this.http.patch<ClientResponse>(`/api/clients/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/clients/${id}`);
  }

  bulkDelete(ids: number[]): Observable<{ deleted: number }> {
    return this.http.post<{ deleted: number }>('/api/clients/bulk-delete', { ids });
  }

  bulkActivate(ids: number[]): Observable<{ activated: number }> {
    return this.http.post<{ activated: number }>('/api/clients/bulk-activate', { ids });
  }

  bulkDeactivate(ids: number[]): Observable<{ deactivated: number }> {
    return this.http.post<{ deactivated: number }>('/api/clients/bulk-deactivate', { ids });
  }
}
