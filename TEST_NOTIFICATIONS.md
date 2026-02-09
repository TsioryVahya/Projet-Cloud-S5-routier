# 🧪 Test Manuel des Notifications

## Créer une notification de test dans Firestore

### Via Firebase Console (Méthode la plus simple)

1. Ouvrez [Firebase Console](https://console.firebase.google.com)
2. Sélectionnez le projet **`projet-cloud-s5-routier`**
3. Allez dans **Firestore Database**
4. Cliquez sur **"Start collection"** (ou ouvrez la collection `notifications` si elle existe)
5. ID de collection : `notifications`
6. Cliquez sur **"Auto-ID"** pour le document
7. Ajoutez ces champs :

```
userId: wkxCvD2K6NYgp5WdAx0VQDcYGky1
signalementId: test-123
titre: Test de notification
message: Votre signalement est maintenant "en cours"
type: status_change
oldStatus: nouveau
newStatus: en cours
dateCreation: [Cliquez sur le type "timestamp" et sélectionnez "now"]
lu: false
```

8. Cliquez sur **"Save"**

### ✅ Résultat Attendu

Dans la console de votre navigateur, vous devriez voir immédiatement :
```
📬 1 notifications chargées (1 non lues)
```

Et le badge rouge avec "1" apparaîtra sur l'onglet Notifications !

---

## 🔍 Vérifier pourquoi le backend ne crée pas de notifications

Le backend crée des notifications lors du changement de statut SEULEMENT si :

### 1. Le signalement a un `idFirebase`
- Le signalement doit avoir été créé via l'app mobile
- OU avoir un champ `id_firebase` dans PostgreSQL

### 2. Le signalement a un utilisateur
- Avec un email valide
- Qui existe dans la collection Firestore `users`

### 3. Le backend peut trouver l'UID Firebase de l'utilisateur
- Via la méthode `getUserFirebaseId(email)`
- Qui cherche dans Firestore : `users` where `email = "..."`

---

## 🐛 Debug : Vérifier les logs du backend

Ouvrez les logs du backend :

```powershell
docker-compose logs -f backend | Select-String "notification"
```

Cherchez ces messages lors du changement de statut :
```
📬 Préparation notification pour userId=..., signalement=...
✅ Notification FCM envoyée avec succès
✅ Notification enregistrée dans Firestore
```

Si vous ne voyez AUCUN log :
- Le signalement n'a pas d'`idFirebase`
- Ou le signalement n'a pas d'utilisateur associé
- Ou l'utilisateur n'existe pas dans Firestore `users`

---

## 🔧 Solution : Créer un signalement depuis l'app mobile

### Étapes :

1. **Connectez-vous** dans l'app mobile
2. **Créez un signalement** en cliquant sur la carte
3. **Notez l'ID du signalement** (dans la console ou Firestore)
4. **Allez sur le dashboard web** (admin)
5. **Changez le statut** du signalement
6. **Vérifiez** :
   - Les logs backend
   - La console mobile
   - La collection Firestore `notifications`

---

## 📋 Vérification de la Configuration

### 1. Vérifier que l'utilisateur existe dans Firestore

Firebase Console > Firestore > `users` > Cherchez votre UID : `wkxCvD2K6NYgp5WdAx0VQDcYGky1`

Le document doit contenir :
```
email: "tendryniavo76@gmail.com"
```

### 2. Vérifier les signalements

Firebase Console > Firestore > `signalements`

Chaque signalement doit avoir :
```
email: "tendryniavo76@gmail.com"  (ou utilisateur.email)
```

### 3. Vérifier PostgreSQL

Le signalement dans PostgreSQL doit avoir :
- `id_firebase` : ID du document Firestore
- `utilisateur_id` : Lié à l'utilisateur

---

## 🔄 Flux Complet (Ce qui DOIT se passer)

```
1. Admin change statut dans dashboard web
   ↓
2. Backend : SignalementService.modifierSignalement()
   - Détecte : oldStatus ≠ newStatus
   ↓
3. Backend : sendStatusChangeNotification()
   - Récupère email de l'utilisateur
   - Appelle getUserFirebaseId(email)
   ↓
4. Backend : getUserFirebaseId()
   - Query Firestore: users where email = "tendryniavo76@gmail.com"
   - Retourne UID: "wkxCvD2K6NYgp5WdAx0VQDcYGky1"
   ↓
5. Backend : FcmNotificationService.notifyStatusChange()
   - Récupère fcmToken (optionnel)
   - Crée document dans Firestore/notifications
   ↓
6. Mobile : Firestore listener détecte nouveau document
   - Met à jour notifications.value
   - Met à jour unreadCount.value
   - Badge s'affiche
```

---

## 🎯 Test Rapide (Sans Backend)

Pour vérifier que le système mobile fonctionne :

### Script PowerShell pour créer une notification via REST API

```powershell
$projectId = "projet-cloud-s5-routier"
$userId = "wkxCvD2K6NYgp5WdAx0VQDcYGky1"

$notification = @{
    fields = @{
        userId = @{ stringValue = $userId }
        signalementId = @{ stringValue = "test-signal-123" }
        titre = @{ stringValue = "Test de notification" }
        message = @{ stringValue = "Votre signalement est maintenant 'en cours'" }
        type = @{ stringValue = "status_change" }
        oldStatus = @{ stringValue = "nouveau" }
        newStatus = @{ stringValue = "en cours" }
        dateCreation = @{ timestampValue = (Get-Date).ToUniversalTime().ToString("yyyy-MM-ddTHH:mm:ss.fffZ") }
        lu = @{ booleanValue = $false }
    }
} | ConvertTo-Json -Depth 10

# Note: Nécessite authentification Firebase
Write-Host "💡 Utilisez plutôt Firebase Console pour créer la notification manuellement"
Write-Host "   Ou créez un signalement depuis l'app mobile et changez son statut"
```

---

## ✅ Checklist de Débogage

- [ ] L'utilisateur existe dans Firestore `users` avec son email
- [ ] Vous vous êtes connecté dans l'app mobile (UID visible dans les logs)
- [ ] Vous avez créé un signalement depuis l'app mobile (pas le dashboard)
- [ ] Le signalement apparaît dans Firestore `signalements`
- [ ] Le signalement a un champ `email` correspondant à votre compte
- [ ] Vous changez le statut depuis le dashboard web
- [ ] Les logs backend montrent l'envoi de notification
- [ ] La notification apparaît dans Firestore `notifications`
- [ ] Le mobile reçoit la notification (logs + badge)

---

## 💡 Solution Rapide pour Tester MAINTENANT

**Créez manuellement une notification dans Firebase Console** comme décrit en haut de ce document.

Vous devriez voir immédiatement le badge et la notification dans l'app mobile !

Cela confirmera que tout le système frontend fonctionne correctement. ✅
