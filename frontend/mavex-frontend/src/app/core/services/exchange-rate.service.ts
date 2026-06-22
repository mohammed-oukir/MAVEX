import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  ExchangeRate,
  ExchangeRateRequest,
  ExchangeRateImportResult,
} from '../models/exchange-rate.model';

@Injectable({ providedIn: 'root' })
export class ExchangeRateService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/exchange-rates';

  getAll(fromCurrency?: string, toCurrency?: string, isActive?: boolean): Observable<ExchangeRate[]> {
    let params = new HttpParams();
    if (fromCurrency)       params = params.set('fromCurrency', fromCurrency);
    if (toCurrency)         params = params.set('toCurrency',   toCurrency);
    if (isActive !== undefined) params = params.set('isActive', String(isActive));
    return this.http.get<ExchangeRate[]>(this.base, { params });
  }

  getActive(from: string, to: string): Observable<ExchangeRate> {
    const params = new HttpParams().set('from', from).set('to', to);
    return this.http.get<ExchangeRate>(`${this.base}/active`, { params });
  }

  create(req: ExchangeRateRequest): Observable<ExchangeRate> {
    return this.http.post<ExchangeRate>(this.base, req);
  }

  update(id: number, req: ExchangeRateRequest): Observable<ExchangeRate> {
    return this.http.put<ExchangeRate>(`${this.base}/${id}`, req);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.base}/${id}`);
  }

  importFile(file: File): Observable<ExchangeRateImportResult> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ExchangeRateImportResult>(`${this.base}/import`, form);
  }
}
