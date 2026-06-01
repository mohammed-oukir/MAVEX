export type UserRole = 'ADMIN' | 'AGENT';

export interface UserResponse {
  id: number;
  fullName: string;
  email: string;
  role: UserRole;
  active: boolean;
  createdAt?: string;
  updatedAt?: string;
}

export interface UserRequest {
  fullName: string;
  email: string;
  password?: string;
  role: UserRole;
}
