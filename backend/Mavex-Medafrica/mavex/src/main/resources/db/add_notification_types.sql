-- Table notification_types
CREATE TABLE notification_types (
    id          BIGINT AUTO_INCREMENT PRIMARY KEY,
    name        VARCHAR(255) NOT NULL UNIQUE,
    created_by  BIGINT NULL,
    created_at  DATETIME NOT NULL DEFAULT NOW(),
    updated_by  BIGINT NULL,
    updated_at  DATETIME NULL,
    CONSTRAINT fk_notification_types_created_by FOREIGN KEY (created_by) REFERENCES users(id),
    CONSTRAINT fk_notification_types_updated_by FOREIGN KEY (updated_by) REFERENCES users(id)
);

-- Seed des 9 types existants
INSERT INTO notification_types (name) VALUES
    ('PAYMENT_INVOICE_NO_AMOUNT'),
    ('PAYMENT_INVOICE_WITH_AMOUNT'),
    ('PAYMENT_CONFIRMED'),
    ('ACCOUNT_ACTIVATED'),
    ('ACCOUNT_REJECTED'),
    ('PASSWORD_RESET'),
    ('OTP_SENT'),
    ('PROFILE_UPDATE_NEEDED'),
    ('DELIVERY_UPDATE');

-- Migration email_templates.type (VARCHAR/enum) -> type_id (FK)
ALTER TABLE email_templates ADD COLUMN type_id BIGINT NULL;

UPDATE email_templates et
JOIN notification_types nt ON nt.name = et.type
SET et.type_id = nt.id;

ALTER TABLE email_templates DROP COLUMN type;

ALTER TABLE email_templates
    MODIFY COLUMN type_id BIGINT NOT NULL,
    ADD CONSTRAINT fk_email_templates_type
        FOREIGN KEY (type_id) REFERENCES notification_types(id);
