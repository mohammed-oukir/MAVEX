import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../models/api.model';
import { ImportLogResponse } from '../models/import.model';

@Injectable({ providedIn: 'root' })
export class ImportService {
  private readonly http = inject(HttpClient);

  upload(file: File): Observable<ImportLogResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ImportLogResponse>('/api/v1/imports/manifest', form);
  }

  getHistory(page = 0, size = 20): Observable<Page<ImportLogResponse>> {
    const params = new HttpParams().set('page', page).set('size', size).set('sort', 'importedAt,desc');
    return this.http.get<Page<ImportLogResponse>>('/api/v1/imports', { params });
  }

  getById(id: number): Observable<ImportLogResponse> {
    return this.http.get<ImportLogResponse>(`/api/v1/imports/${id}`);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`/api/v1/imports/${id}`);
  }
}
