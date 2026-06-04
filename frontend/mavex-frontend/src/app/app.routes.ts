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
      {
        path: 'shippers',
        loadComponent: () =>
          import('./features/shippers/shippers.component').then(m => m.ShippersComponent),
      },
      {
        path: 'payments',
        loadComponent: () =>
          import('./features/payments/payments.component').then(m => m.PaymentsComponent),
      },
      {
        path: 'users',
        loadComponent: () =>
          import('./features/users/users.component').then(m => m.UsersComponent),
      },
      {
        path: 'airlines',
        loadComponent: () =>
          import('./features/airlines/airlines.component').then(m => m.AirlinesComponent),
      },
    ],
  },

  {
    path: 'pay/:token',
    loadComponent: () =>
      import('./features/payment-page/payment-page.component').then(m => m.PaymentPageComponent),
  },
  {
    path: 'pay-success',
    data: { type: 'success' },
    loadComponent: () =>
      import('./features/payment-result/payment-result.component').then(m => m.PaymentResultComponent),
  },
  {
    path: 'pay-error',
    data: { type: 'error' },
    loadComponent: () =>
      import('./features/payment-result/payment-result.component').then(m => m.PaymentResultComponent),
  },
  {
    path: 'pay-cancelled',
    data: { type: 'cancelled' },
    loadComponent: () =>
      import('./features/payment-result/payment-result.component').then(m => m.PaymentResultComponent),
  },

  { path: '**', redirectTo: 'dashboard' },
];
