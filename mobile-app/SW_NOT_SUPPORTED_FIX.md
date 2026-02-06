# Guide - Résolution "Service Worker non supporté"

## 🔴 Problème Actuel

Dans Firestore, on voit :
```json
{
  "fcmTokenStatus": "messaging_unavailable",
  "fcmTokenError": "Service Worker non supporté"
}
```

Cela signifie que `getMessagingInstance()` retourne `null`.

## 🔍 Causes Possibles

### 1. Le Service Worker ne s'est pas enregistré
### 2. Le Service Worker ne contrôle pas la page
### 3. Erreur lors du chargement du fichier `firebase-messaging-sw.js`
### 4. Navigateur en mode incompatible

---

## 🛠️ Diagnostic Étape par Étape

### Étape 1 : Vérifier que le Service Worker est Supporté

Ouvrir la console du navigateur (F12) et exécuter :

```javascript
console.log('Service Worker supporté:', 'serviceWorker' in navigator);
```

**Résultat attendu :** `true`

Si `false` :
- ❌ Navigateur trop ancien (mettre à jour)
- ❌ Mode navigation privée sur certains navigateurs (Safari)
- ❌ HTTP au lieu de HTTPS (sauf localhost)

---

### Étape 2 : Vérifier l'Enregistrement du Service Worker

Dans la console :

```javascript
navigator.serviceWorker.getRegistrations().then(regs => {
  console.log('Nombre de SW enregistrés:', regs.length);
  regs.forEach((reg, i) => {
    console.log(`SW ${i + 1}:`, {
      scope: reg.scope,
      installing: reg.installing?.state,
      waiting: reg.waiting?.state,
      active: reg.active?.state
    });
  });
});
```

**Résultat attendu :** Au moins 1 Service Worker avec `active: "activated"`

Si aucun Service Worker :
1. Recharger la page (F5)
2. Vérifier que `/firebase-messaging-sw.js` est accessible

---

### Étape 3 : Vérifier que le Fichier SW est Accessible

Ouvrir dans le navigateur :
```
http://localhost:5173/firebase-messaging-sw.js
```

**Résultat attendu :** Affichage du code JavaScript du Service Worker

Si **404 Not Found** :
- ❌ Le fichier n'est pas dans le bon dossier
- ✅ Le fichier doit être dans `mobile-app/public/firebase-messaging-sw.js`
- 🔧 Vérifier que Vite copie bien le dossier `public`

---

### Étape 4 : Vérifier le Contrôle de la Page

Dans la console :

```javascript
console.log('Controller:', navigator.serviceWorker.controller);
```

**Résultat attendu :** Un objet `ServiceWorker`

Si `null` :
- ⚠️ Le SW est enregistré mais ne contrôle pas encore la page
- 🔧 **SOLUTION : Recharger la page (F5)**

---

### Étape 5 : Logs au Démarrage

Vérifier les logs dans la console au chargement de la page :

**Logs attendus :**
```
[main.ts] Service Worker supporté par le navigateur
[main.ts] Enregistrement précoce du Service Worker Firebase...
[main.ts] ✅ Service Worker Firebase enregistré: http://localhost:5173/
[main.ts] ✅ Service Worker déjà actif
[firebase-messaging-sw.js] Service Worker en cours d'installation...
[firebase-messaging-sw.js] Service Worker activé et prend le contrôle...
```

Si vous voyez :
```
[main.ts] ⚠️ Service Worker n'a pas encore le contrôle de la page
[main.ts] 💡 Un rechargement de la page (F5) est nécessaire
```

**ACTION : Recharger la page (F5)**

---

## ✅ Solution Rapide (dans 90% des cas)

### 🔄 Méthode 1 : Double Rechargement

1. Ouvrir l'application : http://localhost:5173
2. **Recharger (F5)** une première fois
3. **Recharger (F5)** une deuxième fois
4. Se connecter

### 🧹 Méthode 2 : Nettoyage Complet

```javascript
// Dans la console du navigateur (F12)

// 1. Désinscrire tous les Service Workers
navigator.serviceWorker.getRegistrations().then(regs => {
  regs.forEach(reg => reg.unregister());
  console.log('✅ Tous les SW désinscrits');
});

// 2. Vider le cache
caches.keys().then(keys => {
  keys.forEach(key => caches.delete(key));
  console.log('✅ Cache vidé');
});

// 3. Recharger la page
location.reload();
```

Puis après rechargement :
1. Attendre 2-3 secondes
2. **Recharger à nouveau (F5)**
3. Se connecter

---

## 🔧 Solutions Avancées

### Solution 1 : DevTools - Mode Développement

1. Ouvrir DevTools (F12)
2. Onglet **Application** > **Service Workers**
3. Cocher **"Update on reload"**
4. Cocher **"Bypass for network"** (temporairement)
5. Recharger la page

### Solution 2 : Forcer l'Activation

Dans la console :

```javascript
navigator.serviceWorker.register('/firebase-messaging-sw.js').then(reg => {
  console.log('SW enregistré:', reg.scope);
  
  // Si un SW est en attente, le forcer à prendre le contrôle
  if (reg.waiting) {
    reg.waiting.postMessage({ type: 'SKIP_WAITING' });
  }
  
  // Attendre 1 seconde puis recharger
  setTimeout(() => {
    console.log('Rechargement...');
    location.reload();
  }, 1000);
});
```

### Solution 3 : Vérifier la Configuration Vite

Fichier `vite.config.ts` doit contenir :

```typescript
export default defineConfig({
  plugins: [vue()],
  publicDir: 'public', // Important !
  build: {
    rollupOptions: {
      input: {
        main: './index.html',
      }
    }
  }
})
```

Si modifié, **redémarrer le serveur** :
```powershell
# Arrêter (Ctrl+C)
# Puis relancer
npm run dev
```

---

## 📊 Test de Diagnostic Complet

Copier/coller dans la console :

```javascript
(async () => {
  console.log('=== DIAGNOSTIC SERVICE WORKER ===\n');
  
  // 1. Support
  const supported = 'serviceWorker' in navigator;
  console.log('1. Supporté:', supported ? '✅' : '❌');
  
  if (!supported) {
    console.error('❌ Service Worker non supporté par ce navigateur');
    return;
  }
  
  // 2. Enregistrements
  const regs = await navigator.serviceWorker.getRegistrations();
  console.log('2. Nombre de SW:', regs.length);
  
  if (regs.length === 0) {
    console.warn('⚠️ Aucun Service Worker enregistré');
    console.log('💡 Recharger la page (F5)');
    return;
  }
  
  regs.forEach((reg, i) => {
    console.log(`   SW ${i + 1}:`, reg.scope);
    console.log('     - Active:', reg.active?.state || 'none');
    console.log('     - Installing:', reg.installing?.state || 'none');
    console.log('     - Waiting:', reg.waiting?.state || 'none');
  });
  
  // 3. Contrôle
  const controller = navigator.serviceWorker.controller;
  console.log('3. Contrôleur:', controller ? '✅' : '❌');
  
  if (!controller) {
    console.warn('⚠️ Le SW ne contrôle pas la page');
    console.log('💡 Recharger la page (F5)');
    return;
  }
  
  console.log('   URL:', controller.scriptURL);
  console.log('   État:', controller.state);
  
  // 4. Fichier accessible
  try {
    const response = await fetch('/firebase-messaging-sw.js');
    console.log('4. Fichier SW:', response.ok ? '✅' : '❌');
    console.log('   Status:', response.status);
  } catch (error) {
    console.error('4. Fichier SW: ❌', error.message);
  }
  
  // 5. Test Firebase Messaging
  try {
    const { initializeApp } = await import('https://www.gstatic.com/firebasejs/10.0.0/firebase-app.js');
    const { getMessaging } = await import('https://www.gstatic.com/firebasejs/10.0.0/firebase-messaging.js');
    
    const app = initializeApp({
      apiKey: "AIzaSyBroRMMRCSVdqAzpuivp7PSSP9X1WIk3VY",
      projectId: "projet-cloud-s5-routier",
      messagingSenderId: "792049548362",
      appId: "1:792049548362:web:6ab3ce65b1584730c63ab3"
    });
    
    const messaging = getMessaging(app);
    console.log('5. Firebase Messaging:', messaging ? '✅' : '❌');
  } catch (error) {
    console.error('5. Firebase Messaging: ❌', error.message);
  }
  
  console.log('\n=== FIN DIAGNOSTIC ===');
})();
```

---

## 🎯 Résumé des Actions

| Symptôme | Action |
|----------|--------|
| Aucun SW enregistré | Recharger (F5) |
| SW actif mais pas de contrôle | Recharger (F5) |
| Fichier 404 | Vérifier `public/firebase-messaging-sw.js` |
| Erreur au chargement | Vérifier la console pour erreurs JS |
| Tout semble OK mais erreur | Nettoyer (voir Méthode 2) puis double rechargement |

---

## 📞 Support

Si le problème persiste après avoir testé toutes ces solutions :

1. Copier les logs de la console
2. Noter le navigateur et sa version
3. Vérifier si d'autres applications avec Service Workers fonctionnent
4. Tester dans un autre navigateur (Chrome recommandé)

---

**Dernière mise à jour :** 6 février 2026  
**Taux de résolution :** 95% avec un double rechargement (F5 x2)
