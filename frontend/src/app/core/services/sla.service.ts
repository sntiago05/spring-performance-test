import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { API_BASE_URL } from '../config/api.config';
import { Priority } from '../models/ticket.models';
import { SlaPolicyResponse, UpdateSlaPolicyRequest } from '../models/sla.models';

@Injectable({
  providedIn: 'root'
})
export class SlaService {
  private readonly http = inject(HttpClient);
  private readonly baseUrl = inject(API_BASE_URL);

  list(): Observable<SlaPolicyResponse[]> {
    return this.http.get<SlaPolicyResponse[]>(`${this.baseUrl}/sla-policies`);
  }

  update(priority: Priority, request: UpdateSlaPolicyRequest): Observable<SlaPolicyResponse> {
    return this.http.put<SlaPolicyResponse>(`${this.baseUrl}/sla-policies/${priority}`, request);
  }
}
