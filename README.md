# Projet Cloud S5 - Signalement et Suivi des Travaux Routiers (Antananarivo)

Ce projet est un système de gestion des signalements routiers comprenant un backend d'authentification, une application web de gestion et une application mobile de saisie.

## 🏗️ Structure du Projet

- `infra/` : Configuration de l'infrastructure (Postgres, Tileserver).
- `backend-identity/` : API REST de gestion des identités (Java Spring Boot).
- `web-app/` : Interface d'administration et de visualisation (Angular).
- `mobile-app/` : Application de signalement terrain (Ionic / Vue).
- `docs/` : Documentation technique et schémas (MCD, captures).

## 🚀 Technologies Choisies

- **Backend** : Java Spring Boot (Port 8081)
- **Web** : Angular (Port 4200)
- **Mobile** : Ionic Vue
- **Base de données** : PostgreSQL 15 (Port 5432)
- **Cartographie** : Tileserver GL (Port 8080)

## 🛠️ Installation et Lancement

### Prérequis
- Docker et Docker Compose
- Node.js (pour le développement local web/mobile)
- Java 17+ (pour le développement local backend)

### Lancer l'infrastructure (DB + Cartes + Backend + Web)
```bash
docker-compose up --build
```

### Accès aux services
- **Web App** : [http://localhost:4200](http://localhost:4200)
- **Backend API (Swagger)** : [http://localhost:8081/swagger-ui/index.html](http://localhost:8081/swagger-ui/index.html)
- **Serveur de Cartes** : [http://localhost:8082](http://localhost:8082)
- **pgAdmin (Gestion DB)** : [http://localhost:5050](http://localhost:5050) (Login: `admin@routier.mg`, PWD: `admin`)
- **Base de données** : `localhost:5432` (User: `user_cloud`, PWD: `password_cloud`, DB: `routier_db`, Host: `db`)

## 📝 À faire (Prochaines étapes)

1. **Infra** : Ajouter le fichier `.mbtiles` d'Antananarivo dans `infra/tileserver/`.
2. **Backend** : Implémenter les entités `User`, le système de login (JWT), et la limitation des tentatives.
3. **Web** : Créer les composants Leaflet pour afficher la carte et les points de signalement.
4. **Mobile** : Configurer Firebase Auth et Firestore.
