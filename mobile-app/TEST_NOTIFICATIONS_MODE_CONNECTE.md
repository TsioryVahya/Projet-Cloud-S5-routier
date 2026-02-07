# 🧪 Guide de Test - Notifications Mode Connecté Uniquement

## ✅ Modifications Apportées

Le système a été corrigé pour que **les notifications n'arrivent QUE quand l'utilisateur est connecté**.

### Vérifications Ajoutées

1. **Listener Firestore** : Vérifie `store.user` avant d'afficher le toast
2. **Listener FCM Foreground** : Vérifie `store.user` avant d'afficher le toast  
3. **Désactivation à la déconnexion** : Le listener Firestore est complètement désactivé
4. **Double protection** : Même si le listener reçoit un événement, il ne l'affiche pas si déconnecté

---

## 🧪 Scénarios de Test

### Test 1 : Notification en Mode Connecté (✅ Doit fonctionner)

1. **Connectez-vous** sur l'app mobile
2. **Ouvrez la console** (F12)
3. **Vérifiez les logs** :
   ```
   🔔 Configuration du listener temps réel pour: user@example.com
   ✅ Listener notifications activé
   ✅ Listener FCM foreground configuré
   ```
4. **Sur le backoffice** : Changez le statut d'un signalement
5. **Résultat attendu** :
   - ✅ Toast s'affiche immédiatement
   - ✅ Logs : `📩 Nouvelle notification détectée`

---

### Test 2 : Notification en Mode Visiteur (❌ Ne doit PAS fonctionner)

1. **Connectez-vous** sur l'app mobile
2. **Déconnectez-vous** (bouton logout)
3. **Vérifiez les logs** :
   ```
   🚪 Déconnexion en cours...
   🔕 Désactivation du listener Firestore
   ✅ Listener Firestore désactivé
   ✅ Déconnexion terminée - Mode visiteur activé
   ```
4. **Sur le backoffice** : Changez le statut d'un signalement
5. **Résultat attendu** :
   - ❌ **AUCUN toast ne s'affiche**
   - ❌ Pas de log `📩 Nouvelle notification détectée`
   - ℹ️ Possiblement : `⚠️ Notification FCM reçue mais utilisateur non connecté, ignorée`

---

### Test 3 : Reconnexion après Notification Manquée

1. **Mode visiteur** (déconnecté)
2. **Sur le backoffice** : Changez le statut → Notification créée dans Firestore
3. **Reconnectez-vous** sur l'app mobile
4. **Résultat attendu** :
   - ✅ Fonction `checkUnreadNotifications()` s'exécute
   - ✅ Toast s'affiche avec la notification manquée
   - ✅ Log : `📬 X notification(s) non lue(s) trouvée(s)`

---

## 🔍 Logs à Surveiller

### Au Montage de l'App
```
✅ Listener FCM foreground configuré (attente connexion)
```

### À la Connexion
```
🔔 Vérification des notifications non lues pour: user@example.com
🔔 Configuration du listener temps réel pour: user@example.com
✅ Listener notifications activé
```

### À la Déconnexion
```
🚪 Déconnexion en cours...
🔕 Désactivation du listener Firestore
✅ Listener Firestore désactivé
✅ Déconnexion terminée - Mode visiteur activé
```

### Si Notification Arrive en Mode Visiteur
```
⚠️ Notification FCM reçue mais utilisateur non connecté, ignorée
```

OU (si le listener n'a pas été correctement désactivé)
```
⚠️ Snapshot reçu mais utilisateur déconnecté - Ne rien faire
```

---

## 🐛 Si les Notifications Arrivent Toujours en Mode Visiteur

### Cas 1 : Le listener Firestore n'est pas désactivé

**Symptôme** : Logs montrent `📬 Snapshot reçu` après déconnexion

**Solution** : Vérifier que `handleLogout()` est bien appelé lors de la déconnexion

```typescript
// Dans le bouton de déconnexion
@click="handleLogout"
```

---

### Cas 2 : Les notifications FCM arrivent via Service Worker

**Symptôme** : Notification système Android/iOS s'affiche même déconnecté

**Cause** : Le Service Worker ne sait pas que l'utilisateur est déconnecté

**Solution** : Modifier `firebase-messaging-sw.js` pour vérifier le token :

```javascript
messaging.onBackgroundMessage((payload) => {
  // Vérifier si l'utilisateur est connecté (via localStorage)
  // Mais attention : le Service Worker n'a pas accès direct au localStorage du contexte principal
  
  // Solution simple : Toujours afficher mais avec un message générique
  // L'utilisateur verra la notification mais ne pourra pas agir dessus s'il est déconnecté
});
```

**Note** : C'est **normal** que les notifications background arrivent même déconnecté car :
- Le Service Worker est indépendant de l'état de l'app
- Le token FCM reste valide même après déconnexion
- C'est le comportement standard de FCM

**Recommandation** : Si vous voulez vraiment bloquer les notifications en mode visiteur, le **backend doit supprimer le token FCM** lors de la déconnexion :

```typescript
// Dans handleLogout()
const handleLogout = async () => {
  // Supprimer le token FCM côté backend
  if (store.user && store.user.postgresId) {
    const userDocRef = doc(db, 'utilisateurs', store.user.postgresId);
    await updateDoc(userDocRef, {
      fcmToken: null,
      fcmTokenStatus: 'logged_out'
    });
  }
  
  // ... reste du code
};
```

---

## 📊 Tableau Comparatif

| Situation | Avant | Après |
|-----------|-------|-------|
| **Connecté + Notification** | ✅ Toast | ✅ Toast |
| **Déconnecté + Notification Foreground** | ✅ Toast (BUG) | ❌ Rien |
| **Déconnecté + Notification Background** | ✅ Notif système | ✅ Notif système* |
| **Reconnexion** | ❌ Rien | ✅ Anciennes notifs affichées |

\* *Normal : FCM ne peut pas être désactivé côté client, le token reste valide*

---

## 💡 Solution Complète (Recommandée)

Pour **vraiment** bloquer toutes les notifications en mode visiteur :

1. ✅ **Frontend** : Vérifier `store.user` (déjà fait)
2. ✅ **Firestore listener** : Désactiver à la déconnexion (déjà fait)
3. 🔧 **Backend** : Supprimer le `fcmToken` lors de la déconnexion

Implémentation complète :

```typescript
const handleLogout = async () => {
  console.log('🚪 Déconnexion en cours...');
  
  // 1. Supprimer le token FCM côté Firestore
  if (store.user && store.user.postgresId) {
    try {
      const userDocRef = doc(db, 'utilisateurs', store.user.postgresId);
      await updateDoc(userDocRef, {
        fcmToken: null,
        fcmTokenStatus: 'logged_out',
        fcmTokenDate: new Date().toISOString()
      });
      console.log('✅ Token FCM supprimé côté backend');
    } catch (err) {
      console.error('❌ Erreur suppression token:', err);
    }
  }
  
  // 2. Désactiver le listener Firestore
  if (unsubscribeNotifications) {
    console.log('🔕 Désactivation du listener Firestore');
    unsubscribeNotifications();
    unsubscribeNotifications = null;
  }
  
  // 3. Déconnexion locale
  setUser(null);
  localStorage.removeItem('app_user');
  filterMine.value = false;
  
  console.log('✅ Déconnexion complète - Aucune notification ne sera reçue');
};
```

---

**Date** : 07 Février 2026  
**Version** : 2.1 (Mode Connecté Uniquement)
