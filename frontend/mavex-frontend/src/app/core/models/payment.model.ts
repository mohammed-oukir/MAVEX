export type PaymentGatewayType = 'STRIPE' | 'PAYPAL' | 'CMI' | 'MANUEL';
export type PaymentGatewayMode = 'TEST' | 'PRODUCTION';
export type PaymentStatus = 'INITIATED' | 'SUCCESS' | 'FAILED' | 'CANCELLED' | 'REFUNDED';

export interface PaymentTransactionResponse {
  id: number;
  gatewayRef?: string;
  gateway: PaymentGatewayType;
  amount: number;
  currency: string;
  status: PaymentStatus;
  ipAddress?: string;
  paidAt?: string;
  createdAt?: string;
}

export interface GatewayConfigResponse {
  id: number;
  type: PaymentGatewayType;
  name: string;
  mode: PaymentGatewayMode;
  active: boolean;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface GatewayConfigRequest {
  type: PaymentGatewayType;
  name: string;
  mode: PaymentGatewayMode;
  description?: string;
}
