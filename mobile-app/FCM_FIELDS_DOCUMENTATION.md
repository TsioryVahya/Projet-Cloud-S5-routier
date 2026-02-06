# Documentation des Champs FCM dans Firestore

## 📋 Nouveaux Champs Ajoutés

Lors de la connexion d'un utilisateur, l'application tente maintenant d'obtenir un token FCM et enregistre **toujours** le résultat dans Firestore, même en cas d'erreur.

### Champs FCM dans la Collection `utilisateurs`

| Champ | Type | Description |
|-------|------|-------------|
| `fcmToken` | string \| null | Le token FCM si obtenu avec succès, sinon `null` |
| `fcmTokenStatus` | string | Statut de la récupération du token (voir ci-dessous) |
| `fcmTokenError` | string \| undefined | Message d'erreur si applicable |
| `fcmTokenDate` | string (ISO 8601) | Date/heure de la dernière tentative |

---

## 📊 Valeurs Possibles de `fcmTokenStatus`

### ✅ Statuts de Succès

#### `active`
- **Description :** Token FCM obtenu avec succès
- **`fcmToken` :** Chaîne de caractères (token valide)
- **`fcmTokenError` :** Absent
- **Action :** L'utilisateur peut recevoir des notifications push

**Exemple :**
```json
{
  "fcmToken": "dXbY9Q8...",
  "fcmTokenStatus": "active",
  "fcmTokenDate": "2026-02-06T17:30:00.000Z"
}
```

---

### ⚠️ Statuts d'Erreur/Limitation

#### `no_token`
- **Description :** Aucun token n'a été retourné par Firebase
- **`fcmToken` :** `null`
- **`fcmTokenError` :** Absent
- **Cause :** Problème rare, généralement temporaire

**Exemple :**
```json
{
  "fcmToken": null,
  "fcmTokenStatus": "no_token",
  "fcmTokenDate": "2026-02-06T17:30:00.000Z"
}
```

---

#### `error`
- **Description :** Erreur lors de la récupération du token
- **`fcmToken` :** `null`
- **`fcmTokenError` :** Message d'erreur détaillé
- **Causes possibles :**
  - Push service error (clé VAPID incorrecte)
  - Problème réseau
  - Configuration Firebase incorrecte

**Exemple :**
```json
{
  "fcmToken": null,
  "fcmTokenStatus": "error",
  "fcmTokenError": "Registration failed - push service error",
  "fcmTokenDate": "2026-02-06T17:30:00.000Z"
}
```

---

#### `messaging_unavailable`
- **Description :** Firebase Messaging n'a pas pu être initialisé
- **`fcmToken` :** `null`
- **`fcmTokenError` :** "Service Worker non supporté"
- **Causes possibles :**
  - Navigateur ne supporte pas les Service Workers
  - Problème d'initialisation de Firebase
  - Mode navigation privée (certains navigateurs)

**Exemple :**
```json
{
  "fcmToken": null,
  "fcmTokenStatus": "messaging_unavailable",
  "fcmTokenError": "Service Worker non supporté",
  "fcmTokenDate": "2026-02-06T17:30:00.000Z"
}
```

---

#### `permission_denied`
- **Description :** L'utilisateur a bloqué les notifications
- **`fcmToken` :** `null`
- **`fcmTokenError` :** "Permission: denied"
- **Action utilisateur :** Doit autoriser les notifications dans les paramètres du navigateur

**Exemple :**
```json
{
  "fcmToken": null,
  "fcmTokenStatus": "permission_denied",
  "fcmTokenError": "Permission: denied",
  "fcmTokenDate": "2026-02-06T17:30:00.000Z"
}
```

---

#### `permission_default`
- **Description :** L'utilisateur a fermé la demande de permission sans répondre
- **`fcmToken` :** `null`
- **`fcmTokenError` :** "Permission: default"
- **Action :** L'utilisateur peut réessayer plus tard

**Exemple :**
```json
{
  "fcmToken": null,
  "fcmTokenStatus": "permission_default",
  "fcmTokenError": "Permission: default",
  "fcmTokenDate": "2026-02-06T17:30:00.000Z"
}
```

---

## 🔍 Diagnostic via Firestore

### Cas 1 : Notifications Push Fonctionnelles
```javascript
// Dans Firestore, pour un utilisateur
{
  "email": "user@example.com",
  "fcmToken": "dXbY9Q8R7...",
  "fcmTokenStatus": "active",
  "fcmTokenDate": "2026-02-06T17:30:00.000Z"
}
```
✅ Cet utilisateur peut recevoir des notifications push.

---

### Cas 2 : Push Service Error (Problème Courant)
```javascript
{
  "email": "user@example.com",
  "fcmToken": null,
  "fcmTokenStatus": "error",
  "fcmTokenError": "Registration failed - push service error",
  "fcmTokenDate": "2026-02-06T17:30:00.000Z"
}
```
❌ **Cause :** Clé VAPID incorrecte ou Cloud Messaging API non activée  
🔧 **Solution :** Voir `PUSH_SERVICE_ERROR_FIX.md`

---

### Cas 3 : Permission Refusée
```javascript
{
  "email": "user@example.com",
  "fcmToken": null,
  "fcmTokenStatus": "permission_denied",
  "fcmTokenError": "Permission: denied",
  "fcmTokenDate": "2026-02-06T17:30:00.000Z"
}
```
❌ **Cause :** L'utilisateur a bloqué les notifications  
🔧 **Solution :** L'utilisateur doit autoriser les notifications dans les paramètres du navigateur

---

## 📈 Statistiques Utiles

### Requête Firestore : Compter les utilisateurs avec FCM actif

```javascript
// Dans la console Firebase ou via l'API
const usersWithFCM = await db.collection('utilisateurs')
  .where('fcmTokenStatus', '==', 'active')
  .count()
  .get();

console.log('Utilisateurs avec notifications actives:', usersWithFCM.data().count);
```

### Requête : Trouver les utilisateurs avec erreur push service

```javascript
const usersWithError = await db.collection('utilisateurs')
  .where('fcmTokenStatus', '==', 'error')
  .get();

usersWithError.forEach(doc => {
  const data = doc.data();
  console.log(`${data.email}: ${data.fcmTokenError}`);
});
```

---

## 🔄 Mise à Jour du Token

Le token FCM peut être rafraîchi :
- À chaque connexion de l'utilisateur
- Périodiquement (tous les X jours)
- Quand l'utilisateur autorise les notifications après les avoir bloquées

Le champ `fcmTokenDate` permet de savoir quand la dernière tentative a eu lieu.

---

## 🎯 Impact sur l'Application

### Mode Normal (avec token)
- ✅ Notifications push en temps réel
- ✅ Mises à jour instantanées des signalements
- ✅ Alertes sur changement de statut

### Mode Dégradé (sans token)
- ✅ Application entièrement fonctionnelle
- ✅ Création et consultation de signalements
- ✅ Toutes les fonctionnalités sauf notifications push
- ℹ️ L'utilisateur doit rafraîchir manuellement pour voir les mises à jour

---

## 🛠️ Test Rapide

Pour tester si un utilisateur peut recevoir des notifications :

```javascript
// Dans votre backend (Node.js avec firebase-admin)
const admin = require('firebase-admin');

async function testNotification(userEmail) {
  // Récupérer l'utilisateur
  const userDoc = await admin.firestore()
    .collection('utilisateurs')
    .where('email', '==', userEmail)
    .get();
  
  if (userDoc.empty) {
    console.log('Utilisateur introuvable');
    return;
  }
  
  const userData = userDoc.docs[0].data();
  
  if (userData.fcmTokenStatus !== 'active' || !userData.fcmToken) {
    console.log(`❌ Notifications désactivées pour ${userEmail}`);
    console.log(`   Statut: ${userData.fcmTokenStatus}`);
    console.log(`   Erreur: ${userData.fcmTokenError || 'N/A'}`);
    return;
  }
  
  // Envoyer une notification de test
  const message = {
    notification: {
      title: '🔔 Test de Notification',
      body: 'Vos notifications fonctionnent correctement !'
    },
    token: userData.fcmToken
  };
  
  try {
    await admin.messaging().send(message);
    console.log(`✅ Notification envoyée à ${userEmail}`);
  } catch (error) {
    console.error(`❌ Erreur d'envoi:`, error);
  }
}

// Utilisation
testNotification('tendryniavo76@gmail.com');
```

---

**Date de mise à jour :** 6 février 2026  
**Version :** 2.0 - Enregistrement systématique du statut FCM
