-- Fix: colonne role trop petite pour COMPTABLE (9 chars)
-- ADMIN=5, AGENT=5, COMPTABLE=9 — passer à VARCHAR(20) pour avoir de la marge
ALTER TABLE users MODIFY COLUMN role VARCHAR(20) NOT NULL;
