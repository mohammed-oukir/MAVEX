import { Routes } from '@angular/router';
import { authGuard, noAuthGuard } from './core/auth/auth.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

  {
    path: 'login',
    canActivate: [noAuthGuard],
    loadComponent: () =>
      import('./features/login/login.component').then(m => m.LoginComponent),
  },

  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layout/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    children: [
      {
        path: 'dashboard',
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
      },
      {
        path: 'shipments',
        loadComponent: () =>
          import('./features/shipments/shipments.component').then(m => m.ShipmentsComponent),
      },
      {
        path: 'shipments/:id',
        loadComponent: () =>
          import('./features/shipment-detail/shipment-detail.component').then(m => m.ShipmentDetailComponent),
      },
      {
        path: 'orders',
        loadComponent: () =>
          import('./features/orders/orders.component').then(m => m.OrdersComponent),
      },
      {
        path: 'imports',
        loadComponent: () =>
          import('./features/imports/imports.component').then(m => m.ImportsComponent),
      },
      {
        path: 'clients',
        loadComponent: () =>
          import('./features/clients/clients.component').then(m => m.ClientsComponent),
      },
    ],
  },

  { path: '**', redirectTo: 'dashboard' },
];
