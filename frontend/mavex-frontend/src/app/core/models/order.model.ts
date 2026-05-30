export type OrderStatus =
  | 'CREATED'
  | 'EMAIL_SENT'
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'IN_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED';

export interface OrderResponse {
  id: number;
  hawb: string;
  mawb?: string;
  clientId?: number;
  clientFullName?: string;
  clientEmail?: string;
  clientPhone?: string;
  clientAddress?: string;
  clientCity?: string;
  clientCountry?: string;
  companyName?: string;
  goodsDescription?: string;
  htsusCode?: string;
  numberOfItems?: number;
  shipmentWeight?: number;
  customsValue?: number;
  customsCurrency?: string;
  dutyRate?: number;
  dutyAmount?: number;
  bankCharges?: number;
  totalAmount?: number;
  status: OrderStatus;
  shipmentId?: number;
  createdAt?: string;
}

export interface OrderRequest {
  hawb: string;
  shipmentId: number;
  clientId: number;
  numberOfItems?: number;
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
  clientId?: number;
  numberOfItems?: number;
  goodsDescription?: string;
  shipmentWeight?: number;
  htsusCode?: string;
  customsValue?: number;
  customsCurrency?: string;
  dutyRate?: number;
  bankCharges?: number;
}

export interface OrderStatusUpdate {
  newStatus: OrderStatus;
  note?: string;
}
