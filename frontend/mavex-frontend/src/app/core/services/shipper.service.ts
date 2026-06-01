import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Page } from '../models/api.model';
import { ShipperResponse, ShipperRequest } from '../models/shipper.model';

@Injectable({ providedIn: 'root' })
export class ShipperService {
  private readonly http = inject(HttpClient);

  getAll(size = 200): Observable<ShipperResponse[]> {
    const params = new HttpParams().set('size', size);
    return this.http.get<Page<ShipperResponse>>('/api/shippers', { params }).pipe(
      map(page => page.content),
    );
  }

  getById(id: number): Observable<ShipperResponse> {
    return this.http.get<ShipperResponse>(`/api/shippers/${id}`);
  }

  create(req: ShipperRequest): Observable<{ data: ShipperResponse }> {
    return this.http.post<{ data: ShipperResponse }>('/api/shippers', req);
  }

  update(id: number, req: ShipperRequest): Observable<{ data: ShipperResponse }> {
    return this.http.put<{ data: ShipperResponse }>(`/api/shippers/${id}`, req);
  }

  activate(id: number): Observable<void> {
    return this.http.patch<void>(`/api/shippers/${id}/activate`, {});
  }

  deactivate(id: number): Observable<void> {
    return this.http.patch<void>(`/api/shippers/${id}/deactivate`, {});
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/shippers/${id}`);
  }
}
