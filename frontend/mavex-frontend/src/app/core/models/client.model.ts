export interface ClientCountry {
  code: string;
  name?: string;
}

export interface ClientResponse {
  id: number;
  fullName: string;
  email?: string;
  phone?: string;
  address?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  country?: ClientCountry;
  createdAt?: string;
  active: boolean;
}

export interface ClientRequest {
  fullName: string;
  email: string;
  phone?: string;
  address?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  countryCode: string;
}

export interface ClientPatch {
  fullName?: string;
  email?: string;
  phone?: string;
  address?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  countryCode?: string;
  active?: boolean;
}

export interface ClientSearchCriteria {
  name?: string;
  email?: string;
  phone?: string;
  city?: string;
  state?: string;
  country?: string;
  status?: 'all' | 'active' | 'inactive';
  dateFrom?: string; // YYYY-MM-DD
  dateTo?: string;   // YYYY-MM-DD
}

export interface ClientStats {
  totalClients: number;
  activeClients: number;
  inactiveClients: number;
  newThisMonth: number;
}
