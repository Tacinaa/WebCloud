-- ============================================================
-- init.sql — Initialisation de la base de données AppCloud
-- ============================================================

-- Extension pour les UUIDs (optionnel mais utile)
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Créer l'utilisateur applicatif avec des droits limités
DO $$
BEGIN
    IF NOT EXISTS (SELECT FROM pg_catalog.pg_roles WHERE rolname = 'appuser') THEN
        CREATE ROLE appuser LOGIN PASSWORD 'changeme';
    END IF;
END
$$;

-- Créer la base si elle n'existe pas (géré par l'env POSTGRES_DB)
-- Donner les droits à l'utilisateur applicatif
GRANT CONNECT ON DATABASE appdb TO appuser;
GRANT USAGE ON SCHEMA public TO appuser;
GRANT CREATE ON SCHEMA public TO appuser;
GRANT SELECT, INSERT, UPDATE, DELETE ON ALL TABLES IN SCHEMA public TO appuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT SELECT, INSERT, UPDATE, DELETE ON TABLES TO appuser;
GRANT USAGE ON ALL SEQUENCES IN SCHEMA public TO appuser;
ALTER DEFAULT PRIVILEGES IN SCHEMA public GRANT USAGE ON SEQUENCES TO appuser;

-- Table articles (créée ici pour référence, Flask SQLAlchemy la recréera)
CREATE TABLE IF NOT EXISTS articles (
    id SERIAL PRIMARY KEY,
    titre VARCHAR(200) NOT NULL,
    contenu TEXT NOT NULL,
    auteur VARCHAR(100) NOT NULL DEFAULT 'Anonyme',
    categorie VARCHAR(50) NOT NULL DEFAULT 'Général',
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    actif BOOLEAN DEFAULT TRUE
);

-- Table contacts
CREATE TABLE IF NOT EXISTS contacts (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) NOT NULL,
    email VARCHAR(150) NOT NULL,
    message TEXT NOT NULL,
    date_envoi TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    lu BOOLEAN DEFAULT FALSE
);

-- Index pour les performances
CREATE INDEX IF NOT EXISTS idx_articles_actif ON articles(actif);
CREATE INDEX IF NOT EXISTS idx_articles_categorie ON articles(categorie);
CREATE INDEX IF NOT EXISTS idx_articles_date ON articles(date_creation DESC);
CREATE INDEX IF NOT EXISTS idx_contacts_date ON contacts(date_envoi DESC);

-- Données initiales de test
INSERT INTO articles (titre, contenu, auteur, categorie) VALUES
('Bienvenue sur AppCloud', 'Application 3-tiers déployée sur AWS EC2 avec PostgreSQL et Python Flask.', 'Admin', 'Actualités'),
('Architecture Réseau AWS', 'VPC avec 3 sous-réseaux isolés : public (web), privé applicatif (backend), privé base de données. Security Groups et Network ACL configurés.', 'Équipe Technique', 'Technique')
ON CONFLICT DO NOTHING;

RAISE NOTICE 'Base de données AppCloud initialisée avec succès.';
