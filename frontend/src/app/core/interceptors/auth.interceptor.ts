import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { I18nService } from '../i18n/i18n.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const i18n = inject(I18nService);

  const token = authService.token();
  const lang = i18n.currentLang();

  let headers = req.headers.set('Accept-Language', lang);

  // Skip auth header for authentication endpoints
  const isAuthEndpoint = req.url.includes('/auth/login') || req.url.includes('/auth/register');
  if (token && !isAuthEndpoint) {
    headers = headers.set('Authorization', `Bearer ${token}`);
  }

  const modifiedReq = req.clone({ headers });
  return next(modifiedReq);
};
