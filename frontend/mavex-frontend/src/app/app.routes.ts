import { Routes } from '@angular/router';
import { adminGuard, authGuard, noAuthGuard } from './core/auth/auth.guard';
import { permissionGuard } from './core/auth/permission.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

  {
    path: 'login',
    canActivate: [noAuthGuard],
    loadComponent: () =>
      import('./features/login/login.component').then(m => m.LoginComponent),
  },

  {
    path: '403',
    loadComponent: () =>
      import('./features/forbidden/forbidden.component').then(m => m.ForbiddenComponent),
  },

  {
    path: '',
    canActivate: [authGuard],
    loadComponent: () =>
      import('./layout/main-layout/main-layout.component').then(m => m.MainLayoutComponent),
    children: [
      {
        path: 'dashboard',
        canActivate: [permissionGuard('DASHBOARD')],
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
      },
      {
        path: 'shipments',
        canActivate: [permissionGuard('SHIPMENTS')],
        loadComponent: () =>
          import('./features/shipments/shipments.component').then(m => m.ShipmentsComponent),
      },
      {
        path: 'shipments/:id',
        canActivate: [permissionGuard('SHIPMENTS')],
        loadComponent: () =>
          import('./features/shipment-detail/shipment-detail.component').then(m => m.ShipmentDetailComponent),
      },
      {
        path: 'orders',
        canActivate: [permissionGuard('ORDERS')],
        loadComponent: () =>
          import('./features/orders/orders.component').then(m => m.OrdersComponent),
      },
      {
        path: 'imports',
        canActivate: [permissionGuard('IMPORTS')],
        loadComponent: () =>
          import('./features/imports/imports.component').then(m => m.ImportsComponent),
      },
      {
        path: 'clients',
        canActivate: [permissionGuard('CLIENTS')],
        loadComponent: () =>
          import('./features/clients/clients.component').then(m => m.ClientsComponent),
      },
      {
        path: 'shippers',
        canActivate: [permissionGuard('SHIPPERS')],
        loadComponent: () =>
          import('./features/shippers/shippers.component').then(m => m.ShippersComponent),
      },
      {
        path: 'payments',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/payments/payments.component').then(m => m.PaymentsComponent),
      },
      {
        path: 'airlines',
        canActivate: [permissionGuard('AIRLINES')],
        loadComponent: () =>
          import('./features/airlines/airlines.component').then(m => m.AirlinesComponent),
      },
      {
        path: 'users',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/users/users.component').then(m => m.UsersComponent),
      },
      {
        path: 'permissions',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/permissions/permissions.component').then(m => m.PermissionsComponent),
      },
      {
        path: 'profile',
        loadComponent: () =>
          import('./features/profile/profile.component').then(m => m.ProfileComponent),
      },
      {
        path: 'exchange-rates',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/exchange-rates/exchange-rates.component').then(m => m.ExchangeRatesComponent),
      },
      {
        path: 'settings/email-template',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/email-template/email-template.component').then(m => m.EmailTemplateComponent),
      },
      {
        path: 'settings/email-provider',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/email-provider/email-provider.component').then(m => m.EmailProviderComponent),
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
