import { PermissionAction, PermissionModule } from './auth.model';

export interface PermissionEntry {
  module: PermissionModule;
  action: PermissionAction;
  granted: boolean;
}

export interface AgentPermissions {
  userId: number;
  fullName: string;
  email: string;
  entries: PermissionEntry[];
  byModule: Record<PermissionModule, PermissionAction[]>;
}

export interface AgentSummary {
  userId: number;
  fullName: string;
  email: string;
  totalGranted: number;
}

export interface SavePermissionsRequest {
  permissions: PermissionEntry[];
}

export const ALL_MODULES: PermissionModule[] = [
  'DASHBOARD', 'SHIPMENTS', 'ORDERS', 'CLIENTS', 'IMPORTS', 'SHIPPERS', 'AIRLINES'
];

export const ALL_ACTIONS: PermissionAction[] = ['VIEW', 'CREATE', 'EDIT', 'DELETE'];

export const MODULE_LABELS: Record<PermissionModule, string> = {
  DASHBOARD: 'Dashboard',
  SHIPMENTS: 'Shipments',
  ORDERS:    'Orders',
  CLIENTS:   'Clients',
  IMPORTS:   'Imports',
  SHIPPERS:  'Shippers',
  AIRLINES:  'Compagnies aériennes',
};

export const ACTION_LABELS: Record<PermissionAction, string> = {
  VIEW:   'Voir',
  CREATE: 'Créer',
  EDIT:   'Modifier',
  DELETE: 'Supprimer',
};

export const ACTION_COLORS: Record<PermissionAction, string> = {
  VIEW:   'blue',
  CREATE: 'green',
  EDIT:   'amber',
  DELETE: 'red',
};

/** Modules qui n'ont que VIEW (pas de CREATE/EDIT/DELETE) */
export const VIEW_ONLY_MODULES: PermissionModule[] = ['DASHBOARD'];
