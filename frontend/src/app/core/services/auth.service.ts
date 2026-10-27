import { Injectable, computed, inject, signal } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';
import { Observable, tap } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { LoginRequest, LoginResponse, RegisterRequest } from '../models/auth.models';
import { Role, UserResponse } from '../models/user.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private readonly http = inject(HttpClient);
  private readonly router = inject(Router);
  private readonly baseUrl = inject(API_BASE_URL);

  private readonly TOKEN_KEY = 'helpflow_token';
  private readonly EXPIRES_AT_KEY = 'helpflow_expires_at';
  private readonly USER_KEY = 'helpflow_user';

  readonly user = signal<UserResponse | null>(null);
  readonly token = signal<string | null>(null);
  readonly expiresAt = signal<string | null>(null);

  readonly isAuthenticated = computed(() => !!this.token() && !this.isExpired());
  readonly role = computed<Role | null>(() => this.user()?.role ?? null);
  readonly isAdmin = computed(() => this.role() === 'ROLE_ADMIN');
  readonly isTechnician = computed(() => this.role() === 'ROLE_TECNICO');
  readonly isUser = computed(() => this.role() === 'ROLE_USUARIO');

  private expirationTimer: ReturnType<typeof setTimeout> | null = null;

  constructor() {
    this.restoreSession();
  }

  private restoreSession(): void {
    try {
      const storedToken = sessionStorage.getItem(this.TOKEN_KEY);
      const storedExpires = sessionStorage.getItem(this.EXPIRES_AT_KEY);
      const storedUser = sessionStorage.getItem(this.USER_KEY);

      if (storedToken && storedExpires && storedUser) {
        const expirationDate = new Date(storedExpires).getTime();
        if (expirationDate > Date.now()) {
          this.token.set(storedToken);
          this.expiresAt.set(storedExpires);
          this.user.set(JSON.parse(storedUser) as UserResponse);
          this.scheduleAutoLogout(expirationDate - Date.now());
        } else {
          this.clearSession();
        }
      }
    } catch {
      this.clearSession();
    }
  }

  isExpired(): boolean {
    const exp = this.expiresAt();
    if (!exp) return true;
    return new Date(exp).getTime() <= Date.now();
  }

  login(credentials: LoginRequest): Observable<LoginResponse> {
    return this.http.post<LoginResponse>(`${this.baseUrl}/auth/login`, credentials).pipe(
      tap(response => {
        this.saveSession(response);
      })
    );
  }

  register(data: RegisterRequest): Observable<UserResponse> {
    return this.http.post<UserResponse>(`${this.baseUrl}/auth/register`, data);
  }

  logout(redirectUrl: string = '/login'): void {
    this.clearSession();
    this.router.navigateByUrl(redirectUrl);
  }

  updateCurrentUser(updated: UserResponse): void {
    this.user.set(updated);
    try {
      sessionStorage.setItem(this.USER_KEY, JSON.stringify(updated));
    } catch {
      // Ignore storage errors
    }
  }

  private saveSession(response: LoginResponse): void {
    this.token.set(response.accessToken);
    this.expiresAt.set(response.expiresAt);
    this.user.set(response.user);

    try {
      sessionStorage.setItem(this.TOKEN_KEY, response.accessToken);
      sessionStorage.setItem(this.EXPIRES_AT_KEY, response.expiresAt);
      sessionStorage.setItem(this.USER_KEY, JSON.stringify(response.user));
    } catch {
      // Ignore storage error
    }

    const remainingMs = new Date(response.expiresAt).getTime() - Date.now();
    this.scheduleAutoLogout(remainingMs);
  }

  private clearSession(): void {
    if (this.expirationTimer) {
      clearTimeout(this.expirationTimer);
      this.expirationTimer = null;
    }
    this.token.set(null);
    this.expiresAt.set(null);
    this.user.set(null);

    try {
      sessionStorage.removeItem(this.TOKEN_KEY);
      sessionStorage.removeItem(this.EXPIRES_AT_KEY);
      sessionStorage.removeItem(this.USER_KEY);
    } catch {
      // Ignore
    }
  }

  private scheduleAutoLogout(ms: number): void {
    if (this.expirationTimer) {
      clearTimeout(this.expirationTimer);
    }
    if (ms <= 0) {
      this.logout();
      return;
    }
    this.expirationTimer = setTimeout(() => {
      this.logout();
    }, ms);
  }
}
