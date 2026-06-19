export type EmailProviderType = 'BREVO' | 'SMTP';

export interface EmailProviderConfigResponse {
  id: number;
  providerType: EmailProviderType;
  active: boolean;

  // Communs
  fromEmail: string;
  fromName: string;

  // SMTP (null si providerType = BREVO)
  smtpHost: string | null;
  smtpPort: number | null;
  smtpUsername: string | null;
  smtpSslEnabled: boolean;
  smtpTlsEnabled: boolean;

  // Presence des secrets — jamais les valeurs reelles
  hasSmtpPassword: boolean;
  hasBrevoApiKey: boolean;
  hasBrevoWebhookToken: boolean;

  createdAt: string;
  updatedAt: string;
}

export interface EmailProviderConfigRequest {
  providerType: EmailProviderType;

  // Communs — obligatoires
  fromEmail: string;
  fromName: string;

  // Brevo — optionnels (null = conserver la valeur existante)
  brevoApiKey?: string | null;
  brevoWebhookToken?: string | null;

  // SMTP — optionnels selon provider
  smtpHost?: string | null;
  smtpPort?: number | null;
  smtpUsername?: string | null;
  smtpPassword?: string | null;
  smtpSslEnabled?: boolean;
  smtpTlsEnabled?: boolean;
}

export interface TestConnectionRequest {
  testEmail: string;
}

export interface TestConnectionResponse {
  message: string;
}
