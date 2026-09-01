import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserResponse, UserRequest, UserSearchCriteria, UserStats } from '../models/user.model';
import { ApiResponse, Page } from '../models/api.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>('/api/users');
  }

  search(criteria: UserSearchCriteria = {}, page = 0, size = 20): Observable<Page<UserResponse>> {
    let p = new HttpParams()
      .set('page', page)
      .set('size', size)
      .set('sort', 'createdAt,desc');
    if (criteria.fullName) p = p.set('fullName', criteria.fullName);
    if (criteria.email)    p = p.set('email', criteria.email);
    if (criteria.role)     p = p.set('role', criteria.role);
    if (criteria.status && criteria.status !== 'all') p = p.set('status', criteria.status);
    return this.http.get<Page<UserResponse>>('/api/users/search', { params: p });
  }

  getStats(): Observable<UserStats> {
    return this.http.get<UserStats>('/api/users/stats');
  }

  getById(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`/api/users/${id}`);
  }

  create(req: UserRequest): Observable<ApiResponse<UserResponse>> {
    return this.http.post<ApiResponse<UserResponse>>('/api/users', req);
  }

  patch(id: number, req: Partial<UserRequest>): Observable<ApiResponse<UserResponse>> {
    return this.http.patch<ApiResponse<UserResponse>>(`/api/users/${id}`, req);
  }

  delete(id: number): Observable<ApiResponse<void>> {
    return this.http.delete<ApiResponse<void>>(`/api/users/${id}`);
  }

  activate(id: number): Observable<void> {
    return this.http.patch<void>(`/api/users/${id}/activate`, {});
  }
}
