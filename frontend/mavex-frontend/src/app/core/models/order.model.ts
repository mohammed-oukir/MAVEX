export type OrderStatus =
  | 'CREATED'
  | 'EMAIL_SENT'
  | 'EMAIL_OUTDATED'
  | 'PAID'
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
  emailSentAt?:          string;
  emailSentCount?:       number;
  emailSentToAddress?:   string;
  emailOutdatedReason?:  string;
  deliveredAt?:          string | null;
  openedAt?:             string | null;
  clickedAt?:            string | null;
  bouncedAt?:            string | null;
  bounceReason?:         string | null;
  createdAt?:            string;
  updatedAt?:            string;
  lockedExchangeRate?:   number | null;
  lockedToCurrency?:     string | null;
  lockedAmountMAD?:      number | null;
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
  hawb?:            string;
  client?:          string;
  clientEmail?:     string;
  shipmentSearch?:  string;
  shipmentWeight?:  number;
  customsValue?:    number;
  totalAmount?:     number;
  dutyRate?:        number;
  customsCurrency?: string;
  status?:          OrderStatus | '';
  shipmentId?:      number;
  from?:            string;
  to?:              string;
}

export interface BulkEmailResult {
  total:  number;
  sent:   number;
  failed: number;
}

export type EmailLogStatus = 'PENDING' | 'SENT' | 'FAILED' | 'RETRYING';

export interface EmailLogResponse {
  id:           number;
  toEmail:      string;
  subject?:     string;
  status:       EmailLogStatus;
  errorMessage?: string;
  retryCount:   number;
  sentAt?:      string;
  deliveredAt?: string | null;
  openedAt?:    string | null;
  clickedAt?:   string | null;
  bouncedAt?:   string | null;
  bounceReason?: string | null;
  sentByName?:  string | null;
}
