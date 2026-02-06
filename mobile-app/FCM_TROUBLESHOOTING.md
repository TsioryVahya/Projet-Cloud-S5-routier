# Guide de Dépannage FCM (Firebase Cloud Messaging)

## Problème Résolu: "Failed to execute 'subscribe' on 'PushManager': Subscription failed - no active Service Worker"

### ⚠️ Cause du Problème

Le Service Worker doit être **actif ET contrôler la page** avant de pouvoir s'abonner aux notifications push. Le problème survient quand :
1. Le Service Worker est installé mais ne contrôle pas encore la page
2. On essaie d'obtenir le token FCM trop tôt

### ✅ Solution Implémentée

#### 1. **Enregistrement Précoce du Service Worker** (`main.ts`)
- Le Service Worker est maintenant enregistré **dès le chargement de l'application**
- Cela garantit qu'il est prêt avant toute tentative de connexion
- Logs ajoutés pour suivre le processus

#### 2. **Vérification du Contrôle** (`firebase/config.ts`)
- La fonction `initMessaging()` vérifie que le Service Worker contrôle la page
- Si ce n'est pas le cas, elle propose un rechargement
- Délai de sécurité ajouté pour garantir la stabilité

#### 3. **Activation Immédiate** (`firebase-messaging-sw.js`)
- `self.skipWaiting()` force l'installation immédiate
- `clients.claim()` prend le contrôle de toutes les pages
- Message handler pour forcer l'activation si nécessaire

### 🧪 Comment Tester (IMPORTANT)

#### Option 1 : Premier Chargement Propre (RECOMMANDÉ)

1. **Nettoyer complètement le navigateur :**
   - Ouvrir DevTools (F12)
   - Aller dans "Application" > "Service Workers"
   - Cliquer sur "Unregister" pour tous les Service Workers
   - Aller dans "Application" > "Storage" > "Clear site data"
   - **Fermer et rouvrir le navigateur**

2. **Démarrer l'application :**
   ```powershell
   cd mobile-app
   npm run dev
   ```

3. **Première visite :**
   - Ouvrir http://localhost:5173
   - Vérifier les logs de la console :
     ```
     [main.ts] Enregistrement précoce du Service Worker Firebase...
     [main.ts] Service Worker Firebase enregistré: http://localhost:5173/
     [firebase-messaging-sw.js] Service Worker en cours d'installation...
     [firebase-messaging-sw.js] Service Worker activé et prend le contrôle...
     ```

4. **RECHARGER LA PAGE** (Ctrl + R ou F5)
   - Cette étape est CRUCIALE pour que le SW prenne le contrôle
   - Vérifier que le log affiche : `[main.ts] Service Worker contrôle la page`

5. **Se connecter :**
   - Cliquer sur "Se connecter"
   - Entrer les identifiants
   - Accepter les notifications si demandé
   - Vérifier les logs :
     ```
     [config.ts] Service Worker en contrôle, initialisation du messaging
     [config.ts] Firebase Messaging initialisé avec succès
     Début requestFcmToken...
     Permission notification: granted
     Tentative de récupération du token FCM...
     FCM Token reçu: [token]
     Token FCM enregistré avec succès dans Firestore
     ```

#### Option 2 : Test Rapide avec Page de Diagnostic

1. Ouvrir http://localhost:5173/test-fcm.html
2. Suivre les étapes dans l'ordre :
   - Étape 1 : Vérifier/Enregistrer le Service Worker
   - Étape 2 : Demander la permission
   - **Recharger la page** (F5)
   - Étape 3 : Obtenir le token FCM

### 🔍 Vérifications dans la Console

1. **Vérifier que le Service Worker contrôle la page :**
   ```javascript
   // Dans la console du navigateur (F12)
   console.log('Contrôleur:', navigator.serviceWorker.controller);
   // Si null, le SW ne contrôle pas encore la page → Recharger (F5)
   // Si objet, tout est OK ✅
   ```

2. **Vérifier l'enregistrement du Service Worker :**
   ```javascript
   navigator.serviceWorker.getRegistrations().then(regs => {
     console.log('Service Workers enregistrés:', regs.length);
     regs.forEach(reg => {
       console.log('Scope:', reg.scope);
       console.log('Active:', reg.active?.state);
       console.log('Installing:', reg.installing?.state);
       console.log('Waiting:', reg.waiting?.state);
     });
   });
   ```

3. **Vérifier dans l'onglet "Application" de DevTools :**
   - Aller dans "Application" > "Service Workers"
   - Vous devriez voir `/firebase-messaging-sw.js`
   - État : "activated and is running"
   - ⚠️ **Si vous voyez "waiting to activate" → Cocher "Update on reload"**

4. **Vérifier la permission de notification :**
   ```javascript
   console.log('Permission actuelle:', Notification.permission);
   // Si 'default' → Pas encore demandée
   // Si 'denied' → Bloquée par l'utilisateur
   // Si 'granted' → OK ✅
   ```

### ⚠️ Résolution des Problèmes Courants

#### Problème : "Service Worker pas encore en contrôle"

**Solution :** Recharger la page (F5) après la première visite
```javascript
// Si vous voyez ce message dans les logs
[config.ts] Service Worker pas encore en contrôle. Enregistrement et rechargement nécessaire...

// Action : Accepter le rechargement ou presser F5 manuellement
```

#### Problème : Service Worker reste en état "waiting"

**Solution :**
1. Dans DevTools > Application > Service Workers
2. Cocher "Update on reload"
3. Cliquer sur "Skip waiting" sur le SW en attente
4. Recharger la page (F5)

#### Problème : Erreur "AbortError: Subscription failed"

**Cause :** Le Service Worker n'est pas en contrôle de la page

**Solution :**
```javascript
// 1. Vérifier
console.log('Contrôle:', navigator.serviceWorker.controller);

// 2. Si null, recharger la page
if (!navigator.serviceWorker.controller) {
  console.warn('Rechargement nécessaire');
  location.reload();
}
```

#### Problème : Notifications bloquées

**Solution :**
1. Cliquer sur l'icône 🔒 à gauche de l'URL
2. Paramètres du site > Notifications > Autoriser
3. Recharger la page (F5)

4. **Tester la permission de notification:**
   ```javascript
   // Dans la console du navigateur
   console.log('Permission actuelle:', Notification.permission);
   // Si 'default', demander la permission
   if (Notification.permission === 'default') {
     Notification.requestPermission().then(perm => console.log('Nouvelle permission:', perm));
   }
   ```

### Étapes de Test

1. **Démarrer l'application:**
   ```powershell
   cd mobile-app
   npm run dev
   ```

2. **Ouvrir dans le navigateur:** http://localhost:5173

3. **Se connecter** avec un compte agent

4. **Vérifier les logs dans la console:**
   - Vous devriez voir: "Service Worker enregistré avec succès"
   - Puis: "Permission notification: granted" (après avoir accepté)
   - Enfin: "FCM Token reçu: ..." avec le token

### En Cas de Problème Persistant

1. **Vider le cache et les Service Workers:**
   ```javascript
   // Dans la console
   navigator.serviceWorker.getRegistrations().then(regs => {
     regs.forEach(reg => reg.unregister());
   });
   // Puis recharger la page (Ctrl + Shift + R)
   ```

2. **Vérifier que le fichier `firebase-messaging-sw.js` est accessible:**
   - Ouvrir http://localhost:5173/firebase-messaging-sw.js dans le navigateur
   - Vous devriez voir le contenu du fichier JavaScript

3. **Vérifier les permissions du navigateur:**
   - Cliquer sur l'icône à gauche de l'URL
   - Vérifier que les notifications sont autorisées

4. **Mode incognito:**
   - Tester dans une fenêtre de navigation privée
   - Les Service Workers fonctionnent différemment en mode incognito

### Notes Importantes

- **Le Service Worker ne fonctionne que sur HTTPS ou localhost**
- **Les notifications peuvent être bloquées au niveau du système d'exploitation**
- **Sur mobile Android avec Capacitor, FCM fonctionne via les API natives, pas via le Service Worker**
- **Vérifier que la clé VAPID dans le code correspond à celle dans Firebase Console**

### Vérification de la Clé VAPID

1. Aller sur Firebase Console: https://console.firebase.google.com
2. Sélectionner votre projet
3. Allez dans Paramètres du projet > Cloud Messaging
4. Vérifier que la "Clé de serveur" correspond à celle dans le code

### Logs Attendus (Séquence Normale)

```
[firebase-messaging-sw.js] Service Worker en cours d'installation...
[firebase-messaging-sw.js] Service Worker activé et prend le contrôle...
Enregistrement du Service Worker Firebase...
Service Worker enregistré avec succès
Service Worker déjà actif
Début requestFcmToken...
Permission notification: granted
Tentative de récupération du token FCM...
FCM Token reçu: [long token string]
Token FCM enregistré avec succès dans Firestore
```

### Support Navigateur

| Navigateur | Service Worker | FCM | Notes |
|------------|----------------|-----|-------|
| Chrome     | ✅ | ✅ | Support complet |
| Firefox    | ✅ | ✅ | Support complet |
| Safari     | ✅ | ❌ | Pas de support FCM |
| Edge       | ✅ | ✅ | Support complet |
| Mobile Chrome | ✅ | ✅ | Support complet |
| Mobile Safari | ❌ | ❌ | Pas de support SW/FCM |
