-- Extension pour la génération de UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Table des Configurations Système
CREATE TABLE configurations (
    cle VARCHAR(100) PRIMARY KEY,
    valeur TEXT NOT NULL,
    description TEXT,
    date_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des Rôles
CREATE TABLE roles (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) UNIQUE NOT NULL -- 'UTILISATEUR', 'MANAGER'
);

-- Table des Statuts Utilisateur (Les types de statuts possibles)
CREATE TABLE statuts_utilisateur (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) UNIQUE NOT NULL -- 'ACTIF', 'BLOQUE'
);

-- Table des Utilisateurs
CREATE TABLE utilisateurs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,
    role_id INT REFERENCES roles(id),
    statut_actuel_id INT REFERENCES statuts_utilisateur(id), -- Pour avoir le statut actuel rapidement
    tentatives_connexion INT DEFAULT 0,
    date_dernier_echec_connexion TIMESTAMP,
    date_deblocage_automatique TIMESTAMP,
    derniere_connexion TIMESTAMP,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des Sessions
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

-- Table Historique des Connexions
CREATE TABLE historique_connexions (
    id SERIAL PRIMARY KEY,
    utilisateur_id UUID REFERENCES utilisateurs(id) ON DELETE CASCADE,
    date_tentative TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    succes BOOLEAN NOT NULL,
    ip_address VARCHAR(45),
    user_agent TEXT,
    raison_echec VARCHAR(255) -- 'mot_de_passe_incorrect', 'compte_bloque', etc.
);

CREATE INDEX idx_historique_utilisateur ON historique_connexions(utilisateur_id);

-- Table Historique des Statuts Utilisateur
CREATE TABLE historique_utilisateur (
    id SERIAL PRIMARY KEY,
    utilisateur_id UUID REFERENCES utilisateurs(id) ON DELETE CASCADE,
    statut_id INT REFERENCES statuts_utilisateur(id),
    date_changement TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des Statuts de Signalement
CREATE TABLE statuts_signalement (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(50) UNIQUE NOT NULL -- 'nouveau', 'en cours', 'terminé'
);

-- Table des Signalements (Informations de base et localisation)
CREATE TABLE signalements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_firebase VARCHAR(255) UNIQUE, -- Lien avec l'application mobile
    date_signalement TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut_id INT REFERENCES statuts_signalement(id),
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    utilisateur_id UUID REFERENCES utilisateurs(id)
);

-- Table des Détails des Signalements (Informations techniques et médias)
CREATE TABLE signalements_details (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    signalement_id UUID UNIQUE REFERENCES signalements(id) ON DELETE CASCADE,
    description TEXT,
    surface_m2 DOUBLE PRECISION,
    budget DECIMAL(15, 2),
    entreprise_concerne VARCHAR(255),
    photo_url TEXT
);

-- Table Historique des Statuts de Signalement
CREATE TABLE historique_signalement (
    id SERIAL PRIMARY KEY,
    signalement_id UUID REFERENCES signalements(id) ON DELETE CASCADE,
    statut_id INT REFERENCES statuts_signalement(id),
    date_changement TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Initialisation des données de référence
INSERT INTO configurations (cle, valeur, description) VALUES
('max_tentatives_connexion', '3', 'Nombre maximum de tentatives de connexion avant blocage'),
('duree_blocage_minutes', '15', 'Durée de blocage automatique en minutes'),
('duree_session_heures', '24', 'Durée de validité d''une session en heures'),
('duree_refresh_token_jours', '7', 'Durée de validité du refresh token en jours');

INSERT INTO roles (nom) VALUES ('UTILISATEUR'), ('MANAGER');
INSERT INTO statuts_utilisateur (nom) VALUES ('ACTIF'), ('BLOQUE');
INSERT INTO statuts_signalement (nom) VALUES ('nouveau'), ('en cours'), ('terminé');

-- Insertion du compte Manager par défaut
-- Mot de passe: manager123
INSERT INTO utilisateurs (email, mot_de_passe, role_id, statut_actuel_id) 
VALUES (
    'manager@routier.mg', 
    'manager123', 
    (SELECT id FROM roles WHERE nom = 'MANAGER'),
    (SELECT id FROM statuts_utilisateur WHERE nom = 'ACTIF')
)
ON CONFLICT (email) DO NOTHING;

-- Ajout d'une entrée initiale dans l'historique pour le manager
INSERT INTO historique_utilisateur (utilisateur_id, statut_id)
SELECT id, statut_actuel_id FROM utilisateurs WHERE email = 'manager@routier.mg';
