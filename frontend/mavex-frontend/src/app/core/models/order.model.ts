export type OrderStatus =
  | 'CREATED'
  | 'EMAIL_SENT'
  | 'EMAIL_OUTDATED'
  | 'PAID'
  | 'CANCELLED';

export interface OrderStatusOption {
  value: OrderStatus;
  label: string;
  color?: string;
  bg?: string;
}

/**
 * Source unique des statuts Order pour le frontend (libellés FR, couleurs).
 * Couleurs/bg repris de shipment-detail.component.ts (statusOptions).
 * Les 5 valeurs reflètent l'enum backend OrderStatus.java — toute évolution
 * de l'enum backend doit être répercutée ici manuellement.
 */
export const ORDER_STATUSES: ReadonlyArray<OrderStatusOption> = [
  { value: 'CREATED',        label: 'Créé',            color: '#6B7280', bg: '#F3F4F6' },
  { value: 'EMAIL_SENT',     label: 'Email envoyé',    color: '#3B82F6', bg: '#EFF6FF' },
  { value: 'EMAIL_OUTDATED', label: 'Email obsolète',  color: '#F97316', bg: '#FFF7ED' },
  { value: 'PAID',           label: 'Payé',            color: '#22C55E', bg: '#F0FDF4' },
  { value: 'CANCELLED',      label: 'Annulé',          color: '#EF4444', bg: '#FEF2F2' },
];

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

export interface BulkStatusResult {
  total:     number;
  succeeded: number;
  failed:    number;
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
