export interface ShipperCountry {
  code: string;
  name?: string;
}

export interface ShipperResponse {
  id: number;
  companyName: string;
  contactName?: string;
  email?: string;
  phone?: string;
  address?: string;
  city?: string;
  state?: string;
  zipCode?: string;
  locationName?: string;
  countryCode?: ShipperCountry;
  active: boolean;
  createdAt?: string;
}

export interface ShipperRequest {
  companyName: string;
  email: string;
  contactName?: string;
  phone?: string;
  address?: string;
  city?: string;
  countryCode?: string;
}
