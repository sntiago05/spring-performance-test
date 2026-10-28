import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { catchError, throwError } from 'rxjs';
import { ProblemDetail } from '../models/problem-detail.models';
import { I18nService } from '../i18n/i18n.service';

export interface FallbackTexts {
  httpError: string;
  unexpectedError: string;
}

const DEFAULT_TEXTS: FallbackTexts = { httpError: 'HTTP Error', unexpectedError: 'Unexpected network or server error' };

export function normalizeProblemDetail(errorResponse: HttpErrorResponse, texts: FallbackTexts = DEFAULT_TEXTS): ProblemDetail {
  const body = errorResponse.error;
  const traceIdHeader = errorResponse.headers?.get('X-Trace-Id') ?? undefined;

  if (body && typeof body === 'object' && 'title' in body && 'status' in body) {
    return {
      type: body.type || 'urn:helpflow:problem:error',
      title: body.title,
      status: body.status,
      detail: body.detail || '',
      instance: body.instance || errorResponse.url || undefined,
      timestamp: body.timestamp || new Date().toISOString(),
      traceId: body.traceId || traceIdHeader,
      errors: Array.isArray(body.errors) ? body.errors : undefined
    };
  }

  return {
    type: 'urn:helpflow:problem:http-error',
    title: errorResponse.statusText || texts.httpError,
    status: errorResponse.status || 500,
    detail: (typeof body === 'string' ? body : errorResponse.message) || texts.unexpectedError,
    instance: errorResponse.url || undefined,
    timestamp: new Date().toISOString(),
    traceId: traceIdHeader
  };
}

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const i18n = inject(I18nService);
  const router = inject(Router);
  const snackBar = inject(MatSnackBar);

  return next(req).pipe(
    catchError((error: unknown) => {
      if (error instanceof HttpErrorResponse) {
        const problem = normalizeProblemDetail(error, { httpError: i18n.t().ui.httpError, unexpectedError: i18n.t().ui.unexpectedError });

        // 401 Unauthorized: redirect to login preserving intended URL
        if (problem.status === 401 && !req.url.includes('/auth/login')) {
          const currentUrl = router.url;
          sessionStorage.removeItem('helpflow_token');
          sessionStorage.removeItem('helpflow_expires_at');
          sessionStorage.removeItem('helpflow_user');

          router.navigate(['/login'], {
            queryParams: currentUrl && currentUrl !== '/login' ? { returnUrl: currentUrl } : {}
          });
        } else if (problem.status === 403 || problem.status >= 500) {
          // 403 Forbidden or 5xx Server Error: show alert snackbar with traceId
          const traceMsg = problem.traceId ? ` [Trace: ${problem.traceId}]` : '';
          const message = `${problem.title}: ${problem.detail}${traceMsg}`;
          snackBar.open(message, 'Dismiss', {
            duration: 7000,
            panelClass: problem.status >= 500 ? ['snackbar-error'] : ['snackbar-warning'],
            horizontalPosition: 'right',
            verticalPosition: 'top'
          });
        }

        return throwError(() => problem);
      }

      return throwError(() => error);
    })
  );
};
