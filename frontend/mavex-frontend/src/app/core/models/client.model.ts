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
