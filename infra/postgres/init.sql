-- Extension pour les fonctions de cryptage
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";

-- Table des Utilisateurs (Module Authentification)
CREATE TABLE IF NOT EXISTS utilisateurs (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    email VARCHAR(255) UNIQUE NOT NULL,
    mot_de_passe VARCHAR(255) NOT NULL,
    role VARCHAR(50) NOT NULL DEFAULT 'UTILISATEUR', -- 'UTILISATEUR', 'MANAGER'
    statut VARCHAR(20) NOT NULL DEFAULT 'ACTIF', -- 'ACTIF', 'BLOQUE'
    tentatives_connexion INT DEFAULT 0,
    derniere_connexion TIMESTAMP,
    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- Table des Signalements (Module Web/Mobile)
CREATE TABLE IF NOT EXISTS signalements (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    id_firebase VARCHAR(255) UNIQUE, -- ID original dans Firebase
    date_signalement TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    statut VARCHAR(50) NOT NULL DEFAULT 'nouveau', -- 'nouveau', 'en cours', 'terminé'
    latitude DOUBLE PRECISION NOT NULL,
    longitude DOUBLE PRECISION NOT NULL,
    description TEXT,
    surface_m2 DOUBLE PRECISION,
    budget DECIMAL(15, 2),
    entreprise_concerne VARCHAR(255),
    photo_url TEXT,
    utilisateur_id UUID REFERENCES utilisateurs(id)
);

-- Insertion du compte Manager par défaut
INSERT INTO utilisateurs (email, mot_de_passe, role) 
VALUES ('manager@routier.mg', 'manager123', 'MANAGER')
ON CONFLICT (email) DO NOTHING;
