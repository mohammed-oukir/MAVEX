import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  EmailStatsHistoryResponse,
  BrevoQuotaResponse,
} from '../models/dashboard-stats.model';

@Injectable({ providedIn: 'root' })
export class DashboardAnalyticsService {
  private readonly http = inject(HttpClient);
  private readonly base = '/api/dashboard';

  getStatsHistory(days: number = 30): Observable<EmailStatsHistoryResponse> {
    const params = new HttpParams().set('days', String(days));
    return this.http.get<EmailStatsHistoryResponse>(`${this.base}/stats`, { params });
  }

  getQuota(): Observable<BrevoQuotaResponse> {
    return this.http.get<BrevoQuotaResponse>(`${this.base}/quota`);
  }

  recomputeStats(date?: string): Observable<{ date: string; message: string }> {
    let params = new HttpParams();
    if (date) params = params.set('date', date);
    return this.http.post<{ date: string; message: string }>(
      `${this.base}/stats/recompute`, null, { params }
    );
  }
}
