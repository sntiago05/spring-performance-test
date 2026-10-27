import { TestBed } from '@angular/core/testing';
import { provideHttpClient } from '@angular/common/http';
import { provideHttpClientTesting, HttpTestingController } from '@angular/common/http/testing';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';
import { API_BASE_URL } from '../config/api.config';
import { LoginResponse } from '../models/auth.models';
import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';

describe('AuthService', () => {
  let service: AuthService;
  let httpMock: HttpTestingController;
  let routerMock: { navigateByUrl: any };

  beforeEach(() => {
    sessionStorage.clear();
    routerMock = {
      navigateByUrl: vi.fn()
    };

    TestBed.configureTestingModule({
      providers: [
        AuthService,
        provideHttpClient(),
        provideHttpClientTesting(),
        { provide: Router, useValue: routerMock },
        { provide: API_BASE_URL, useValue: '/api' }
      ]
    });

    service = TestBed.inject(AuthService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
    sessionStorage.clear();
    vi.restoreAllMocks();
  });

  it('should initialize unauthenticated when sessionStorage is empty', () => {
    expect(service.isAuthenticated()).toBe(false);
    expect(service.token()).toBeNull();
    expect(service.user()).toBeNull();
  });

  it('should authenticate user and store token upon successful login', () => {
    const futureDate = new Date(Date.now() + 3600 * 1000).toISOString();
    const mockResponse: LoginResponse = {
      accessToken: 'jwt-test-token-123',
      tokenType: 'Bearer',
      expiresAt: futureDate,
      user: {
        id: 1,
        document: '12345678',
        fullName: 'Admin User',
        email: 'admin@helpflow.example',
        site: 'BOGOTA',
        area: 'IT',
        status: 'ACTIVO',
        role: 'ROLE_ADMIN'
      }
    };

    service.login({ email: 'admin@helpflow.example', password: 'password' }).subscribe(res => {
      expect(res).toEqual(mockResponse);
    });

    const req = httpMock.expectOne('/api/auth/login');
    expect(req.request.method).toBe('POST');
    req.flush(mockResponse);

    expect(service.isAuthenticated()).toBe(true);
    expect(service.token()).toBe('jwt-test-token-123');
    expect(service.role()).toBe('ROLE_ADMIN');
    expect(service.isAdmin()).toBe(true);
    expect(sessionStorage.getItem('helpflow_token')).toBe('jwt-test-token-123');
  });

  it('should clear session and navigate to /login upon logout', () => {
    sessionStorage.setItem('helpflow_token', 'test-token');
    sessionStorage.setItem('helpflow_expires_at', new Date(Date.now() + 10000).toISOString());
    sessionStorage.setItem('helpflow_user', JSON.stringify({ id: 1, role: 'ROLE_USUARIO' }));

    service.logout();

    expect(service.isAuthenticated()).toBe(false);
    expect(service.token()).toBeNull();
    expect(sessionStorage.getItem('helpflow_token')).toBeNull();
    expect(routerMock.navigateByUrl).toHaveBeenCalledWith('/login');
  });

  it('should identify expired tokens correctly', () => {
    const pastDate = new Date(Date.now() - 10000).toISOString();
    service.expiresAt.set(pastDate);
    expect(service.isExpired()).toBe(true);
  });
});
