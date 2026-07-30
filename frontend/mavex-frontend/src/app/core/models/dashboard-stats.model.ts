export interface EmailStatsDay {
  date:      string;
  sent:      number;
  delivered: number;
  opened:    number;
  bounced:   number;
}

export interface EmailStatsSummary {
  totalSent:      number;
  totalDelivered: number;
  totalOpened:    number;
  totalBounced:   number;
  deliveryRate:   number;
  openRate:       number;
}

export interface EmailStatsHistoryResponse {
  days:    EmailStatsDay[];
  summary: EmailStatsSummary;
}

export interface BrevoQuotaResponse {
  remainingCredits: number | null;
  available:        boolean;
  errorMessage:     string | null;
}
