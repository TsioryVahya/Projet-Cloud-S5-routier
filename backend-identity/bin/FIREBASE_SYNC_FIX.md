# Correction de la synchronisation Firebase

## Problème rencontré
```
Erreur lors de la création Firestore : com.google.api.gax.rpc.UnavailableException: 
io.grpc.StatusRuntimeException: UNAVAILABLE: Credentials failed to obtain metadata
```

## Cause du problème
L'utilisation de `@EntityListeners` avec JPA causait des problèmes car :
1. Les EntityListeners ne sont **pas gérés par Spring** - ils sont instanciés par JPA/Hibernate
2. Le contexte Spring n'est pas complètement disponible lors de l'exécution du listener
3. Les credentials Firebase n'étaient pas correctement initialisés dans ce contexte

## Solution implémentée

### 1. Remplacement de l'EntityListener par des événements Spring

**Avant** : Utilisation de `@EntityListeners(SignalementEntityListener.class)`
- ❌ Problème de contexte Spring
- ❌ Credentials Firebase non disponibles

**Après** : Utilisation d'événements Spring asynchrones
- ✅ Gestion complète par Spring
- ✅ Credentials correctement initialisés
- ✅ Exécution asynchrone pour de meilleures performances

### 2. Fichiers créés/modifiés

#### Nouveaux fichiers :
- `events/SignalementSavedEvent.java` - Événement publié après sauvegarde
- `events/SignalementEventListener.java` - Listener d'événements Spring avec `@Async`

#### Fichiers modifiés :
- `IdentityApplication.java` - Ajout de `@EnableAsync`
- `SignalementService.java` - Publication d'événements au lieu d'utiliser EntityListener
- `Signalement.java` - Suppression de `@EntityListeners`
- `FirestoreConfig.java` - Ajout des scopes nécessaires aux credentials

### 3. Améliorations apportées

#### Configuration Firebase améliorée :
```java
GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount)
    .createScoped("https://www.googleapis.com/auth/cloud-platform",
                  "https://www.googleapis.com/auth/datastore");
```

#### Synchronisation asynchrone :
```java
@Async
@EventListener
public void handleSignalementSaved(SignalementSavedEvent event) {
    // La synchronisation se fait en arrière-plan
    firestoreSyncService.syncSignalementToFirebase(signalement);
}
```

## Comment ça fonctionne maintenant

1. **Création d'un signalement** :
   ```
   SignalementService.creerSignalement()
   → Sauvegarde dans Postgres
   → Synchronisation directe vers Firestore
   → Mise à jour de l'ID Firebase dans Postgres
   ```

2. **Modification d'un signalement** :
   ```
   SignalementService.modifierSignalement()
   → Sauvegarde dans Postgres
   → Publication de SignalementSavedEvent
   → SignalementEventListener (async) synchronise vers Firebase
   ```

3. **Validation d'un signalement** :
   ```
   SignalementService.validerSignalement()
   → Changement de statut dans Postgres
   → Publication de SignalementSavedEvent
   → SignalementEventListener (async) synchronise vers Firebase
   ```

## Avantages de cette approche

✅ **Meilleure intégration Spring** - Tous les composants sont gérés par Spring
✅ **Credentials correctement initialisés** - Plus d'erreur de metadata
✅ **Synchronisation asynchrone** - N'impacte pas les performances
✅ **Meilleure gestion des erreurs** - Try-catch avec logs détaillés
✅ **Code plus maintenable** - Séparation claire des responsabilités

## Test de la solution

Pour tester, créez un signalement via votre endpoint :
```bash
POST /api/signalements
{
  "latitude": 48.8566,
  "longitude": 2.3522,
  "description": "Test synchronisation",
  "email": "test@example.com"
}
```

Vous devriez voir dans les logs :
```
📝 Création d'un signalement pour : test@example.com
✅ Signalement sauvegardé dans Postgres, ID : xxx
✅ Détails sauvegardés.
🔄 Tentative de synchronisation vers Firestore...
✅ Signalement créé dans Firestore avec ID : yyy
🚀 Synchronisation réussie ! ID Firebase : yyy
```
