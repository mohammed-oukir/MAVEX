export type OrderStatus =
  | 'CREATED'
  | 'EMAIL_SENT'
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'IN_DELIVERY'
  | 'DELIVERED';

export interface OrderResponse {
  id: number;
  hawb: string;
  mawb?: string;
  clientFullName?: string;
  clientEmail?: string;
  goodsDescription?: string;
  shipmentWeight?: number;
  customsValue?: number;
  customsCurrency?: string;
  totalAmount?: number;
  dutyAmount?: number;
  bankCharges?: number;
  status: OrderStatus;
  shipmentId?: number;
  clientId?: number;
  itemCount?: number;
  htsusCode?: string;
  dutyRate?: number;
  senderName?: string;
  receiverName?: string;
  country?: string;
  phone?: string;
  createdAt?: string;
}

export interface OrderRequest {
  hawb: string;
  shipmentId: number;
  clientId: number;
  itemCount?: number;
  goodsDescription?: string;
  shipmentWeight?: number;
  htsusCode?: string;
  customsValue?: number;
  customsCurrency?: string;
  dutyRate?: number;
  bankCharges?: number;
}

export interface OrderPatch {
  hawb?: string;
  itemCount?: number;
  goodsDescription?: string;
  shipmentWeight?: number;
  htsusCode?: string;
  customsValue?: number;
  customsCurrency?: string;
  dutyRate?: number;
  bankCharges?: number;
}
