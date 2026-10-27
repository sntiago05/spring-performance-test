import { UserStatus } from './user.models';
import { Category } from './ticket.models';

export interface TechnicianResponse {
  id: number;
  code: string;
  userId: number | null;
  fullName: string;
  specialty: Category;
  site: string;
  maxActiveTickets: number;
  status: UserStatus;
}

export interface CreateTechnicianRequest {
  document: string;
  fullName: string;
  email: string;
  password: string;
  site: string;
  area: string;
  specialty: Category;
  maxActiveTickets: number;
}

export interface UpdateTechnicianRequest {
  fullName: string;
  specialty: Category;
  site: string;
  maxActiveTickets: number;
  status: UserStatus;
}
