import { HttpBackend, HttpClient, HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { ApiResponse } from '../models/api.model';
import { AuthTokens } from '../models/auth.model';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth    = inject(AuthService);
  const backend = inject(HttpBackend);

  // Routes auth qui n'ont pas besoin de token — les exclure de l'injection Bearer.
  // /api/auth/me/* nécessite un token valide et ne doit PAS être exclu.
  const publicAuthPaths = ['/api/auth/login', '/api/auth/refresh', '/api/auth/forgot-password', '/api/auth/reset-password', '/api/auth/logout'];
  if (publicAuthPaths.some(p => req.url.includes(p))) {
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
