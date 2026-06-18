import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AgentPermissions, AgentSummary, SavePermissionsRequest } from '../models/permission.model';

@Injectable({ providedIn: 'root' })
export class PermissionApiService {
  private readonly http = inject(HttpClient);

  /** Liste tous les agents avec leur compteur de permissions */
  getAgents(): Observable<AgentSummary[]> {
    return this.http.get<AgentSummary[]>('/api/permissions/agents');
  }

  /** Récupère le détail des permissions d'un agent */
  getAgentPermissions(userId: number): Observable<AgentPermissions> {
    return this.http.get<AgentPermissions>(`/api/permissions/agents/${userId}`);
  }

  /** Sauvegarde les permissions d'un agent (remplace tout) */
  saveAgentPermissions(userId: number, request: SavePermissionsRequest): Observable<AgentPermissions> {
    return this.http.put<AgentPermissions>(`/api/permissions/agents/${userId}`, request);
  }
}
