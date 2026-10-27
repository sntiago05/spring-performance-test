export type UserStatus = 'ACTIVO' | 'INACTIVO';
export type Role = 'ROLE_USUARIO' | 'ROLE_TECNICO' | 'ROLE_ADMIN';

export interface UserResponse {
  id: number;
  document: string;
  fullName: string;
  email: string;
  site: string;
  area: string;
  status: UserStatus;
  role: Role;
}

export interface UpdateProfileRequest {
  fullName: string;
  site: string;
  area: string;
}

export interface ChangeUserStatusRequest {
  status: UserStatus;
}
