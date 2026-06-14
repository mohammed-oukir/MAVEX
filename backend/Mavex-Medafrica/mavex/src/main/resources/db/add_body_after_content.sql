-- Add body_after_content column to email_templates
-- Stores the editable paragraphs that appear after the total-box (fixed block)
ALTER TABLE email_templates ADD COLUMN IF NOT EXISTS body_after_content TEXT;
