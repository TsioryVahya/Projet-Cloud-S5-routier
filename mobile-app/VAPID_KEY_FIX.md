# URGENT - Vérification Clé VAPID Firebase

## 🔴 Erreur Actuelle
```
Registration failed - push service error
```

Le Service Worker fonctionne maintenant ✅, mais la **clé VAPID** est incorrecte ou l'**API Cloud Messaging** n'est pas activée.

---

## ✅ ÉTAPE 1 : Vérifier/Générer la Clé VAPID

### 1. Aller sur Firebase Console

🔗 https://console.firebase.google.com

### 2. Sélectionner le Projet
- Cliquer sur **`projet-cloud-s5-routier`**

### 3. Paramètres du Projet
- Cliquer sur l'icône **⚙️** (engrenage) en haut à gauche
- Cliquer sur **"Paramètres du projet"**

### 4. Onglet Cloud Messaging
- Cliquer sur l'onglet **"Cloud Messaging"**
- Descendre jusqu'à la section **"Certificats de clés Web push"** ou **"Web Push certificates"**

### 5a. Si une Clé Existe
- **COPIER EXACTEMENT** la clé affichée
- Elle doit commencer par `B` et faire ~88 caractères
- Format : `BMjmtEyox-Cq7673l2i68K...`

### 5b. Si AUCUNE Clé n'Existe
- Cliquer sur le bouton **"Generate key pair"** ou **"Générer une paire de clés"**
- Une nouvelle clé sera générée
- **COPIER** cette nouvelle clé

---

## ✅ ÉTAPE 2 : Activer l'API Cloud Messaging

### 1. Aller sur Google Cloud Console

🔗 https://console.cloud.google.com

### 2. Sélectionner le Projet
- En haut : Sélectionner **`projet-cloud-s5-routier`**

### 3. Activer l'API
- Menu hamburger ☰ > **"APIs & Services"** > **"Library"**
- Dans la barre de recherche, taper : **"Firebase Cloud Messaging API"**
- Cliquer sur le résultat
- Cliquer sur le bouton **"ACTIVER"** (s'il n'est pas déjà activé)
- Attendre quelques secondes pour l'activation

### 4. Vérifier l'Activation
- Retourner dans **"APIs & Services"** > **"Enabled APIs & services"**
- Vous devriez voir **"Firebase Cloud Messaging API"** dans la liste

---

## ✅ ÉTAPE 3 : Remplacer la Clé VAPID dans le Code

### Fichiers à Modifier

La clé actuelle dans votre code est :
```
BMjmtEyox-Cq7673l2i68KbFeQQNRF6trQeuN4tfYHvwMBFbPoMtMgUL2FdX4MDd0XLm-PdCQLM-mZunRByy9tI
```

**SI CETTE CLÉ EST DIFFÉRENTE** de celle dans Firebase Console, il faut la remplacer dans :

1. **`mobile-app/src/views/MapView.vue`** (ligne ~295)
2. **`mobile-app/src/App.vue`** (si elle y est)
3. **`mobile-app/public/test-fcm.html`** (si elle y est)

### Comment Remplacer

#### Option 1 : Recherche et Remplacement Global

Dans VS Code :
1. Appuyer sur **Ctrl + Shift + H** (Rechercher et Remplacer dans tous les fichiers)
2. Dans "Rechercher" : Coller l'ancienne clé
3. Dans "Remplacer" : Coller la nouvelle clé depuis Firebase Console
4. Cliquer sur "Remplacer tout"

#### Option 2 : Manuel

Ouvrir `MapView.vue` et trouver :
```typescript
vapidKey: 'BMjmtEyox-Cq7673l2i68KbFeQQNRF6trQeuN4tfYHvwMBFbPoMtMgUL2FdX4MDd0XLm-PdCQLM-mZunRByy9tI'
```

Remplacer par la nouvelle clé :
```typescript
vapidKey: 'VOTRE_NOUVELLE_CLE_ICI'
```

---

## ✅ ÉTAPE 4 : Tester

### 1. Redémarrer le Serveur
```powershell
# Arrêter le serveur (Ctrl + C)
npm run dev
```

### 2. Nettoyer le Navigateur
Dans la console du navigateur (F12) :
```javascript
// Désinscrire tous les SW
navigator.serviceWorker.getRegistrations().then(regs => {
  regs.forEach(r => r.unregister());
});

// Recharger
location.reload();
```

### 3. Test Complet
1. Ouvrir http://localhost:5173
2. Recharger (F5) 2 fois
3. Se connecter
4. Vérifier les logs

### Logs Attendus (Succès)
```
[config.ts] Firebase Messaging initialisé avec succès
Début requestFcmToken...
Permission notification: granted
Tentative de récupération du token FCM...
✅ FCM Token reçu: dXbY9Q8R7pM:APA91bH...
Token FCM enregistré avec succès dans Firestore
```

---

## 🔍 Alternative : Test Sans Clé VAPID

Si vous voulez juste vérifier que l'API fonctionne, créez un fichier de test :

### `test-vapid.html` dans `public/`

```html
<!DOCTYPE html>
<html>
<head>
    <title>Test Clé VAPID</title>
</head>
<body>
    <h1>Test Clé VAPID</h1>
    <button onclick="testVapid()">Tester la Clé</button>
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

        window.testVapid = async () => {
            const result = document.getElementById('result');
            result.textContent = 'Test en cours...\n';

            try {
                // Enregistrer SW
                await navigator.serviceWorker.register('/firebase-messaging-sw.js');
                await navigator.serviceWorker.ready;
                result.textContent += '✅ Service Worker prêt\n';

                // Permission
                const perm = await Notification.requestPermission();
                result.textContent += `Permission: ${perm}\n`;

                if (perm !== 'granted') {
                    result.textContent += '❌ Permission refusée\n';
                    return;
                }

                // Tester SANS VAPID (utilisera celle par défaut)
                const messaging = getMessaging(app);
                result.textContent += '✅ Messaging initialisé\n';

                // Si vous avez la clé VAPID depuis Firebase Console, la mettre ici
                const VAPID_KEY = 'COLLEZ_ICI_LA_CLE_DEPUIS_FIREBASE_CONSOLE';
                
                const token = await getToken(messaging, {
                    vapidKey: VAPID_KEY
                });

                result.textContent += `✅ TOKEN: ${token}\n`;
            } catch (error) {
                result.textContent += `❌ ERREUR: ${error.message}\n`;
                console.error(error);
            }
        };
    </script>
</body>
</html>
```

Puis ouvrir : http://localhost:5173/test-vapid.html

---

## 📊 Vérification Firebase Console

### Checklist Complète

- [ ] Projet existe : `projet-cloud-s5-routier`
- [ ] Clé VAPID générée (Paramètres > Cloud Messaging)
- [ ] API activée (Google Cloud Console > APIs & Services)
- [ ] Clé copiée exactement (pas d'espace, pas de retour à la ligne)
- [ ] Clé remplacée dans tous les fichiers
- [ ] Serveur redémarré
- [ ] Navigateur nettoyé

---

## 🆘 Si Rien Ne Marche

### Option : Désactiver Temporairement FCM

Si vous voulez juste que l'application fonctionne sans notifications :

Dans `MapView.vue`, remplacer `requestFcmToken` par une version vide :

```typescript
const requestFcmToken = async (userDocRef: any) => {
  console.log('FCM désactivé temporairement');
  await updateDoc(userDocRef, {
    fcmToken: null,
    fcmTokenStatus: 'disabled',
    fcmTokenError: 'FCM désactivé manuellement',
    fcmTokenDate: new Date().toISOString()
  });
};
```

L'application fonctionnera parfaitement sans notifications push.

---

## 📞 Support Supplémentaire

Si après avoir :
1. ✅ Vérifié la clé VAPID sur Firebase Console
2. ✅ Activé l'API Cloud Messaging
3. ✅ Remplacé la clé dans le code
4. ✅ Redémarré le serveur
5. ✅ Nettoyé le navigateur

... et que ça ne marche toujours pas, **partager** :
- La nouvelle clé VAPID (première et dernière partie : `BMjmt...9tI`)
- Capture d'écran de Firebase Console (Cloud Messaging)
- Capture d'écran de Google Cloud Console (API activée)
- Logs complets de la console

---

**Date :** 6 février 2026  
**Prochaine étape :** Vérifier la clé VAPID sur Firebase Console
