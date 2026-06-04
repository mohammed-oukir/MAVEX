import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Airline } from '../models/airline.model';

@Injectable({ providedIn: 'root' })
export class AirlineService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<Airline[]> {
    return this.http.get<Airline[]>('/api/airlines');
  }

  getByMawb(mawb: string): Observable<Airline> {
    return this.http.get<Airline>(`/api/airlines/prefix/${encodeURIComponent(mawb)}`);
  }

  search(name: string): Observable<Airline[]> {
    return this.http.get<Airline[]>(`/api/airlines/search?name=${encodeURIComponent(name)}`);
  }

  create(airline: Airline): Observable<Airline> {
    return this.http.post<Airline>('/api/airlines', airline);
  }

  update(prefix: string, airline: Airline): Observable<Airline> {
    return this.http.put<Airline>(`/api/airlines/${prefix}`, airline);
  }

  delete(prefix: string): Observable<void> {
    return this.http.delete<void>(`/api/airlines/${prefix}`);
  }
}
