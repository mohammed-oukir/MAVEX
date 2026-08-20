export interface PermissionCatalogItem {
  id: number;
  module: string;
  action: string;
  label: string;
  contextNote?: string;
}

export interface AgentPermissionsResponse {
  agentId: number;
  agentName: string;
  permissionIds: number[];
}

export type PermissionActionType = 'GRANTED' | 'REVOKED';

export interface PermissionAuditLogItem {
  id: number;
  permissionModule: string;
  permissionAction: string;
  permissionLabel: string;
  actionType: PermissionActionType;
  performedByName: string;
  performedAt: string;
}
