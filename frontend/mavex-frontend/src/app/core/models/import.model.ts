export type ImportStatus    = 'SUCCESS' | 'PARTIAL' | 'FAILED' | 'SKIPPED';
export type ImportRowStatus = 'IMPORTED' | 'SKIPPED' | 'FAILED';

export interface ImportRowDetail {
  rowNumber:     number;
  hawb?:         string;
  receiverEmail?: string;
  status:        ImportRowStatus;
  reason?:       string;
  warnings?:     string;
}

export interface ImportLogResponse {
  id:           number;
  fileName:     string;
  mawb?:        string;
  shipmentId?:  number;
  totalRows:    number;
  successRows?: number;
  skippedRows?: number;
  failedRows?:  number;
  status:       ImportStatus;
  importedBy?:  string;
  importedAt:   string;
  rows?:        ImportRowDetail[];
}
