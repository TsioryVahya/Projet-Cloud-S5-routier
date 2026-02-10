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
    date_derniere_modification TIMESTAMP,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    firebase_uid VARCHAR(255) UNIQUE
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
    utilisateur_id UUID REFERENCES utilisateurs(id),
    firebase_uid_utilisateur VARCHAR(255), -- UUID Firebase de l'utilisateur qui a créé le signalement
    date_derniere_modification TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des Entreprises
CREATE TABLE entreprises (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(255) UNIQUE NOT NULL,
    description TEXT,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des Détails des Signalements (Informations techniques et médias)
CREATE TABLE signalements_details (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    signalement_id UUID UNIQUE REFERENCES signalements(id) ON DELETE CASCADE,
    description TEXT,
    surface_m2 DOUBLE PRECISION,
    budget DECIMAL(15, 2),
    entreprise_id INT REFERENCES entreprises(id),
    galerie_id UUID -- Sera lié à la galerie si besoin d'une photo principale ou groupe
);

-- Table de la Galerie des Signalements (Multiples images)
CREATE TABLE galerie_signalement (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_signalement UUID REFERENCES signalements(id) ON DELETE CASCADE,
    photo_url TEXT NOT NULL,
    date_ajout TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Ajout d'une clé étrangère optionnelle de signalements_details vers galerie_signalement si on veut pointer vers une image spécifique
ALTER TABLE signalements_details ADD CONSTRAINT fk_signalements_details_galerie FOREIGN KEY (galerie_id) REFERENCES galerie_signalement(id);


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

INSERT INTO entreprises (nom, description) VALUES 
('Colas Madagascar', 'Entreprise de travaux publics spécialisée dans la construction de routes'),
('Sogea-Satom', 'Acteur majeur du BTP en Afrique et à Madagascar');

-- Insertion du compte Manager par défaut
-- Mot de passe: manager123
INSERT INTO utilisateurs (email, mot_de_passe, role_id, statut_actuel_id, date_derniere_modification, firebase_uid) 
VALUES (
    'manager@routier.mg', 
    'manager123', 
    (SELECT id FROM roles WHERE nom = 'MANAGER'),
    (SELECT id FROM statuts_utilisateur WHERE nom = 'ACTIF'),
    CURRENT_TIMESTAMP,
    'manager-firebase-uid-default'
)
ON CONFLICT (email) DO NOTHING;

-- Ajout d'une entrée initiale dans l'historique pour le manager
INSERT INTO historique_utilisateur (utilisateur_id, statut_id)
SELECT id, statut_actuel_id FROM utilisateurs WHERE email = 'manager@routier.mg';


----modificattion: ajout type de signalement
-- Table des Types de Signalement
CREATE TABLE types_signalement (
    id SERIAL PRIMARY KEY,
    nom VARCHAR(100) UNIQUE NOT NULL,
    description TEXT,
    icone_path TEXT NOT NULL, -- Chemin SVG de l'icône
    couleur VARCHAR(7) DEFAULT '#FF0000', -- Couleur hex pour différencier visuellement
    actif BOOLEAN DEFAULT TRUE,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Modification de la table signalements pour ajouter le type
ALTER TABLE signalements 
ADD COLUMN type_id INT REFERENCES types_signalement(id);

CREATE INDEX idx_signalements_type ON signalements(type_id);

INSERT INTO types_signalement (nom, description, icone_path, couleur) VALUES
(
    'Nid de poule',
    'Trou ou dégradation importante de la chaussée',
    'M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9.5c-1.38 0-2.5-1.12-2.5-2.5s1.12-2.5 2.5-2.5 2.5 1.12 2.5 2.5-1.12 2.5-2.5 2.5z',
    '#E53935'
),
(
    'Affaissement',
    'Affaissement de la chaussée ou du bas-côté',
    'M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm-1 13v-6h2v6h-2zm0-8V5h2v2h-2z',
    '#FB8C00'
),
(
    'Inondation',
    'Accumulation d''eau sur la chaussée',
    'M12 2C8.13 2 5 5.13 5 9c0 5.25 7 13 7 13s7-7.75 7-13c0-3.87-3.13-7-7-7zm0 9c-1.66 0-3-1.34-3-3 0-.97.46-1.84 1.18-2.39.1.74.31 1.43.62 2.06.33.66.76 1.26 1.27 1.77.07.07.13.14.2.21-.08.22-.27.35-.27.35z',
    '#0288D1'
);

-- Insertion de l'utilisateur Tsiory (Donnée d'initialisation)

-- Ajout à l'historique pour Tsiory
INSERT INTO historique_utilisateur (utilisateur_id, statut_id)
SELECT id, statut_actuel_id FROM utilisateurs WHERE email = 'tsiory@gmail.com'
ON CONFLICT DO NOTHING;

-- Trigger pour historiser automatiquement les changements de statut des signalements
CREATE OR REPLACE FUNCTION fn_historiser_statut_signalement()
RETURNS TRIGGER AS $$
BEGIN
    -- Si c'est une insertion OU si le statut a changé
    IF (TG_OP = 'INSERT') OR (OLD.statut_id IS DISTINCT FROM NEW.statut_id) THEN
        -- On insère seulement si le dernier statut dans l'historique est différent
        -- (Utile pour éviter les doublons lors de synchronisations répétées)
        IF NOT EXISTS (
            SELECT 1 FROM historique_signalement 
            WHERE signalement_id = NEW.id 
            AND statut_id = NEW.statut_id
            ORDER BY date_changement DESC LIMIT 1
        ) THEN
            INSERT INTO historique_signalement (signalement_id, statut_id, date_changement)
            VALUES (NEW.id, NEW.statut_id, COALESCE(NEW.date_derniere_modification, CURRENT_TIMESTAMP));
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_signalement_statut_history ON signalements;
CREATE TRIGGER trg_signalement_statut_history
AFTER INSERT OR UPDATE OF statut_id ON signalements
FOR EACH ROW
EXECUTE FUNCTION fn_historiser_statut_signalement();