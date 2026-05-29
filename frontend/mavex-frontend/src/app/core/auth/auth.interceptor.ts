import { HttpBackend, HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { ApiResponse } from '../models/api.model';
import { AuthTokens } from '../models/auth.model';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth    = inject(AuthService);
  const backend = inject(HttpBackend);

  if (req.url.includes('/api/auth/')) {
    return next(req);
  }

  const token   = auth.getToken();
  const authReq = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(authReq).pipe(
    catchError((error: HttpErrorResponse) => {
      if (error.status === 401 && !auth.isRefreshing) {
        const refreshToken = auth.getRefreshToken();
        if (refreshToken) {
          auth.isRefreshing = true;
          return new HttpClient(backend)
            .post<ApiResponse<AuthTokens>>('/api/auth/refresh', { refreshToken })
            .pipe(
              switchMap(res => {
                auth.isRefreshing = false;
                auth.updateTokens(res.data.accessToken, res.data.refreshToken);
                const retried = req.clone({
                  setHeaders: { Authorization: `Bearer ${res.data.accessToken}` },
                });
                return next(retried);
              }),
              catchError(err => {
                auth.isRefreshing = false;
                auth.logout();
                return throwError(() => err);
              }),
            );
        }
        auth.logout();
      }
      return throwError(() => error);
    }),
  );
};
