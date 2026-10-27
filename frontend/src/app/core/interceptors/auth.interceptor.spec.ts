import { TestBed } from '@angular/core/testing';
import { HttpClient, provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { authInterceptor } from './auth.interceptor';
import { AuthService } from '../services/auth.service';
import { I18nService } from '../i18n/i18n.service';
import { describe, it, expect, beforeEach, afterEach } from 'vitest';

describe('authInterceptor', () => {
  let httpClient: HttpClient;
  let httpMock: HttpTestingController;
  let authService: AuthService;
  let i18nService: I18nService;

  beforeEach(() => {
    TestBed.configureTestingModule({
      providers: [
        provideHttpClient(withInterceptors([authInterceptor])),
        provideHttpClientTesting()
      ]
    });

    httpClient = TestBed.inject(HttpClient);
    httpMock = TestBed.inject(HttpTestingController);
    authService = TestBed.inject(AuthService);
    i18nService = TestBed.inject(I18nService);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should attach Bearer token and Accept-Language to authenticated requests', () => {
    authService.token.set('mock-access-token-999');
    i18nService.setLanguage('es');

    httpClient.get('/api/tickets').subscribe();

    const req = httpMock.expectOne('/api/tickets');
    expect(req.request.headers.get('Authorization')).toBe('Bearer mock-access-token-999');
    expect(req.request.headers.get('Accept-Language')).toBe('es');
    req.flush([]);
  });

  it('should not attach Authorization header to /auth/login', () => {
    authService.token.set('mock-access-token-999');

    httpClient.post('/api/auth/login', {}).subscribe();

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.headers.has('Authorization')).toBe(false);
    expect(req.request.headers.get('Accept-Language')).toBeDefined();
    req.flush({});
  });
});
