import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { UserResponse, UserRequest } from '../models/user.model';
import { ApiResponse } from '../models/api.model';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<UserResponse[]> {
    return this.http.get<UserResponse[]>('/api/users');
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
