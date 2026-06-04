export interface Airline {
  prefix:   string;
  name:     string;
  iataCode?: string;
  icaoCode?: string;
  country?:  string;
  mode?:     string;
}
