# Résolution du Problème "Push Service Error"

## 🔴 Erreur Rencontrée

```
AbortError: Registration failed - push service error
```

Cette erreur se produit lors de l'appel à `getToken()` et indique un problème avec le service de notifications push.

## 🔍 Causes Possibles

### 1. Clé VAPID Incorrecte ou Expirée

La clé VAPID dans votre code doit correspondre exactement à celle configurée dans Firebase Console.

**Comment Vérifier :**

1. Aller sur [Firebase Console](https://console.firebase.google.com)
2. Sélectionner votre projet : `projet-cloud-s5-routier`
3. Aller dans **Paramètres du projet** (⚙️ en haut à gauche)
4. Onglet **Cloud Messaging**
5. Chercher **"Certificats de clés Web push"** ou **"Web Push certificates"**
6. Copier la **Clé de serveur** (VAPID key)

**Clé actuelle dans le code :**
```
BMjmtEyox-Cq7673l2i68KbFeQQNRF6trQeuN4tfYHvwMBFbPoMtMgUL2FdX4MDd0XLm-PdCQLM-mZunRByy9tI
```

Si elle diffère, la remplacer dans :
- `mobile-app/src/views/MapView.vue` (ligne ~280)
- `mobile-app/src/App.vue` (lignes ~74 et ~85)
- `mobile-app/public/test-fcm.html` (si elle y figure)

### 2. Cloud Messaging API Non Activée

**Comment Vérifier :**

1. Aller sur [Google Cloud Console](https://console.cloud.google.com)
2. Sélectionner le projet `projet-cloud-s5-routier`
3. Dans le menu, aller dans **APIs & Services** > **Library**
4. Chercher **"Firebase Cloud Messaging API"**
5. Vérifier qu'elle est **ACTIVÉE** (bouton vert)
6. Si non activée, cliquer sur **ACTIVER**

### 3. Problème du Service Push du Navigateur

Certains navigateurs (surtout sur Windows) peuvent avoir des problèmes temporaires avec les services de push.

**Solutions de Contournement :**

#### A. Nettoyer le Cache du Navigateur

```javascript
// Dans la console du navigateur (F12)

// 1. Désinscrire tous les Service Workers
navigator.serviceWorker.getRegistrations().then(regs => {
  regs.forEach(r => r.unregister());
  console.log('Service Workers désinscrits');
});

// 2. Vider le cache
caches.keys().then(keys => {
  keys.forEach(key => caches.delete(key));
  console.log('Cache vidé');
});
```

Puis **recharger la page** avec Ctrl + Shift + R (rechargement forcé).

#### B. Tester dans un Autre Navigateur

- **Chrome** : Meilleur support FCM
- **Firefox** : Bon support
- **Edge** : Support correct
- **Brave** : Peut bloquer les notifications par défaut

#### C. Vérifier les Paramètres du Navigateur

**Chrome :**
1. Aller dans `chrome://settings/content/notifications`
2. Vérifier que les notifications ne sont pas bloquées globalement
3. Vérifier que `localhost` ou votre domaine est autorisé

**Firefox :**
1. Aller dans `about:preferences#privacy`
2. Section "Permissions" > "Notifications" > "Paramètres"
3. Vérifier les permissions pour votre site

### 4. Configuration HTTPS (Production)

⚠️ **Important :** FCM nécessite HTTPS en production (localhost est OK en développement)

Si vous déployez l'application :
- Assurez-vous que le site est en HTTPS
- Le Service Worker doit être accessible via HTTPS

### 5. Problème de Configuration Firebase

**Vérifier la configuration dans `config.ts` :**

```typescript
const firebaseConfig = {
  apiKey: "AIzaSyBroRMMRCSVdqAzpuivp7PSSP9X1WIk3VY",
  authDomain: "projet-cloud-s5-routier.firebaseapp.com",
  projectId: "projet-cloud-s5-routier",
  storageBucket: "projet-cloud-s5-routier.firebasestorage.app",
  messagingSenderId: "792049548362",
  appId: "1:792049548362:web:6ab3ce65b1584730c63ab3"
};
```

Vérifier que ces valeurs correspondent à votre projet Firebase :
1. Firebase Console > Paramètres du projet > Général
2. Section "Vos applications" > Application Web
3. Comparer chaque valeur

## 🛠️ Solutions Recommandées

### Solution 1 : Mode Dégradé (Déjà Implémentée) ✅

L'application continue de fonctionner sans notifications push :

```typescript
try {
  const token = await getToken(messaging, { vapidKey: '...' });
  // Enregistrer le token
} catch (error) {
  if (error.message.includes('push service error')) {
    console.warn('Connexion sans notifications push');
    // L'utilisateur peut continuer à utiliser l'app
  }
}
```

**Avantage :** L'application reste fonctionnelle même si FCM ne marche pas.

### Solution 2 : Vérifier et Régénérer la Clé VAPID

1. **Firebase Console** > Projet > Paramètres > Cloud Messaging
2. Onglet **"Web Push certificates"**
3. Si la clé existe : **la copier exactement**
4. Si pas de clé : Cliquer sur **"Generate key pair"**
5. Copier la nouvelle clé
6. Remplacer dans tous les fichiers mentionnés ci-dessus

### Solution 3 : Test avec un Token de Test

Créer un fichier de test simple pour isoler le problème :

```html
<!-- test-fcm-simple.html -->
<!DOCTYPE html>
<html>
<head>
    <title>Test FCM Minimal</title>
</head>
<body>
    <h1>Test FCM Minimal</h1>
    <button onclick="testFCM()">Tester FCM</button>
    <pre id="result"></pre>

    <script type="module">
        import { initializeApp } from 'https://www.gstatic.com/firebasejs/10.0.0/firebase-app.js';
        import { getMessaging, getToken } from 'https://www.gstatic.com/firebasejs/10.0.0/firebase-messaging.js';

        const app = initializeApp({
            apiKey: "AIzaSyBroRMMRCSVdqAzpuivp7PSSP9X1WIk3VY",
            projectId: "projet-cloud-s5-routier",
            messagingSenderId: "792049548362",
            appId: "1:792049548362:web:6ab3ce65b1584730c63ab3"
        });

        window.testFCM = async () => {
            const result = document.getElementById('result');
            result.textContent = 'Test en cours...\n';

            try {
                // Enregistrer le SW
                const reg = await navigator.serviceWorker.register('/firebase-messaging-sw.js');
                await navigator.serviceWorker.ready;
                result.textContent += '✅ Service Worker prêt\n';

                // Demander permission
                const perm = await Notification.requestPermission();
                result.textContent += `✅ Permission: ${perm}\n`;

                // Obtenir token
                const messaging = getMessaging(app);
                const token = await getToken(messaging, {
                    vapidKey: 'BMjmtEyox-Cq7673l2i68KbFeQQNRF6trQeuN4tfYHvwMBFbPoMtMgUL2FdX4MDd0XLm-PdCQLM-mZunRByy9tI',
                    serviceWorkerRegistration: reg
                });

                result.textContent += `✅ Token: ${token}\n`;
            } catch (error) {
                result.textContent += `❌ Erreur: ${error.message}\n`;
                console.error(error);
            }
        };
    </script>
</body>
</html>
```

### Solution 4 : Vérifier les Logs Firebase

Dans Firebase Console :
1. **Cloud Messaging** > **Rapports**
2. Vérifier s'il y a des erreurs d'enregistrement
3. Vérifier les quotas (normalement très élevés)

## 📊 Diagnostic en Temps Réel

Exécuter dans la console du navigateur :

```javascript
// Test complet FCM
(async () => {
    console.log('=== Diagnostic FCM ===');
    
    // 1. Service Worker
    console.log('1. Service Worker Controller:', navigator.serviceWorker.controller);
    
    // 2. Permissions
    console.log('2. Notification Permission:', Notification.permission);
    
    // 3. Registrations
    const regs = await navigator.serviceWorker.getRegistrations();
    console.log('3. Service Workers:', regs.length);
    regs.forEach(r => console.log('   -', r.scope, r.active?.state));
    
    // 4. Test de subscription directe (sans Firebase)
    try {
        const reg = await navigator.serviceWorker.ready;
        const sub = await reg.pushManager.subscribe({
            userVisibleOnly: true,
            applicationServerKey: 'BMjmtEyox-Cq7673l2i68KbFeQQNRF6trQeuN4tfYHvwMBFbPoMtMgUL2FdX4MDd0XLm-PdCQLM-mZunRByy9tI'
        });
        console.log('4. Push Subscription OK:', sub.endpoint);
        await sub.unsubscribe();
    } catch (error) {
        console.error('4. Push Subscription ERROR:', error.message);
    }
})();
```

Si l'étape 4 échoue avec "push service error", le problème vient du navigateur ou de la clé VAPID.

## ✅ Vérification Finale

Une fois corrigé, vous devriez voir dans les logs :

```
[config.ts] Service Worker en contrôle, initialisation du messaging
[config.ts] Firebase Messaging initialisé avec succès
Début requestFcmToken...
Permission notification: granted
Tentative de récupération du token FCM...
✅ FCM Token reçu: [long-token-string]
Token FCM enregistré avec succès dans Firestore
```

## 🆘 Si Rien Ne Marche

**Mode dégradé activé :** Votre application fonctionnera sans notifications push. Les utilisateurs pourront :
- Se connecter normalement
- Créer des signalements
- Voir la carte et les signalements
- Tout sauf recevoir des notifications en temps réel

Pour les notifications, vous pouvez utiliser :
- Polling (vérification périodique)
- WebSocket (si vous avez un serveur backend)
- Notifications in-app (bannières dans l'interface)

---

**Date :** 6 février 2026  
**Status :** Mode dégradé activé, application fonctionnelle
