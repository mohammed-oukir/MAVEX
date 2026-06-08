import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface EmailTemplateDTO {
  id: number;
  subject: string;
  bodyContent: string;
  htmlContent?: string;
}

@Injectable({ providedIn: 'root' })
export class EmailTemplateService {
  private readonly http = inject(HttpClient);

  getByType(type: string): Observable<EmailTemplateDTO> {
    return this.http.get<EmailTemplateDTO>(`/api/email-templates/${type}`);
  }

  update(id: number, dto: Omit<EmailTemplateDTO, 'htmlContent'>): Observable<EmailTemplateDTO> {
    return this.http.put<EmailTemplateDTO>(`/api/email-templates/${id}`, dto);
  }
}
