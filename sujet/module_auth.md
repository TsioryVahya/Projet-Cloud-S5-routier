## 🎯 Cas d'usage concrets

### Scénario 1 : Tentatives de connexion échouées
```
Utilisateur essaie de se connecter avec un mauvais mot de passe :

1. tentatives_connexion = 1 (incrémenté)
2. date_dernier_echec_connexion = NOW()
3. Si tentatives_connexion >= 3 :
   - statut_actuel_id = BLOQUE
   - date_deblocage_automatique = NOW() + 15 minutes
```

### Scénario 2 : Connexion réussie
```
Utilisateur entre le bon mot de passe :

1. tentatives_connexion = 0 (reset)
2. derniere_connexion = NOW()
3. date_dernier_echec_connexion = NULL (optionnel)
4. statut_actuel_id = ACTIF (si était bloqué temporairement)
```

### Scénario 3 : Déblocage automatique
```
Vérification périodique (cron job ou fonction) :

Si NOW() >= date_deblocage_automatique :
   - statut_actuel_id = ACTIF
   - tentatives_connexion = 0
   - date_deblocage_automatique = NULL

🤔 