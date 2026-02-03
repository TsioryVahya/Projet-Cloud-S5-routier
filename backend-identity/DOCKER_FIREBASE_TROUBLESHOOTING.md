# Solutions pour l'erreur "Credentials failed to obtain metadata" dans Docker

## Problème
```
Erreur lors de la création Firestore : com.google.api.gax.rpc.UnavailableException: 
io.grpc.StatusRuntimeException: UNAVAILABLE: Credentials failed to obtain metadata
```

## Cause
Dans un conteneur Docker, Firebase ne peut pas obtenir les tokens OAuth2 depuis les serveurs Google, probablement à cause de :
1. Restrictions réseau du conteneur
2. Problèmes de DNS
3. Firewall bloquant les connexions sortantes vers `oauth2.googleapis.com`

## Solutions

### Solution 1 : Vérifier la connectivité réseau du conteneur (RECOMMANDÉ)

Testez si votre conteneur peut accéder à Internet :

```bash
docker exec cloud-s5-backend ping -c 3 8.8.8.8
docker exec cloud-s5-backend ping -c 3 oauth2.googleapis.com
docker exec cloud-s5-backend curl -v https://oauth2.googleapis.com/token
```

Si ces commandes échouent, le problème est réseau. Solutions :
- Vérifiez votre configuration Docker network
- Assurez-vous que le conteneur a accès à Internet
- Vérifiez les paramètres de proxy si vous êtes derrière un proxy d'entreprise

### Solution 2 : Utiliser le mode host network (TEMPORAIRE)

Modifiez votre `docker-compose.yml` :

```yaml
services:
  backend:
    network_mode: "host"
    # ... reste de la configuration
```

⚠️ **Attention** : Cette solution n'est pas recommandée pour la production

### Solution 3 : Ajouter des variables d'environnement Google

Dans votre `docker-compose.yml` :

```yaml
services:
  backend:
    environment:
      - GOOGLE_APPLICATION_CREDENTIALS=/app/serviceAccountKey.json
      - GRPC_DNS_RESOLVER=native
    # ... reste de la configuration
```

Et dans votre `Dockerfile`, copiez le fichier de credentials :

```dockerfile
# Après WORKDIR /app
COPY src/main/resources/serviceAccountKey.json /app/serviceAccountKey.json
```

### Solution 4 : Désactiver temporairement la synchronisation Firestore

Si vous voulez tester le reste de votre application sans Firestore :

1. Modifiez `SignalementService.java` pour rendre la synchronisation optionnelle :

```java
@Value("${firebase.sync.enabled:false}")
private boolean firebaseSyncEnabled;

public void creerSignalement(...) {
    // ... code existant ...
    
    if (firebaseSyncEnabled) {
        String firebaseId = firestoreSyncService.createSignalementInFirestore(s, details);
        // ...
    } else {
        System.out.println("⚠️ Synchronisation Firebase désactivée");
    }
}
```

2. Dans `application.properties` :
```properties
firebase.sync.enabled=false
```

### Solution 5 : Utiliser un émulateur Firestore local

Pour le développement, utilisez l'émulateur Firestore :

```bash
# Installer l'émulateur
npm install -g firebase-tools
firebase init emulators

# Démarrer l'émulateur
firebase emulators:start --only firestore
```

Puis dans votre code, configurez pour utiliser l'émulateur :

```java
@Bean
public Firestore firestore() {
    // ... configuration existante ...
    
    // Pour l'émulateur local
    FirestoreOptions options = FirestoreOptions.newBuilder()
        .setProjectId("synchronisation-ab2ec")
        .setHost("host.docker.internal:8080")  // Port de l'émulateur
        .setCredentials(NoCredentials.getInstance())
        .build();
    
    return options.getService();
}
```

### Solution 6 : Vérifier les logs détaillés

Rebuild et redémarrez avec les nouveaux logs :

```bash
docker compose build backend
docker compose up -d --no-deps backend
docker compose logs -f backend
```

Vous devriez voir :
```
🔄 Début de la création Firestore pour signalement : xxx
📤 Envoi des données vers Firestore...
```

Si vous voyez un TIMEOUT, c'est un problème réseau.
Si vous voyez l'erreur de credentials, c'est un problème d'authentification.

## Diagnostic

Pour identifier la cause exacte, vérifiez dans les logs :

1. **TIMEOUT** → Problème réseau (Solution 1 ou 2)
2. **Credentials failed** → Problème d'authentification (Solution 3)
3. **Connection refused** → Firestore inaccessible (Solution 5)

## Test rapide

Après avoir appliqué une solution, testez :

```bash
# Rebuild
docker compose build backend

# Restart
docker compose up -d --no-deps backend

# Voir les logs
docker compose logs -f backend

# Tester l'endpoint
curl -X POST http://localhost:8081/api/signalements \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 48.8566,
    "longitude": 2.3522,
    "description": "Test",
    "email": "test@example.com"
  }'
```

## Recommandation

Commencez par la **Solution 1** pour diagnostiquer le problème réseau. Si le conteneur n'a pas accès à Internet, aucune autre solution ne fonctionnera.
