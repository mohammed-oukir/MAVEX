export interface ApiResponse<T> {
  message: string;
  data: T;
  timestamp?: string;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
