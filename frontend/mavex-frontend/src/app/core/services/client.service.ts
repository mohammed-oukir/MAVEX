import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../models/api.model';
import { ClientResponse, ClientRequest, ClientPatch, ClientSearchCriteria } from '../models/client.model';

@Injectable({ providedIn: 'root' })
export class ClientService {
  private readonly http = inject(HttpClient);

  search(criteria: ClientSearchCriteria = {}, page = 0, size = 15): Observable<Page<ClientResponse>> {
    let p = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'createdAt,desc');
    if (criteria.name)    p = p.set('name', criteria.name);
    if (criteria.email)   p = p.set('email', criteria.email);
    if (criteria.phone)   p = p.set('phone', criteria.phone);
    if (criteria.city)    p = p.set('city', criteria.city);
    if (criteria.state)   p = p.set('state', criteria.state);
    if (criteria.country) p = p.set('country', criteria.country);
    if (criteria.status && criteria.status !== 'all') p = p.set('status', criteria.status);
    if (criteria.dateFrom) p = p.set('dateFrom', criteria.dateFrom);
    if (criteria.dateTo)   p = p.set('dateTo', criteria.dateTo);
    return this.http.get<Page<ClientResponse>>('/api/clients/search', { params: p });
  }

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
