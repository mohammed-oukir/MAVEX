import { PaymentGatewayType, PaymentGatewayMode, GatewayConfigResponse, GatewayConfigRequest } from './payment.model';

export type { PaymentGatewayType, PaymentGatewayMode, GatewayConfigResponse, GatewayConfigRequest };

export interface ManualPaymentConfigResponse {
  ribFileName: string;
  uploadedAt: string;
}
