# 🔍 Guide de Débogage des Notifications

## 📋 Étapes pour Tester

### 1. Redémarrer le Backend

```powershell
cd "D:\ITU\S5\M.Rojo\Final S5\Projet-Cloud-S5-routier"
docker-compose restart backend
```

### 2. Voir les Logs en Temps Réel

```powershell
docker-compose logs -f backend
```

### 3. Créer un Signalement depuis l'App Mobile

1. Ouvrez l'app mobile (localhost:5173)
2. Connectez-vous
3. Cliquez sur la carte pour créer un signalement
4. Remplissez les informations
5. Soumettez

### 4. Changer le Statut depuis le Dashboard Web

1. Ouvrez le dashboard web (localhost:4200)
2. Connectez-vous en tant qu'admin
3. Trouvez le signalement que vous venez de créer
4. Changez son statut (ex: "nouveau" → "en cours")
5. Sauvegardez

### 5. Vérifier les Logs Backend

Vous devriez voir dans les logs :

```
🔔 sendStatusChangeNotification appelé
   - Signalement ID: [uuid]
   - ID Firebase: [firestore-id]
   - Utilisateur: tendryniavo76@gmail.com
   - Changement: nouveau -> en cours
📧 Email utilisateur: tendryniavo76@gmail.com
🔍 Recherche de l'UID Firebase pour l'email: tendryniavo76@gmail.com
✅ UID trouvé: wkxCvD2K6NYgp5WdAx0VQDcYGky1
📤 Envoi de la notification via FcmNotificationService...
🔔 Notification de changement de statut: nouveau -> en cours pour signalement [id] (user: wkxCvD2K...)
✅ Notification enregistrée dans Firestore pour userId=wkxCvD2K...
✅ Notification envoyée avec succès
```

### 6. Vérifier dans l'App Mobile

Dans la console du navigateur (F12) :
```
📬 1 notifications chargées (1 non lues)
```

Et le badge rouge "1" doit apparaître sur l'onglet Notifications !

---

## ❌ Si Ça Ne Fonctionne Pas

### Problème 1 : "Pas d'utilisateur associé au signalement"

**Solution :** Le signalement n'a pas d'utilisateur dans PostgreSQL.

Vérifiez :
```sql
SELECT id, latitude, longitude, utilisateur_id, id_firebase 
FROM signalements 
WHERE id_firebase IS NOT NULL;
```

### Problème 2 : "Pas d'ID Firebase pour le signalement"

**Solution :** Le signalement n'a pas de champ `id_firebase` dans PostgreSQL.

Cela signifie que le signalement n'a PAS été créé depuis l'app mobile, mais depuis le dashboard web.

**Les notifications ne fonctionnent que pour les signalements créés depuis l'app mobile.**

### Problème 3 : "Aucun utilisateur trouvé dans Firestore avec l'email"

**Solution :** L'utilisateur n'existe pas dans la collection Firestore `users`.

Créez le document manuellement :

1. Firebase Console > Firestore > `users`
2. ID du document : `wkxCvD2K6NYgp5WdAx0VQDcYGky1` (votre UID)
3. Champs :
   ```
   email: "tendryniavo76@gmail.com"
   ```

### Problème 4 : Aucun log "🔔 sendStatusChangeNotification"

**Solution :** La méthode `modifierSignalement` n'est pas appelée.

Vérifiez que vous changez bien le statut via le bon endpoint du dashboard web.

---

## 🎯 Test Rapide : Créer une Notification Manuellement

Pour vérifier que le système mobile fonctionne, créez une notification manuellement :

### Via Firebase Console

1. Ouvrez Firebase Console > Firestore
2. Collection : `notifications`
3. Cliquez sur "Add document"
4. Auto-ID
5. Champs :
   ```
   userId: wkxCvD2K6NYgp5WdAx0VQDcYGky1
   signalementId: test-123
   titre: Test de notification
   message: Votre signalement est maintenant "en cours"
   type: status_change
   oldStatus: nouveau
   newStatus: en cours
   dateCreation: [timestamp now]
   lu: false
   ```

6. Save

**Résultat :** Le badge et la notification doivent apparaître immédiatement dans l'app mobile !

---

## ✅ Checklist Complète

### Backend
- [ ] Backend redémarré
- [ ] Logs visibles (`docker-compose logs -f backend`)
- [ ] Service `FcmNotificationService` chargé

### Firestore
- [ ] Collection `users` existe
- [ ] Document avec votre UID existe
- [ ] Document contient le champ `email`

### Signalement
- [ ] Créé depuis l'app mobile (pas le dashboard)
- [ ] A un `id_firebase` dans PostgreSQL
- [ ] A un `utilisateur_id` dans PostgreSQL
- [ ] L'utilisateur a un email valide

### Test
- [ ] Changement de statut via dashboard web
- [ ] Logs backend montrent l'envoi
- [ ] Notification créée dans Firestore
- [ ] Badge visible dans l'app mobile

---

## 🚀 Commandes Utiles

### Voir les logs backend
```powershell
docker-compose logs -f backend | Select-String "notification|🔔|📧|✅"
```

### Redémarrer le backend
```powershell
docker-compose restart backend
```

### Voir tous les signalements avec Firebase ID
```sql
SELECT s.id, s.id_firebase, s.latitude, s.longitude, 
       u.email, st.nom as statut
FROM signalements s
LEFT JOIN utilisateurs u ON s.utilisateur_id = u.id
LEFT JOIN statuts_signalement st ON s.statut_id = st.id
WHERE s.id_firebase IS NOT NULL;
```

---

## 💡 Résumé

**Le système fonctionne si :**
1. ✅ Vous êtes connecté dans l'app mobile
2. ✅ Vous créez un signalement depuis l'app mobile
3. ✅ Le backend trouve l'utilisateur dans Firestore `users`
4. ✅ Vous changez le statut depuis le dashboard web
5. ✅ Les logs backend montrent l'envoi de notification
6. ✅ La notification apparaît dans Firestore
7. ✅ Le mobile reçoit la notification en temps réel

**Testez maintenant et vérifiez les logs !** 🎉
