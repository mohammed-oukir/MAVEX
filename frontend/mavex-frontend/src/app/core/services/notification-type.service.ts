import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NotificationType } from '../models/notification-type.model';

const BASE = '/api/notification-types';

@Injectable({ providedIn: 'root' })
export class NotificationTypeService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<NotificationType[]> {
    return this.http.get<NotificationType[]>(BASE);
  }

  create(name: string): Observable<NotificationType> {
    return this.http.post<NotificationType>(BASE, { name });
  }

  update(id: number, name: string): Observable<NotificationType> {
    return this.http.put<NotificationType>(`${BASE}/${id}`, { name });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${BASE}/${id}`);
  }
}
