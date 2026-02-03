-- =========================================================
-- EXTENSION
-- =========================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- =========================================================
-- CONFIGURATIONS SYSTEME
-- =========================================================
CREATE TABLE configurations (
    cle VARCHAR(100) PRIMARY KEY,
    valeur TEXT NOT NULL,
    description TEXT,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- ROLES
-- =========================================================
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) UNIQUE NOT NULL
);

-- =========================================================
-- STATUTS UTILISATEUR
-- =========================================================
CREATE TABLE statuts_utilisateur (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) UNIQUE NOT NULL
);

-- =========================================================
-- UTILISATEURS
-- =========================================================
CREATE TABLE utilisateurs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,
    role_id INT REFERENCES roles(id),
    statut_actuel_id INT REFERENCES statuts_utilisateur(id),
    tentatives_connexion INT DEFAULT 0,
    date_dernier_echec_connexion TIMESTAMP,
    date_deblocage_automatique TIMESTAMP,
    derniere_connexion TIMESTAMP,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- SESSIONS
-- =========================================================
CREATE TABLE sessions (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    utilisateur_id UUID REFERENCES utilisateurs(id) ON DELETE CASCADE,
    token_acces TEXT NOT NULL,
    refresh_token TEXT,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    date_expiration TIMESTAMP NOT NULL,
    date_derniere_activite TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    ip_connexion VARCHAR(45),
    user_agent TEXT,
    est_active BOOLEAN DEFAULT TRUE
);

CREATE INDEX idx_sessions_utilisateur ON sessions(utilisateur_id);
CREATE INDEX idx_sessions_token ON sessions(token_acces);

-- =========================================================
-- HISTORIQUE DES CONNEXIONS
-- =========================================================
CREATE TABLE historique_connexions (
    id SERIAL PRIMARY KEY,
    utilisateur_id UUID REFERENCES utilisateurs(id) ON DELETE CASCADE,
    date_tentative TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    succes BOOLEAN NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    raison_echec VARCHAR(255)
);

CREATE INDEX idx_historique_utilisateur ON historique_connexions(utilisateur_id);

-- =========================================================
-- HISTORIQUE STATUT UTILISATEUR
-- =========================================================
CREATE TABLE historique_utilisateur (
    id SERIAL PRIMARY KEY,
    utilisateur_id UUID REFERENCES utilisateurs(id) ON DELETE CASCADE,
    statut_id INT REFERENCES statuts_utilisateur(id),
    date_changement TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- STATUTS SIGNALEMENT
-- =========================================================
CREATE TABLE statuts_signalement (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) UNIQUE NOT NULL
);

-- =========================================================
-- TYPES DE SIGNALEMENT (INTEGRE DIRECTEMENT)
-- =========================================================
CREATE TABLE types_signalement (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    icone_path TEXT NOT NULL,
    couleur VARCHAR(7) DEFAULT '#FF0000',
    actif BOOLEAN DEFAULT TRUE,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- ENTREPRISES
-- =========================================================
CREATE TABLE entreprises (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- =========================================================
-- SIGNALEMENTS (AVEC type_id INTEGRE)
-- =========================================================
CREATE TABLE signalements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_firebase VARCHAR(255) UNIQUE,
    date_signalement TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut_id INT REFERENCES statuts_signalement(id),
    type_id INT REFERENCES types_signalement(id),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    utilisateur_id UUID REFERENCES utilisateurs(id)
);

CREATE INDEX idx_signalements_type ON signalements(type_id);

-- =========================================================
-- DETAILS SIGNALEMENT
-- =========================================================
CREATE TABLE signalements_details (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    signalement_id UUID UNIQUE REFERENCES signalements(id) ON DELETE CASCADE,
    description TEXT,
    surface_m2 DOUBLE PRECISION,
    budget DECIMAL(15, 2),
    entreprise_id INT REFERENCES entreprises(id)
);

CREATE TABLE signalement_photos (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    signalement_id UUID REFERENCES signalements(id) ON DELETE CASCADE,
    photo_url TEXT NOT NULL,
    est_principale BOOLEAN DEFAULT FALSE,
    date_ajout TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);


-- =========================================================
-- HISTORIQUE STATUT SIGNALEMENT
-- =========================================================
CREATE TABLE historique_signalement (
    id SERIAL PRIMARY KEY,
    signalement_id UUID REFERENCES signalements(id) ON DELETE CASCADE,
    statut_id INT REFERENCES statuts_signalement(id),
    date_changement TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
