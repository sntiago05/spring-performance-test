import { ComponentFixture, TestBed } from '@angular/core/testing';
import { LoginComponent } from './login.component';
import { AuthService } from '../../../core/services/auth.service';
import { Router, ActivatedRoute } from '@angular/router';
import { of, throwError } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { describe, it, expect, beforeEach, vi } from 'vitest';

describe('LoginComponent', () => {
  let component: LoginComponent;
  let fixture: ComponentFixture<LoginComponent>;
  let authServiceMock: { isAuthenticated: any; login: any };
  let routerMock: { navigate: any; navigateByUrl: any };

  beforeEach(async () => {
    authServiceMock = {
      isAuthenticated: vi.fn().mockReturnValue(false),
      login: vi.fn()
    };
    routerMock = {
      navigate: vi.fn(),
      navigateByUrl: vi.fn()
    };

    await TestBed.configureTestingModule({
      imports: [LoginComponent],
      providers: [
        provideNoopAnimations(),
        { provide: AuthService, useValue: authServiceMock },
        { provide: Router, useValue: routerMock },
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParams: { returnUrl: '/tickets' }
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(LoginComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create and initialize an invalid form', () => {
    expect(component).toBeTruthy();
    expect(component.loginForm.valid).toBe(false);
  });

  it('should validate corporate email input properly', () => {
    const emailControl = component.loginForm.get('email');
    emailControl?.setValue('invalid-email');
    expect(emailControl?.hasError('email')).toBe(true);

    emailControl?.setValue('valid.user@helpflow.example');
    expect(emailControl?.valid).toBe(true);
  });

  it('should call authService.login on valid submission and navigate', () => {
    authServiceMock.login.mockReturnValue(of({
      accessToken: 'token-123',
      expiresAt: '2026-10-27T20:00:00Z',
      user: { id: 1 }
    }));

    component.loginForm.patchValue({
      email: 'user@helpflow.example',
      password: 'password123'
    });

    component.onSubmit();

    expect(authServiceMock.login).toHaveBeenCalledWith({
      email: 'user@helpflow.example',
      password: 'password123'
    });
    expect(routerMock.navigateByUrl).toHaveBeenCalledWith('/tickets');
  });

  it('should handle login error and display problem alert', () => {
    const mockProblem = {
      type: 'urn:helpflow:problem:bad-credentials',
      title: 'Bad credentials',
      status: 401,
      detail: 'The email or password is incorrect.'
    };
    authServiceMock.login.mockReturnValue(throwError(() => mockProblem));

    component.loginForm.patchValue({
      email: 'wrong@helpflow.example',
      password: 'wrongpassword'
    });

    component.onSubmit();

    expect(component.problem()).toEqual(mockProblem);
    expect(component.isSubmitting()).toBe(false);
  });
});
