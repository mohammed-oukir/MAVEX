import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { DashboardKpi, StatusCount, MonthlyCount, MonthlyRevenue, DashboardAlerts } from '../models/dashboard.model';

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private readonly http = inject(HttpClient);

  getKpis(): Observable<DashboardKpi> {
    return this.http.get<DashboardKpi>('/api/dashboard/kpis');
  }

  getOrdersByStatus(): Observable<StatusCount[]> {
    return this.http.get<StatusCount[]>('/api/dashboard/orders-by-status');
  }

  getShipmentsByMonth(): Observable<MonthlyCount[]> {
    return this.http.get<MonthlyCount[]>('/api/dashboard/shipments-by-month');
  }

  getRevenueByMonth(): Observable<MonthlyRevenue[]> {
    return this.http.get<MonthlyRevenue[]>('/api/dashboard/revenue-by-month');
  }

  getAlerts(): Observable<DashboardAlerts> {
    return this.http.get<DashboardAlerts>('/api/dashboard/alerts');
  }
}
