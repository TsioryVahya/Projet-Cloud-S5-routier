# Configuration Firebase Cloud Messaging (FCM)

## Vue d'ensemble

Cette implémentation permet de recevoir des notifications push lorsque le statut d'un signalement change. Les notifications sont envoyées via Firebase Cloud Messaging (FCM) depuis le backend Java vers l'application mobile.

## Architecture

### Frontend (Mobile App)

1. **Service de Notifications** (`notificationService.ts`)
   - Initialise FCM et demande les permissions
   - Sauvegarde le FCM token dans Firestore
   - Écoute les messages en temps réel
   - Gère la collection `notifications` dans Firestore

2. **Interface Utilisateur** (`NotificationsView.vue`)
   - Affiche la liste des notifications
   - Badge avec compteur de notifications non lues
   - Marquer comme lu / tout marquer comme lu

3. **Navigation**
   - Nouvel onglet "Notifications" dans la barre de navigation
   - Badge rouge indiquant le nombre de notifications non lues

### Backend (Java/Spring Boot)

1. **Service FCM** (`FcmNotificationService.java`)
   - Envoie des notifications FCM aux utilisateurs
   - Crée des enregistrements dans la collection Firestore `notifications`
   - Gère l'envoi lors des changements de statut

2. **Service Signalement** (`SignalementService.java`)
   - Modifié pour envoyer des notifications lors des changements de statut
   - Intégré avec le service FCM

## Configuration Requise

### 1. Obtenir la clé VAPID

1. Allez dans la console Firebase : https://console.firebase.google.com
2. Sélectionnez votre projet
3. Allez dans **Paramètres du projet** (icône engrenage) > **Cloud Messaging**
4. Dans la section **Configuration Web**, générez une paire de clés Web push
5. Copiez la clé publique (VAPID key)

### 2. Configurer le Frontend

Mettez à jour le fichier `mobile-app/src/services/notificationService.ts` :

```typescript
const currentToken = await getToken(this.messaging, {
  vapidKey: 'VOTRE_CLE_VAPID_ICI' // Remplacer par votre clé VAPID
});
```

### 3. Configurer le Backend

Le backend utilise déjà le fichier `serviceAccountKey.json` pour l'authentification Firebase Admin SDK, donc aucune configuration supplémentaire n'est nécessaire côté backend.

## Structure de la Collection Firestore

### Collection: `notifications`

```json
{
  "userId": "string - ID Firebase de l'utilisateur",
  "signalementId": "string - ID Firebase du signalement",
  "titre": "string - Titre de la notification",
  "message": "string - Corps du message",
  "type": "string - Type (status_change, other)",
  "oldStatus": "string - Ancien statut (optionnel)",
  "newStatus": "string - Nouveau statut",
  "dateCreation": "timestamp - Date de création",
  "lu": "boolean - Notification lue ou non"
}
```

### Collection: `users` (mise à jour)

Chaque document utilisateur doit contenir :

```json
{
  "email": "string",
  "fcmToken": "string - Token FCM pour envoyer les notifications",
  "lastTokenUpdate": "timestamp"
}
```

## Flux de Notification

1. **Changement de statut** : Un admin change le statut d'un signalement via le web dashboard
2. **Backend détecte** : `SignalementService.modifierSignalement()` détecte le changement
3. **Récupération du token** : Le backend récupère le FCM token de l'utilisateur depuis Firestore
4. **Envoi FCM** : Une notification push est envoyée à l'utilisateur
5. **Enregistrement** : La notification est enregistrée dans Firestore
6. **Réception mobile** : L'app mobile reçoit la notification et met à jour l'UI

## Permissions Requises

### Android (Capacitor)

Dans `AndroidManifest.xml` :

```xml
<uses-permission android:name="android.permission.INTERNET" />
<uses-permission android:name="android.permission.POST_NOTIFICATIONS" />
```

### iOS (Capacitor)

Les permissions sont automatiquement gérées par Capacitor et Firebase.

## Test de Notifications

### 1. Test Manuel via Console Firebase

1. Allez dans **Messaging** dans la console Firebase
2. Créez une nouvelle campagne
3. Sélectionnez "Notification test"
4. Entrez le FCM token (visible dans les logs de l'app)
5. Envoyez la notification

### 2. Test via Changement de Statut

1. Créez un signalement depuis l'app mobile
2. Connectez-vous au dashboard web en tant qu'admin
3. Changez le statut du signalement
4. Vérifiez la réception de la notification sur mobile

## Debugging

### Vérifier le FCM Token

Dans les logs de l'application mobile :
```
📱 FCM Token obtenu: [TOKEN]
✅ FCM Token sauvegardé dans Firestore
```

### Vérifier l'envoi depuis le Backend

Dans les logs du backend :
```
📬 Préparation notification pour userId=..., signalement=..., ancien -> nouveau
✅ Notification FCM envoyée avec succès: [RESPONSE]
✅ Notification enregistrée dans Firestore
```

### Problèmes Courants

1. **Token non sauvegardé**
   - Vérifiez que l'utilisateur est connecté
   - Vérifiez les permissions de notification

2. **Notification non reçue**
   - Vérifiez que le service worker est enregistré
   - Vérifiez la clé VAPID
   - Vérifiez les logs backend pour les erreurs

3. **Badge ne s'affiche pas**
   - Rechargez les notifications : elles sont chargées en temps réel via Firestore

## Fonctionnalités

- ✅ Notifications push lors des changements de statut
- ✅ Badge avec compteur de notifications non lues
- ✅ Liste des notifications avec détails
- ✅ Marquer comme lu (individuellement ou en masse)
- ✅ Navigation vers le signalement concerné
- ✅ Notifications en arrière-plan (via service worker)
- ✅ Persistance des notifications dans Firestore
- ✅ Synchronisation temps réel

## Améliorations Futures

- [ ] Notifications pour d'autres événements (commentaires, etc.)
- [ ] Notifications groupées
- [ ] Filtrage des notifications par type
- [ ] Paramètres de notification personnalisables
- [ ] Sons et vibrations personnalisés
- [ ] Actions rapides depuis la notification
