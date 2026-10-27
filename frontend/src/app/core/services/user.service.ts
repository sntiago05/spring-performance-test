import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { ChangeUserStatusRequest, Role, UpdateProfileRequest, UserResponse, UserStatus } from '../models/user.models';
import { PageResponse } from '../models/common.models';

export interface UserSearchFilters {
  status?: UserStatus | '';
  role?: Role | '';
  site?: string;
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({
  providedIn: 'root'
})
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  me(): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/users/me`);
  }

  list(filters: UserSearchFilters = {}): Observable<PageResponse<UserResponse>> {
    let params = new HttpParams();

    if (filters.status) params = params.set('status', filters.status);
    if (filters.role) params = params.set('role', filters.role);
    if (filters.site) params = params.set('site', filters.site);
    if (filters.page !== undefined) params = params.set('page', String(filters.page));
    if (filters.size !== undefined) params = params.set('size', String(filters.size));
    if (filters.sort) params = params.set('sort', filters.sort);

    return this.http.get<PageResponse<UserResponse>>(`${this.baseUrl}/users`, { params });
  }

  get(id: number): Observable<UserResponse> {
    return this.http.get<UserResponse>(`${this.baseUrl}/users/${id}`);
  }

  updateProfile(id: number, request: UpdateProfileRequest): Observable<UserResponse> {
    return this.http.put<UserResponse>(`${this.baseUrl}/users/${id}`, request);
  }

  changeStatus(id: number, request: ChangeUserStatusRequest): Observable<UserResponse> {
    return this.http.patch<UserResponse>(`${this.baseUrl}/users/${id}/status`, request);
  }
}
