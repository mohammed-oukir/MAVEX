-- ============================================================
-- Table : agent_permissions
-- Créée automatiquement par Hibernate (ddl-auto=update).
-- Ce fichier sert de documentation de référence.
-- ============================================================
CREATE TABLE IF NOT EXISTS agent_permissions (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id    BIGINT       NOT NULL,
    module     VARCHAR(50)  NOT NULL,   -- ex: ORDERS, SHIPMENTS, CLIENTS ...
    action     VARCHAR(20)  NOT NULL,   -- VIEW | CREATE | EDIT | DELETE
    granted    BOOLEAN      NOT NULL DEFAULT FALSE,

    CONSTRAINT fk_perm_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT uq_perm_user_module_action UNIQUE (user_id, module, action)
);
