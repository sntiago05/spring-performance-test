import { UserResponse } from './user.models';

export interface LoginRequest {
  email: string;
  password: string;
}

export interface LoginResponse {
  accessToken: string;
  tokenType: string;
  expiresAt: string;
  user: UserResponse;
}

export interface RegisterRequest {
  document: string;
  fullName: string;
  email: string;
  password: string;
  site: string;
  area: string;
}
