export type OrderStatus =
  | 'CREATED'
  | 'EMAIL_SENT'
  | 'PENDING_PAYMENT'
  | 'PAID'
  | 'IN_DELIVERY'
  | 'DELIVERED'
  | 'CANCELLED';

export interface OrderResponse {
  id:               number;
  hawb:             string;
  mawb?:            string;
  shipmentId?:      number;
  clientId?:        number;
  clientFullName?:  string;
  clientEmail?:     string;
  clientPhone?:     string;
  clientAddress?:   string;
  clientCity?:      string;
  clientState?:     string;
  clientZipCode?:   string;
  clientCountry?:   string;
  companyName?:     string;
  goodsDescription?:   string;
  htsusCode?:          string;
  alternateReference?: string;
  numberOfItems?:  number;
  shipmentWeight?: number;
  grossWeight?:    number;
  customsValue?:   number;
  customsCurrency?: string;
  dutyRate?:       number;
  dutyAmount?:     number;
  bankCharges?:    number;
  totalAmount?:    number;
  enteredValue?:   number;
  status:          OrderStatus;
  paymentToken?:   string;
  tokenExpiresAt?: string;
  tokenValid?:     boolean;
  emailSentAt?:    string;
  emailSentCount?: number;
  emailOpened?:    boolean;
  createdAt?:      string;
  updatedAt?:      string;
}

export interface OrderRequest {
  hawb:             string;
  shipmentId:       number;
  clientId:         number;
  numberOfItems?:   number;
  goodsDescription?: string;
  shipmentWeight?:  number;
  htsusCode?:       string;
  customsValue?:    number;
  customsCurrency?: string;
  dutyRate?:        number;
  bankCharges?:     number;
}

export interface OrderPatch {
  hawb?:            string;
  clientId?:        number;
  numberOfItems?:   number;
  goodsDescription?: string;
  shipmentWeight?:  number;
  htsusCode?:       string;
  customsValue?:    number;
  customsCurrency?: string;
  dutyRate?:        number;
  bankCharges?:     number;
}

export interface OrderStatusUpdate {
  newStatus: OrderStatus;
  note?:     string;
}

export interface OrderSearchParams {
  q?:          string;
  status?:     OrderStatus | '';
  shipmentId?: number;
  from?:       string;
  to?:         string;
}

export interface BulkEmailResult {
  total:  number;
  sent:   number;
  failed: number;
}
