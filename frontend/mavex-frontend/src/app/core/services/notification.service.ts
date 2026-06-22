import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NotificationItem } from '../models/notification.model';

@Injectable({ providedIn: 'root' })
export class NotificationService {
  private readonly http = inject(HttpClient);

  getUnread(): Observable<NotificationItem[]> {
    return this.http.get<NotificationItem[]>('/api/notifications/unread');
  }

  getUnreadCount(): Observable<{ count: number }> {
    return this.http.get<{ count: number }>('/api/notifications/unread/count');
  }

  markAllAsRead(): Observable<void> {
    return this.http.put<void>('/api/notifications/mark-all-read', null);
  }
}
