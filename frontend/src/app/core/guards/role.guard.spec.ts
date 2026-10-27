import { TestBed } from '@angular/core/testing';
import { Router, RouterStateSnapshot, ActivatedRouteSnapshot } from '@angular/router';
import { roleGuard } from './role.guard';
import { AuthService } from '../services/auth.service';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('roleGuard', () => {
  let authServiceMock: { isAuthenticated: any; role: any };
  let routerMock: { createUrlTree: any };

  beforeEach(() => {
    authServiceMock = {
      isAuthenticated: vi.fn(),
      role: vi.fn()
    };
    routerMock = {
      createUrlTree: vi.fn((path, extras) => ({ path, extras }))
    };

    TestBed.configureTestingModule({
      providers: [
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock }
      ]
    });
  });

  it('should grant access if user possesses required role', () => {
    authServiceMock.isAuthenticated.mockReturnValue(true);
    authServiceMock.role.mockReturnValue('ROLE_ADMIN');

    const guardFn = roleGuard('ROLE_ADMIN');
    const result = TestBed.runInInjectionContext(() =>
      guardFn({} as ActivatedRouteSnapshot, { url: '/admin/users' } as RouterStateSnapshot)
    );

    expect(result).toBe(true);
  });

  it('should redirect to /dashboard if user lacks required role', () => {
    authServiceMock.isAuthenticated.mockReturnValue(true);
    authServiceMock.role.mockReturnValue('ROLE_USUARIO');

    const guardFn = roleGuard('ROLE_ADMIN');
    const result = TestBed.runInInjectionContext(() =>
      guardFn({} as ActivatedRouteSnapshot, { url: '/admin/users' } as RouterStateSnapshot)
    );

    expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/dashboard']);
    expect(result).toEqual({ path: ['/dashboard'], extras: undefined });
  });

  it('should redirect to /login if user is unauthenticated', () => {
    authServiceMock.isAuthenticated.mockReturnValue(false);

    const guardFn = roleGuard('ROLE_ADMIN');
    const result = TestBed.runInInjectionContext(() =>
      guardFn({} as ActivatedRouteSnapshot, { url: '/admin/users' } as RouterStateSnapshot)
    );

    expect(routerMock.createUrlTree).toHaveBeenCalledWith(['/login'], {
      queryParams: { returnUrl: '/admin/users' }
    });
  });
});
