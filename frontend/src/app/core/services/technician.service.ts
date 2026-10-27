import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import {
  CreateTechnicianRequest,
  TechnicianResponse,
  UpdateTechnicianRequest
} from '../models/technician.models';
import { Category } from '../models/ticket.models';
import { UserStatus } from '../models/user.models';
import { PageResponse } from '../models/common.models';

export interface TechnicianSearchFilters {
  status?: UserStatus | '';
  specialty?: Category | '';
  page?: number;
  size?: number;
  sort?: string;
}

@Injectable({
  providedIn: 'root'
})
export class TechnicianService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(filters: TechnicianSearchFilters = {}): Observable<PageResponse<TechnicianResponse>> {
    let params = new HttpParams();

    if (filters.status) params = params.set('status', filters.status);
    if (filters.specialty) params = params.set('specialty', filters.specialty);
    if (filters.page !== undefined) params = params.set('page', String(filters.page));
    if (filters.size !== undefined) params = params.set('size', String(filters.size));
    if (filters.sort) params = params.set('sort', filters.sort);

    return this.http.get<PageResponse<TechnicianResponse>>(`${this.baseUrl}/technicians`, { params });
  }

  get(id: number): Observable<TechnicianResponse> {
    return this.http.get<TechnicianResponse>(`${this.baseUrl}/technicians/${id}`);
  }

  create(request: CreateTechnicianRequest): Observable<TechnicianResponse> {
    return this.http.post<TechnicianResponse>(`${this.baseUrl}/technicians`, request);
  }

  update(id: number, request: UpdateTechnicianRequest): Observable<TechnicianResponse> {
    return this.http.put<TechnicianResponse>(`${this.baseUrl}/technicians/${id}`, request);
  }
}
