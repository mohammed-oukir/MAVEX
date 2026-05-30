import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { map } from 'rxjs/operators';
import { Page } from '../models/api.model';
import { ShipperResponse } from '../models/shipper.model';

@Injectable({ providedIn: 'root' })
export class ShipperService {
  private readonly http = inject(HttpClient);

  getAll(size = 200): Observable<ShipperResponse[]> {
    const params = new HttpParams().set('size', size);
    return this.http.get<Page<ShipperResponse>>('/api/shippers', { params }).pipe(
      map(page => page.content),
    );
  }
}
