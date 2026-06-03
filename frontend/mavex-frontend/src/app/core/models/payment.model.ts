export type PaymentGatewayType = 'STRIPE' | 'PAYPAL' | 'CMI';
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
  apiKeyMasked?: string;
  secretKeyMasked?: string;
  webhookSecretSet: boolean;
  mode: PaymentGatewayMode;
  active: boolean;
  supportedCurrencies?: string;
  description?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface GatewayConfigRequest {
  type: PaymentGatewayType;
  name: string;
  apiKey?: string;
  secretKey?: string;
  webhookSecret?: string;
  mode: PaymentGatewayMode;
  supportedCurrencies?: string;
  description?: string;
}
