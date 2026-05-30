export interface DashboardKpi {
  totalShipments:    number;
  activeShipments:   number;
  totalOrders:       number;
  paidOrders:        number;
  pendingOrders:     number;
  totalPaidAmount:   number;
  totalPendingAmount: number;
}

export interface StatusCount {
  status: string;
  count:  number;
}

export interface MonthlyCount {
  month: string;
  count: number;
}

export interface MonthlyRevenue {
  month:  string;
  amount: number;
}

export interface DashboardAlerts {
  ordersNeverEmailed: number;
  stuckShipments:     number;
  overduePayments:    number;
  hasAlerts:          boolean;
}
