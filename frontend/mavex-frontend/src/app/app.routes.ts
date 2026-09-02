import { Routes } from '@angular/router';
import { adminGuard, authGuard, noAuthGuard } from './core/auth/auth.guard';
import { moduleAccessGuard } from './core/auth/module-access.guard';

export const routes: Routes = [
  { path: '', redirectTo: 'login', pathMatch: 'full' },

  {
    path: 'login',
    canActivate: [noAuthGuard],
    loadComponent: () =>
      import('./features/login/login.component').then(m => m.LoginComponent),
  },

  {
    path: 'settings/email-templates/:type',
    canActivate: [moduleAccessGuard],
    data: { module: 'EMAIL_SETTINGS' },
    loadComponent: () =>
      import('./features/email-settings/email-templates/email-templates-editor.component').then(m => m.EmailTemplatesEditorComponent),
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
        canActivate: [moduleAccessGuard],
        data: { module: 'DASHBOARD' },
        loadComponent: () =>
          import('./features/dashboard/dashboard.component').then(m => m.DashboardComponent),
      },
      {
        path: 'home',
        canActivate: [authGuard],
        loadComponent: () =>
          import('./features/welcome/welcome.component').then(m => m.WelcomeComponent),
      },
      {
        path: 'shipments',
        canActivate: [moduleAccessGuard],
        data: { module: 'SHIPMENTS' },
        loadComponent: () =>
          import('./features/shipments/shipments.component').then(m => m.ShipmentsComponent),
      },
      {
        path: 'shipments/:id',
        canActivate: [moduleAccessGuard],
        data: { module: 'SHIPMENTS' },
        loadComponent: () =>
          import('./features/shipment-detail/shipment-detail.component').then(m => m.ShipmentDetailComponent),
      },
      {
        path: 'shipments/:id/duty-history/shipment',
        canActivate: [moduleAccessGuard],
        data: { module: 'SHIPMENTS' },
        loadComponent: () =>
          import('./features/duty-history/duty-history-shipment.component').then(m => m.DutyHistoryShipmentComponent),
      },
      {
        path: 'shipments/:id/duty-history/orders',
        canActivate: [moduleAccessGuard],
        data: { module: 'SHIPMENTS' },
        loadComponent: () =>
          import('./features/duty-history/duty-history-orders.component').then(m => m.DutyHistoryOrdersComponent),
      },
      {
        path: 'orders',
        canActivate: [moduleAccessGuard],
        data: { module: 'ORDERS' },
        loadComponent: () =>
          import('./features/orders/orders.component').then(m => m.OrdersComponent),
      },
      {
        path: 'imports',
        canActivate: [moduleAccessGuard],
        data: { module: 'IMPORTS' },
        loadComponent: () =>
          import('./features/imports/imports.component').then(m => m.ImportsComponent),
      },
      {
        path: 'clients',
        canActivate: [moduleAccessGuard],
        data: { module: 'CLIENTS' },
        loadComponent: () =>
          import('./features/clients/clients.component').then(m => m.ClientsComponent),
      },
      {
        path: 'shippers',
        canActivate: [moduleAccessGuard],
        data: { module: 'SHIPPERS' },
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
        canActivate: [moduleAccessGuard],
        data: { module: 'AIRLINES' },
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
        path: 'agent-permissions',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/agent-permissions/agent-permissions.component').then(m => m.AgentPermissionsComponent),
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
        path: 'dashboard-analytics',
        canActivate: [moduleAccessGuard],
        data: { module: 'DASHBOARD_ANALYTICS' },
        loadComponent: () =>
          import('./features/dashboard-analytics/dashboard-analytics.component').then(m => m.DashboardAnalyticsComponent),
      },
      {
        path: 'settings/email-settings',
        canActivate: [moduleAccessGuard],
        data: { module: 'EMAIL_SETTINGS' },
        loadComponent: () =>
          import('./features/email-settings/email-settings.component').then(m => m.EmailSettingsComponent),
      },
      {
        path: 'settings/email-provider',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/email-provider/email-provider.component').then(m => m.EmailProviderComponent),
      },
      {
        path: 'settings/payment-config',
        canActivate: [adminGuard],
        loadComponent: () =>
          import('./features/payment-config/payment-config.component').then(m => m.PaymentConfigComponent),
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

  { path: '**', redirectTo: 'login' },
];
