# 🛣️ Projet Cloud S5 - Signalement et Suivi des Travaux Routiers (Antananarivo)

Système complet de gestion, de signalement et de suivi des dégradations routières pour la ville d'Antananarivo. Ce projet permet aux citoyens de signaler des problèmes (nids-de-poule, glissements, etc.) et aux autorités de planifier et suivre les réparations.

---

## 🏗️ Architecture du Système

Le projet repose sur une architecture micro-services conteneurisée :

-   **Backend Identity (Java Spring Boot)** : API REST gérant l'authentification, les utilisateurs, les rôles, les entreprises et la logique métier des signalements.
-   **Web App (Angular)** : Tableau de bord d'administration pour la visualisation sur carte (Leaflet), la gestion des budgets et le suivi des travaux.
-   **Mobile App (Ionic / Vue)** : Application mobile pour les citoyens permettant de prendre des photos, localiser les dégradations et recevoir des notifications.
-   **Infrastructure (Docker)** : Déploiement automatisé incluant PostgreSQL, TileServer-GL pour les cartes offline et pgAdmin.

---

## 🚀 Fonctionnalités Clés

### 📱 Application Mobile (Citoyens)
- **Signalement Géo-localisé** : Capture de photos et détection automatique de la position GPS.
- **Galerie de Photos** : Visualisation des images du signalement via un carrousel horizontal fluide.
- **Suivi en Temps Réel** : Consultation de l'état d'avancement des travaux (En attente, En cours, Terminé).
- **Notifications Push** : Alertes via Firebase Cloud Messaging (FCM).

### 💻 Application Web (Administration)
- **Carte Interactive** : Visualisation globale des signalements sur Antananarivo via Leaflet et des tuiles vectorielles locales.
- **Gestion Budgétaire** : Calcul automatique des budgets estimés selon la surface (`surfaceM2`) et le prix au m².
- **Indicateurs de Gravité** : Visualisation des niveaux d'urgence (1 à 5).
- **Gestion des Entreprises** : Assignation des travaux aux entreprises partenaires (ex: Colas, etc.).

---

## 🛠️ Stack Technique

-   **Frontend Web** : Angular 17+, Tailwind CSS, Leaflet.js
-   **Frontend Mobile** : Ionic Framework, Vue.js, Capacitor, Firebase (Auth/FCM/Firestore)
-   **Backend** : Java 17, Spring Boot, Spring Data JPA, Hibernate, JWT
-   **Base de Données** : PostgreSQL 15 + PostGIS (via Docker)
-   **Cartographie** : TileServer-GL (tuiles vectorielles offline pour Antananarivo)
-   **DevOps** : Docker, Docker Compose

---

## 🚦 Installation et Lancement

### 1. Prérequis
-   **Docker & Docker Compose**
-   **Node.js** (v18+)
-   **JDK 17** (pour le backend)

### 2. Lancement Rapide (Docker)
Clonez le dépôt et lancez tous les services :
```bash
docker-compose up --build -d
```

### 3. Accès aux Services
| Service | URL | Identifiants par défaut |
| :--- | :--- | :--- |
| **Web Administration** | [http://localhost:4200](http://localhost:4200) | `admin@routier.mg` / `admin` |
| **Backend API** | [http://localhost:8081](http://localhost:8081) | - |
| **Serveur de Cartes** | [http://localhost:8082](http://localhost:8082) | - |
| **pgAdmin** | [http://localhost:5050](http://localhost:5050) | `admin@routier.mg` / `admin` |

---

## 📂 Structure du Dépôt

```text
.
├── backend-identity/    # Code source Java Spring Boot
├── web-app/             # Code source Angular
├── mobile-app/          # Code source Ionic Vue
├── infra/               # Fichiers de configuration (SQL, MBTiles)
│   ├── postgres/        # Scripts d'initialisation DB
│   └── tileserver/      # Données cartographiques (mbtiles)
└── docker-compose.yml   # Orchestration des conteneurs
```

---

## 👨‍💻 Équipe de Développement
Projet réalisé dans le cadre du module **INFO309 - Web Avancée** (S5).
- **ITU - 2026**
