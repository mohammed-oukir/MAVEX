import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ShipperResponse } from '../models/shipper.model';

@Injectable({ providedIn: 'root' })
export class ShipperService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<ShipperResponse[]> {
    return this.http.get<ShipperResponse[]>('/api/shippers');
  }
}
