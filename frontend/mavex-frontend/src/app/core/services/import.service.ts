import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Page } from '../models/api.model';
import { ImportConfirmRequest, ImportLogResponse, ImportPreviewResponse } from '../models/import.model';

@Injectable({ providedIn: 'root' })
export class ImportService {
  private readonly http = inject(HttpClient);

  upload(file: File): Observable<ImportLogResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ImportLogResponse>('/api/v1/imports/manifest', form);
  }

  preview(file: File): Observable<ImportPreviewResponse> {
    const form = new FormData();
    form.append('file', file);
    return this.http.post<ImportPreviewResponse>('/api/v1/imports/preview', form);
  }

  confirm(request: ImportConfirmRequest): Observable<ImportLogResponse> {
    return this.http.post<ImportLogResponse>('/api/v1/imports/confirm', request);
  }

  downloadTemplate(): Observable<Blob> {
    return this.http.get('/api/v1/imports/template', { responseType: 'blob' });
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
