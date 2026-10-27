import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { MatSnackBar } from '@angular/material/snack-bar';
import { errorInterceptor } from './error.interceptor';
import { ProblemDetail } from '../models/problem-detail.models';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

describe('errorInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let routerMock: { url: string; navigate: any };
  let snackBarMock: { open: any };

  beforeEach(() => {
    routerMock = {
      url: '/tickets/12',
      navigate: vi.fn()
    };
    snackBarMock = {
      open: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([errorInterceptor])),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerMock },
        { provide: MatSnackBar, useValue: snackBarMock }
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    vi.restoreAllMocks();
  });

  it('should redirect to /login with returnUrl on 401 error', () => {
    httpClient.get('/api/tickets/12').subscribe({
      next: () => expect.fail('should have failed'),
      error: (err: ProblemDetail) => {
        expect(err.status).toBe(401);
      }
    });

    const req = httpMock.expectOne('/api/tickets/12');
    req.flush(
      { title: 'Authentication required', status: 401, detail: 'Token expired' },
      { status: 401, statusText: 'Unauthorized' }
    );

    expect(routerMock.navigate).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/tickets/12' }
    });
  });

  it('should trigger snackbar alert with traceId on 500 internal server error', () => {
    httpClient.get('/api/tickets').subscribe({
      next: () => expect.fail('should have failed'),
      error: (err: ProblemDetail) => {
        expect(err.status).toBe(500);
        expect(err.traceId).toBe('trace-999');
      }
    });

    const req = httpMock.expectOne('/api/tickets');
    req.flush(
      {
        type: 'urn:helpflow:problem:internal-error',
        title: 'Internal Error',
        status: 500,
        detail: 'DB failure',
        traceId: 'trace-999'
      },
      { status: 500, statusText: 'Internal Server Error' }
    );

    expect(snackBarMock.open).toHaveBeenCalledWith(
      expect.stringContaining('trace-999'),
      'Dismiss',
      expect.anything()
    );
  });
});
