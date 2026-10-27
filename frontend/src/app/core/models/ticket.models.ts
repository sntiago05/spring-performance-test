export type Category = 'HARDWARE' | 'SOFTWARE' | 'RED' | 'ACCESOS' | 'OTRO';
export type Priority = 'BAJA' | 'MEDIA' | 'ALTA' | 'CRITICA';
export type TicketStatus = 'ABIERTO' | 'ASIGNADO' | 'EN_PROGRESO' | 'RESUELTO' | 'CERRADO' | 'CANCELADO';
export type AssignmentSource = 'DISPATCH' | 'RULES' | 'MANUAL';
export type Availability = 'ALTA' | 'MEDIA' | 'BAJA';
export type HistoryAction = 'CREATED' | 'ASSIGNED' | 'REASSIGNED' | 'ASSIGNMENT_PENDING' | 'STATUS_CHANGED' | 'ESCALATED' | 'SLA_BREACHED';

export interface UserRef {
  id: number;
  fullName: string;
  email: string;
}

export interface TechnicianRef {
  id: number;
  code: string;
  fullName: string;
  specialty: Category;
}

export interface TicketResponse {
  id: number;
  title: string;
  description: string;
  category: Category;
  priority: Priority;
  site: string;
  affectedAsset: string;
  status: TicketStatus;
  createdAt: string;
  slaDueAt: string;
  slaBreached: boolean;
  escalated: boolean;
  escalatedAt: string | null;
  firstResponseAt: string | null;
  resolvedAt: string | null;
  requester: UserRef;
  technician: TechnicianRef | null;
}

export interface CreateTicketRequest {
  title: string;
  description: string;
  category: Category;
  priority: Priority;
  site: string;
  affectedAsset: string;
}

export interface ChangeStatusRequest {
  status: TicketStatus;
  observations: string;
}

export interface ReassignRequest {
  technicianId: number;
  observations: string;
}

export interface HistoryResponse {
  id: number;
  action: HistoryAction;
  fromStatus: TicketStatus | null;
  toStatus: TicketStatus | null;
  performedBy: string;
  observations: string | null;
  occurredAt: string;
}

export interface AssignmentResponse {
  id: number;
  technicianId: number;
  source: AssignmentSource;
  estimatedHours: number | null;
  availability: Availability | null;
  withinSla: boolean | null;
  assignedBy: string;
  assignedAt: string;
  active: boolean;
}

export interface TicketSearchFilters {
  status?: TicketStatus | '';
  priority?: Priority | '';
  category?: Category | '';
  overdue?: boolean;
  requesterId?: number | null;
  technicianId?: number | null;
  page?: number;
  size?: number;
  sort?: string;
}
