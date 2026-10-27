import { Injectable, inject } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import {
  AssignmentResponse,
  ChangeStatusRequest,
  CreateTicketRequest,
  HistoryResponse,
  ReassignRequest,
  TicketResponse,
  TicketSearchFilters
} from '../models/ticket.models';
import { PageResponse } from '../models/common.models';
import { EscalationSummaryResponse } from '../models/sla.models';

@Injectable({
  providedIn: 'root'
})
export class TicketService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(filters: TicketSearchFilters = {}): Observable<PageResponse<TicketResponse>> {
    let params = new HttpParams();

    if (filters.status) params = params.set('status', filters.status);
    if (filters.priority) params = params.set('priority', filters.priority);
    if (filters.category) params = params.set('category', filters.category);
    if (filters.overdue !== undefined && filters.overdue !== null) {
      params = params.set('overdue', String(filters.overdue));
    }
    if (filters.requesterId) params = params.set('requesterId', String(filters.requesterId));
    if (filters.technicianId) params = params.set('technicianId', String(filters.technicianId));
    if (filters.page !== undefined) params = params.set('page', String(filters.page));
    if (filters.size !== undefined) params = params.set('size', String(filters.size));
    if (filters.sort) params = params.set('sort', filters.sort);

    return this.http.get<PageResponse<TicketResponse>>(`${this.baseUrl}/tickets`, { params });
  }

  get(id: number): Observable<TicketResponse> {
    return this.http.get<TicketResponse>(`${this.baseUrl}/tickets/${id}`);
  }

  create(request: CreateTicketRequest): Observable<TicketResponse> {
    return this.http.post<TicketResponse>(`${this.baseUrl}/tickets`, request);
  }

  history(id: number): Observable<HistoryResponse[]> {
    return this.http.get<HistoryResponse[]>(`${this.baseUrl}/tickets/${id}/history`);
  }

  assignments(id: number): Observable<AssignmentResponse[]> {
    return this.http.get<AssignmentResponse[]>(`${this.baseUrl}/tickets/${id}/assignments`);
  }

  changeStatus(id: number, request: ChangeStatusRequest): Observable<TicketResponse> {
    return this.http.patch<TicketResponse>(`${this.baseUrl}/tickets/${id}/status`, request);
  }

  reassign(id: number, request: ReassignRequest): Observable<TicketResponse> {
    return this.http.put<TicketResponse>(`${this.baseUrl}/tickets/${id}/assignment`, request);
  }

  runEscalations(): Observable<EscalationSummaryResponse> {
    return this.http.post<EscalationSummaryResponse>(`${this.baseUrl}/tickets/escalations`, {});
  }
}
