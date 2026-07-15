export interface EmailTemplate {
  id?: number;
  type: string;
  name?: string;
  subject?: string;
  htmlContent?: string;
  builderJson?: string;
}
