export interface LoginRequest {
  email: string;
  password: string;
}

export interface AuthTokens {
  accessToken: string;
  refreshToken: string;
}

export type PermissionAction = 'VIEW' | 'CREATE' | 'EDIT' | 'DELETE';
export type PermissionModule =
  | 'DASHBOARD' | 'SHIPMENTS' | 'ORDERS'
  | 'CLIENTS'  | 'IMPORTS'   | 'SHIPPERS' | 'AIRLINES';

export type PermissionsMap = Record<PermissionModule, PermissionAction[]>;

export interface LoginResponse extends AuthTokens {
  userId: number;
  email: string;
  fullName: string;
  role: string;
  permissions: PermissionsMap;
}

export interface ForgotPasswordRequest {
  email: string;
}

export interface ResetPasswordRequest {
  email: string;
  otp: string;
  newPassword: string;
}
