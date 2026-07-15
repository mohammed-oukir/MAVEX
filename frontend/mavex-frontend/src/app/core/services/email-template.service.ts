import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { EmailTemplate } from '../models/email-template.model';

const BASE = '/api/email-templates';

@Injectable({ providedIn: 'root' })
export class EmailTemplateService {
  private readonly http = inject(HttpClient);

  getAll(): Observable<EmailTemplate[]> {
    return this.http.get<EmailTemplate[]>(BASE);
  }

  getByType(type: string): Observable<EmailTemplate> {
    return this.http.get<EmailTemplate>(`${BASE}/${type}`);
  }

  create(dto: EmailTemplate): Observable<EmailTemplate> {
    return this.http.post<EmailTemplate>(BASE, dto);
  }

  update(id: number, dto: EmailTemplate): Observable<EmailTemplate> {
    return this.http.put<EmailTemplate>(`${BASE}/${id}`, dto);
  }
}
