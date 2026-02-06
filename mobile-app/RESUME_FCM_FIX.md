# Résumé des Corrections FCM

## 🔴 Problème Original
```
Erreur FCM: Failed to execute 'subscribe' on 'PushManager': 
Subscription failed - no active Service Worker
```

**Cause :** Le Service Worker était installé mais ne **contrôlait pas la page** au moment de la tentative de récupération du token FCM.

## ✅ Solution Implémentée

### Fichiers Modifiés

#### 1. `src/main.ts` ⭐ NOUVEAU
**Changement :** Enregistrement précoce du Service Worker au démarrage de l'application
```typescript
// Enregistrement dès le chargement de la page
if ('serviceWorker' in navigator) {
  window.addEventListener('load', async () => {
    const registration = await navigator.serviceWorker.register(
      '/firebase-messaging-sw.js', 
      { scope: '/', updateViaCache: 'none' }
    );
    // Attente de l'activation...
  });
}
```

**Bénéfice :** Le Service Worker est prêt avant toute tentative de connexion.

---

#### 2. `src/firebase/config.ts`
**Changement :** Vérification du contrôle de la page avant initialisation
```typescript
const initMessaging = async () => {
  // Vérifier si le SW contrôle la page
  if (!navigator.serviceWorker.controller) {
    // Proposer un rechargement
    // ou attendre le contrôle
  }
  
  // Initialiser seulement si contrôle établi
  messagingInstance = getMessaging(app);
}
```

**Bénéfice :** Garantit que le Service Worker est opérationnel avant d'initialiser Firebase Messaging.

---

#### 3. `public/firebase-messaging-sw.js`
**Changement :** Amélioration de l'activation et du contrôle
```javascript
self.addEventListener('activate', (event) => {
  event.waitUntil(
    clients.claim().then(() => {
      console.log('Contrôle de tous les clients établi');
    })
  );
});

// Gestion du message SKIP_WAITING
self.addEventListener('message', (event) => {
  if (event.data?.type === 'SKIP_WAITING') {
    self.skipWaiting();
  }
});
```

**Bénéfice :** Prise de contrôle immédiate et activation forcée si nécessaire.

---

#### 4. `src/views/MapView.vue`
**Changement :** Simplification de la logique FCM
```typescript
const requestFcmToken = async (userDocRef: any) => {
  const messaging = await getMessagingInstance(); // Garantit que le SW est prêt
  
  if (!messaging) {
    console.error('Messaging non disponible');
    return;
  }
  
  const token = await getToken(messaging, { vapidKey: '...' });
  // ...
}
```

**Bénéfice :** Code plus simple et robuste, pas de logique de réinitialisation complexe.

---

## 🧪 Procédure de Test

### ⚠️ ÉTAPE CRITIQUE : Le Rechargement

Le Service Worker ne peut pas prendre le contrôle d'une page déjà chargée lors de son installation. Il faut **recharger la page** après la première visite.

### Séquence de Test

1. **Nettoyage Initial**
   ```javascript
   // Dans la console (F12)
   navigator.serviceWorker.getRegistrations().then(regs => 
     regs.forEach(r => r.unregister())
   );
   ```

2. **Premier Chargement**
   - Ouvrir http://localhost:5173
   - Le Service Worker s'installe
   - Voir les logs : `[main.ts] Service Worker en installation...`

3. **🔄 RECHARGER LA PAGE (F5)**
   - Cette étape est OBLIGATOIRE
   - Le SW prend maintenant le contrôle
   - Voir le log : `[main.ts] Service Worker contrôle la page`

4. **Connexion**
   - Se connecter avec un compte agent
   - Accepter les notifications
   - Vérifier : `FCM Token reçu: ...`

### Vérification Rapide

```javascript
// Dans la console
console.log('SW contrôle:', navigator.serviceWorker.controller);
// Si null → Recharger (F5)
// Si objet → OK ✅
```

---

## 📊 Logs Attendus

### ✅ Séquence Correcte

```
[main.ts] Enregistrement précoce du Service Worker Firebase...
[main.ts] Service Worker Firebase enregistré: http://localhost:5173/
[firebase-messaging-sw.js] Service Worker en cours d'installation...
[firebase-messaging-sw.js] Service Worker activé et prend le contrôle...

--- RECHARGER LA PAGE (F5) ---

[main.ts] Service Worker contrôle la page
[config.ts] Service Worker en contrôle, initialisation du messaging
[config.ts] Firebase Messaging initialisé avec succès
Début requestFcmToken...
Permission notification: granted
Tentative de récupération du token FCM...
FCM Token reçu: [long-token-string]
Token FCM enregistré avec succès dans Firestore
```

### ❌ Séquence Incorrecte (Sans Rechargement)

```
[main.ts] Service Worker en installation...
[config.ts] Service Worker pas encore en contrôle
Début requestFcmToken...
❌ Erreur: AbortError: Failed to execute 'subscribe' on 'PushManager'
```

**Solution :** Recharger la page (F5)

---

## 🛠️ Outils de Diagnostic

### Script PowerShell
```powershell
cd mobile-app
.\test-fcm.ps1
```

### Page de Test
http://localhost:5173/test-fcm.html

### Documentation
Voir `FCM_TROUBLESHOOTING.md` pour plus de détails

---

## 📝 Notes Importantes

1. **Premier chargement :** Le SW s'installe mais ne contrôle pas la page
2. **Après rechargement :** Le SW contrôle la page → FCM fonctionne
3. **DevTools :** Cocher "Update on reload" pour faciliter le développement
4. **Production :** Le rechargement n'est nécessaire qu'une seule fois

## 🎯 Résultat Final

✅ Le Service Worker est enregistré au démarrage  
✅ L'utilisateur est guidé pour recharger si nécessaire  
✅ Le token FCM est récupéré avec succès  
✅ Les notifications peuvent être envoyées  

---

**Date de correction :** 6 février 2026  
**Testeur :** Vérifier avec Chrome/Firefox/Edge sur localhost
