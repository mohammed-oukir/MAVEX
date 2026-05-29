export type ShipmentStatus = 'DRAFT' | 'IMPORTED' | 'PROCESSING' | 'CLOSED';

export interface ShipperSummary {
  id: number;
  companyName: string;
}

export interface ShipmentResponse {
  id: number;
  mawb: string;
  shipper?: ShipperSummary;
  importingCarrier?: string;
  modeOfTransport?: string;
  portCode?: string;
  status: ShipmentStatus;
  createdBy?: string;
  dutyRate?: number;
  totalOrders?: number;
  createdAt?: string;
}

export interface ShipmentRequest {
  mawb: string;
  shipperId?: number;
  importingCarrier?: string;
  modeOfTransport?: string;
  portCode?: string;
  status?: ShipmentStatus;
  dutyRate?: number;
}

export interface ShipmentPatch {
  importingCarrier?: string;
  modeOfTransport?: string;
  portCode?: string;
  status?: ShipmentStatus;
}

export interface ShipmentStatusUpdate {
  status: ShipmentStatus;
}

export interface DutyRateUpdate {
  dutyRate: number;
}
