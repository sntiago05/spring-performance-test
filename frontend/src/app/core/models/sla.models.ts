import { Priority } from './ticket.models';

export interface SlaPolicyResponse {
  priority: Priority;
  resolutionHours: number;
  escalationThresholdPercent: number;
}

export interface UpdateSlaPolicyRequest {
  resolutionHours: number;
  escalationThresholdPercent: number;
}

export interface EscalationSummaryResponse {
  reviewed: number;
  escalated: number;
  breached: number;
  failed: number;
}
