export type ShipmentStatus = 'DRAFT' | 'IMPORTED' | 'PROCESSING' | 'CLOSED';

export interface ShipperSummary {
  id: number;
  companyName: string;
}

export interface ShipmentResponse {
  id: number;
  mawb: string;
  shipper?: ShipperSummary;
  exportDate?: string;
  importDate?: string;
  importingCarrier?: string;
  modeOfTransport?: string;
  portCode?: string;
  status: ShipmentStatus;
  createdBy?: string;
  dutyRate?: number;
  totalOrders?: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface ShipmentRequest {
  mawb: string;
  shipperId?: number;
  exportDate?: string;
  importDate?: string;
  importingCarrier?: string;
  modeOfTransport?: string;
  portCode?: string;
  status?: ShipmentStatus;
  dutyRate?: number;
}

export interface ShipmentPatch {
  mawb?: string;
  shipperId?: number | null;
  exportDate?: string;
  importDate?: string;
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
