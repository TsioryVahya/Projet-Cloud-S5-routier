# Activation de l'API Firebase Cloud Messaging

## ✅ Clé VAPID Confirmée Valide

Vous avez confirmé que la clé VAPID n'est pas expirée. Le problème vient donc de la **configuration Firebase Cloud Messaging API**.

---

## 🔧 SOLUTION : Activer l'API Firebase Cloud Messaging

### Étape 1 : Google Cloud Console

1. **Ouvrir** : https://console.cloud.google.com

2. **Sélectionner le projet** :
   - En haut de la page, cliquer sur le sélecteur de projet
   - Chercher et sélectionner : **`projet-cloud-s5-routier`**

3. **Aller dans le menu des APIs** :
   - Cliquer sur le menu hamburger ☰ (en haut à gauche)
   - **APIs & Services** > **Library**

4. **Chercher l'API** :
   - Dans la barre de recherche : `Firebase Cloud Messaging API`
   - OU : `FCM API`
   - Cliquer sur le résultat

5. **Activer l'API** :
   - Si vous voyez un bouton bleu **"ACTIVER"** → Cliquer dessus
   - Si vous voyez **"GÉRER"** → L'API est déjà activée ✅
   - Attendre quelques secondes pour l'activation

6. **Vérifier l'activation** :
   - Retourner dans **APIs & Services** > **Dashboard**
   - Vous devriez voir **"Firebase Cloud Messaging API"** dans la liste des APIs activées

---

## 🔍 Vérification Supplémentaire : Legacy Cloud Messaging

Firebase a 2 versions d'API. Vérifiez aussi l'ancienne version :

1. Dans **APIs & Services** > **Library**
2. Chercher : **`Cloud Messaging`** (sans "Firebase")
3. Vous devriez voir :
   - **Firebase Cloud Messaging API** ← La nouvelle (celle à activer)
   - **Cloud Messaging (Legacy)** ← L'ancienne (peut être nécessaire)

4. Activer **les deux** si possible

---

## ⚙️ Configuration Firebase Console

### Vérifier la Configuration du Projet

1. **Ouvrir** : https://console.firebase.google.com

2. **Projet** : `projet-cloud-s5-routier`

3. **Cloud Messaging** :
   - Paramètres ⚙️ > Cloud Messaging
   - Vérifier que vous voyez :
     - **Sender ID** : `792049548362` ✅
     - **Server key** : Une longue clé commençant par `AAAA...`
     - **Certificats de clés Web push** : Votre clé VAPID `BMjmt...`

4. **Si l'onglet Cloud Messaging est vide** :
   - Cela confirme que l'API n'est pas activée
   - Retourner sur Google Cloud Console et activer l'API

---

## 🧪 Test Après Activation

### 1. Attendre 2-3 Minutes

Après activation de l'API, attendre quelques minutes pour que les changements se propagent.

### 2. Nettoyer le Navigateur

```javascript
// Dans la console du navigateur (F12)
navigator.serviceWorker.getRegistrations().then(regs => {
  regs.forEach(r => r.unregister());
  console.log('✅ Service Workers désinscrits');
});

// Recharger
location.reload();
```

### 3. Redémarrer le Serveur

```powershell
# Terminal
# Ctrl + C pour arrêter
npm run dev
```

### 4. Test Complet

1. Ouvrir http://localhost:5173
2. Recharger (F5) **2 fois**
3. Se connecter avec `tendryniavo76@gmail.com`

### 5. Vérifier les Logs

**Succès attendu :**
```
[config.ts] Service Worker en contrôle, initialisation du messaging
[config.ts] Firebase Messaging initialisé avec succès
Début requestFcmToken...
Permission notification: granted
Tentative de récupération du token FCM...
✅ FCM Token reçu: dXbY9Q8R7pM:APA91bH...
Token FCM enregistré avec succès dans Firestore
```

### 6. Vérifier Firestore

```json
{
  "fcmToken": "dXbY9Q8R7pM:APA91bH...",
  "fcmTokenStatus": "active",
  "fcmTokenDate": "2026-02-06T18:00:00.000Z"
}
```

---

## 🔍 Diagnostic Supplémentaire

Si l'erreur persiste après activation de l'API, tester avec ce code :

### Test Direct de l'API

Créer `test-fcm-api.html` dans `public/` :

```html
<!DOCTYPE html>
<html>
<head>
    <title>Test API FCM</title>
</head>
<body>
    <h1>Test API Firebase Cloud Messaging</h1>
    <button onclick="testAPI()">Tester l'API</button>
    <pre id="result"></pre>

    <script type="module">
        window.testAPI = async () => {
            const result = document.getElementById('result');
            result.textContent = 'Test en cours...\n\n';

            try {
                // 1. Vérifier que l'API est accessible
                result.textContent += '1. Test d\'accès à Firebase...\n';
                
                const testResponse = await fetch('https://fcm.googleapis.com/fcm/send', {
                    method: 'GET',
                    headers: {
                        'Authorization': 'key=YOUR_SERVER_KEY_HERE'
                    }
                });
                
                result.textContent += `   Status: ${testResponse.status}\n`;
                result.textContent += `   ${testResponse.status === 401 ? '✅ API accessible (401 attendu sans clé valide)' : '⚠️ Réponse inattendue'}\n\n`;

                // 2. Tester avec Firebase SDK
                result.textContent += '2. Test Firebase SDK...\n';
                
                const { initializeApp } = await import('https://www.gstatic.com/firebasejs/10.0.0/firebase-app.js');
                const { getMessaging, getToken } = await import('https://www.gstatic.com/firebasejs/10.0.0/firebase-messaging.js');

                const app = initializeApp({
                    apiKey: "AIzaSyBroRMMRCSVdqAzpuivp7PSSP9X1WIk3VY",
                    projectId: "projet-cloud-s5-routier",
                    messagingSenderId: "792049548362",
                    appId: "1:792049548362:web:6ab3ce65b1584730c63ab3"
                });

                result.textContent += '   ✅ Firebase initialisé\n\n';

                // 3. Service Worker
                result.textContent += '3. Service Worker...\n';
                await navigator.serviceWorker.register('/firebase-messaging-sw.js');
                const reg = await navigator.serviceWorker.ready;
                result.textContent += '   ✅ Service Worker prêt\n\n';

                // 4. Permission
                result.textContent += '4. Permission notification...\n';
                const perm = await Notification.requestPermission();
                result.textContent += `   Permission: ${perm}\n\n`;

                if (perm !== 'granted') {
                    result.textContent += '   ❌ Veuillez autoriser les notifications\n';
                    return;
                }

                // 5. Token FCM
                result.textContent += '5. Récupération du token FCM...\n';
                const messaging = getMessaging(app);
                
                const token = await getToken(messaging, {
                    vapidKey: 'BMjmtEyox-Cq7673l2i68KbFeQQNRF6trQeuN4tfYHvwMBFbPoMtMgUL2FdX4MDd0XLm-PdCQLM-mZunRByy9tI',
                    serviceWorkerRegistration: reg
                });

                result.textContent += `   ✅ TOKEN REÇU:\n   ${token}\n\n`;
                result.textContent += '========================================\n';
                result.textContent += '✅ TOUT FONCTIONNE !\n';
                result.textContent += '========================================\n';

            } catch (error) {
                result.textContent += `\n❌ ERREUR:\n`;
                result.textContent += `   Message: ${error.message}\n`;
                result.textContent += `   Code: ${error.code || 'N/A'}\n`;
                result.textContent += `   Name: ${error.name}\n`;
                console.error('Détails complets:', error);
            }
        };
    </script>
</body>
</html>
```

Puis ouvrir : http://localhost:5173/test-fcm-api.html

---

## 📊 Checklist Complète

- [ ] API activée sur Google Cloud Console
- [ ] Projet correct sélectionné (`projet-cloud-s5-routier`)
- [ ] Clé VAPID présente sur Firebase Console
- [ ] Sender ID correct : `792049548362`
- [ ] Service Worker enregistré et actif
- [ ] Permission notification accordée
- [ ] Navigateur à jour (Chrome/Firefox/Edge)
- [ ] Pas de bloqueur de pub/VPN qui interfère
- [ ] Attente de 2-3 minutes après activation API

---

## 🆘 Si Toujours Pas Résolu

### Option 1 : Vérifier les Quotas

Sur Google Cloud Console :
- **IAM & Admin** > **Quotas**
- Chercher "Cloud Messaging"
- Vérifier qu'il n'y a pas de limite atteinte

### Option 2 : Vérifier la Facturation

Parfois, l'API nécessite un compte avec facturation activée :
- Google Cloud Console > **Billing**
- Vérifier qu'un compte de facturation est lié au projet

### Option 3 : Créer une Nouvelle Clé VAPID

1. Firebase Console > Cloud Messaging
2. Dans "Certificats de clés Web push"
3. Supprimer l'ancienne clé (si possible)
4. Générer une nouvelle avec "Generate key pair"
5. Remplacer dans le code

---

## 📞 Informations de Debug

Si le problème persiste, noter :

1. **Capture d'écran** de :
   - Firebase Console > Cloud Messaging
   - Google Cloud Console > APIs activées

2. **Logs complets** de la console navigateur

3. **Vérification** :
   ```javascript
   // Dans la console
   console.log('Project ID:', 'projet-cloud-s5-routier');
   console.log('Sender ID:', '792049548362');
   console.log('VAPID Key:', 'BMjmt...' + '(première partie)');
   ```

---

**Date :** 6 février 2026  
**Priorité :** Activer l'API Firebase Cloud Messaging sur Google Cloud Console  
**Temps estimé :** 5-10 minutes (avec propagation)
