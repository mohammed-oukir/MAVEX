-- Migration : remplacement de smtp_configs par email_provider_configs dans email_logs
--
-- ORDRE D'EXECUTION OBLIGATOIRE :
--   1. Demarrer l'application une premiere fois
--      Hibernate (ddl-auto=update) cree automatiquement :
--        - la table email_provider_configs
--        - la colonne email_provider_config_id dans email_logs avec sa FK
--   2. Appliquer CE script manuellement sur la base APRES le demarrage
--
-- Ce script ne contient que les DROP que Hibernate ne fait jamais seul.

-- -----------------------------------------------------------------------
-- ETAPE 1 : Supprimer la FK smtp_config_id dans email_logs
--
-- Le nom de la contrainte est genere automatiquement par MySQL et peut
-- varier selon l'environnement. Executer d'abord cette requete pour
-- trouver le nom reel, puis remplacer FK_NAME ci-dessous :
--
--   SELECT CONSTRAINT_NAME
--   FROM information_schema.KEY_COLUMN_USAGE
--   WHERE TABLE_NAME = 'email_logs'
--     AND COLUMN_NAME = 'smtp_config_id'
--     AND CONSTRAINT_SCHEMA = DATABASE();
--
-- ALTER TABLE email_logs DROP FOREIGN KEY FK_NAME;
-- -----------------------------------------------------------------------

-- ETAPE 2 : Supprimer la colonne devenue orpheline
ALTER TABLE email_logs
    DROP COLUMN IF EXISTS smtp_config_id;

-- ETAPE 3 : Supprimer la table smtp_configs (jamais alimentee en production)
DROP TABLE IF EXISTS smtp_configs;
