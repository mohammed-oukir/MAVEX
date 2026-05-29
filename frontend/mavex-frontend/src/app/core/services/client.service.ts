import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ClientResponse, ClientRequest } from '../models/client.model';

@Injectable({ providedIn: 'root' })
export class ClientService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<ClientResponse[]> {
    return this.http.get<ClientResponse[]>('/api/clients');
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

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/clients/${id}`);
  }
}
