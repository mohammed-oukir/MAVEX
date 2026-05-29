export type ImportStatus = 'SUCCESS' | 'PARTIAL' | 'FAILED' | 'SKIPPED';

export interface ImportLogResponse {
  id: number;
  fileName: string;
  mawb?: string;
  totalRows: number;
  successRows?: number;
  skippedRows?: number;
  failedRows?: number;
  status: ImportStatus;
  importedBy?: string;
  importedAt: string;
}
