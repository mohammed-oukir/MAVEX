export type ImportStatus     = 'SUCCESS' | 'PARTIAL' | 'FAILED' | 'SKIPPED';
export type ImportRowStatus  = 'IMPORTED' | 'SKIPPED' | 'FAILED';
export type PreviewRowStatus = 'VALID' | 'INVALID' | 'SKIPPED';

export interface ImportRowDetail {
  rowNumber:      number;
  hawb?:          string;
  receiverEmail?: string;
  status:         ImportRowStatus;
  reason?:        string;
  warnings?:      string;
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

/* ── Preview (analyse sans import) ─────────────────────────── */

export interface ImportPreviewRow {
  rowNumber:           number;
  previewStatus:       PreviewRowStatus;
  errors:              string[];
  warnings:            string[];
  alreadyExists:       boolean;
  mawb?:               string;
  hawb?:               string;
  alternateReference?: string;
  senderName?:         string;
  senderCountry?:      string;
  senderAddress?:      string;
  senderCity?:         string;
  senderState?:        string;
  senderPostcode?:     string;
  senderContact?:      string;
  senderPhone?:        string;
  senderEmail?:        string;
  receiverName?:       string;
  receiverCountry?:    string;
  receiverAddress?:    string;
  receiverCity?:       string;
  receiverState?:      string;
  receiverPostcode?:   string;
  receiverContact?:    string;
  receiverPhone?:      string;
  receiverEmail?:      string;
  numberOfItems?:      number;
  goodsDescription?:   string;
  shipmentWeight?:     number;
  customsValue?:       number;
  customsCurrency?:    string;
}

export interface ImportPreviewResponse {
  fileName:            string;
  fileHash:            string;
  mawb?:               string;
  totalRows:           number;
  validRows:           number;
  invalidRows:         number;
  skippedRows:         number;
  alreadyImported:     boolean;
  previousImportDate?: string;
  rows:                ImportPreviewRow[];
}

/* ── Confirm (après correction éventuelle) ──────────────────── */

export interface ImportConfirmRequest {
  fileName: string;
  fileHash: string;
  rows:     ImportPreviewRow[];
}

/* ── Revalidate (relecture complète après correction) ──────── */

export interface ImportRevalidateRequest {
  rows: ImportPreviewRow[];
}

/* ── Stats (KPI) ─────────────────────────────────────────────── */

export interface ImportStatsResponse {
  importsThisMonth:    number;
  successRatePercent:  number;
  failedRowsRecent:    number;
  totalImportsRecent:  number;
}
