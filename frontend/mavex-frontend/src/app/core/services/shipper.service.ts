import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Page } from '../models/api.model';
import { ShipperResponse, ShipperRequest, ShipperSearchCriteria } from '../models/shipper.model';

@Injectable({ providedIn: 'root' })
export class ShipperService {
  private readonly http = inject(HttpClient);

  search(criteria: ShipperSearchCriteria = {}, page = 0, size = 15): Observable<Page<ShipperResponse>> {
    let p = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'createdAt,desc');
    if (criteria.company) p = p.set('company', criteria.company);
    if (criteria.contact) p = p.set('contact', criteria.contact);
    if (criteria.email)   p = p.set('email', criteria.email);
    if (criteria.phone)   p = p.set('phone', criteria.phone);
    if (criteria.city)    p = p.set('city', criteria.city);
    if (criteria.country) p = p.set('country', criteria.country);
    if (criteria.status && criteria.status !== 'all') p = p.set('status', criteria.status);
    return this.http.get<Page<ShipperResponse>>('/api/shippers/search', { params: p });
  }

  getAll(size = 200): Observable<ShipperResponse[]> {
    const params = new HttpParams().set('size', size);
    return this.http.get<Page<ShipperResponse>>('/api/shippers', { params }).pipe(
      map(page => page.content),
    );
  }

  getAllActive(size = 200): Observable<ShipperResponse[]> {
    const params = new HttpParams().set('size', size);
    return this.http.get<Page<ShipperResponse>>('/api/shippers', { params }).pipe(
      map(page => page.content.filter(s => s.active)),
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

  bulkDelete(ids: number[]): Observable<{ deleted: number }> {
    return this.http.post<{ deleted: number }>('/api/shippers/bulk-delete', { ids });
  }

  bulkActivate(ids: number[]): Observable<{ activated: number }> {
    return this.http.post<{ activated: number }>('/api/shippers/bulk-activate', { ids });
  }

  bulkDeactivate(ids: number[]): Observable<{ deactivated: number }> {
    return this.http.post<{ deactivated: number }>('/api/shippers/bulk-deactivate', { ids });
  }
}
