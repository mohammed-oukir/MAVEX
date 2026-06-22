export interface ExchangeRate {
  id:             number;
  fromCurrency:   string;
  toCurrency:     string;
  rate:           number;
  effectiveDate:  string;
  source:         string | null;
  isActive:       boolean;
  importedByName: string | null;
  importedAt:     string | null;
  createdAt:      string | null;
  updatedAt:      string | null;
}

export interface ExchangeRateRequest {
  fromCurrency:  string;
  toCurrency:    string;
  rate:          number;
  effectiveDate: string;
  source?:       string;
}

export interface ExchangeRateImportResult {
  imported:     number;
  skipped:      number;
  errors:       number;
  errorDetails: string[];
}

export interface ExchangeRateLookup {
  fromCurrency:  string;
  toCurrency:    string;
  rate:          number;
  effectiveDate: string;
  isActive:      boolean;
}
