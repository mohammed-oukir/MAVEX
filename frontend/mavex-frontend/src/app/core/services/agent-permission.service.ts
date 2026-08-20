import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  PermissionCatalogItem,
  AgentPermissionsResponse,
  PermissionAuditLogItem,
} from '../models/agent-permission.model';

@Injectable({ providedIn: 'root' })
export class AgentPermissionService {
  private readonly http = inject(HttpClient);

  getCatalog(): Observable<PermissionCatalogItem[]> {
    return this.http.get<PermissionCatalogItem[]>('/api/permission-catalog');
  }

  getAgentPermissions(agentId: number): Observable<AgentPermissionsResponse> {
    return this.http.get<AgentPermissionsResponse>(`/api/agent-permissions/${agentId}`);
  }

  updateAgentPermissions(agentId: number, permissionIds: number[]): Observable<AgentPermissionsResponse> {
    return this.http.put<AgentPermissionsResponse>(`/api/agent-permissions/${agentId}`, { permissionIds });
  }

  getAgentHistory(agentId: number): Observable<PermissionAuditLogItem[]> {
    return this.http.get<PermissionAuditLogItem[]>(`/api/agent-permissions/${agentId}/history`);
  }
}
