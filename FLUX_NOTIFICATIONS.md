# 🔄 Flux Complet des Notifications Persistantes

```
┌─────────────────────────────────────────────────────────────────────┐
│                    SCÉNARIO : Admin change le statut                │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────┐          ┌──────────────┐          ┌──────────────┐
│   BACKOFFICE │          │   BACKEND    │          │  FIRESTORE   │
│   (Admin)    │          │   (Java)     │          │  (Database)  │
└──────┬───────┘          └──────┬───────┘          └──────┬───────┘
       │                          │                          │
       │  PUT /signalements/123   │                          │
       │  { statut: "APPROUVE" }  │                          │
       ├─────────────────────────>│                          │
       │                          │                          │
       │                    [1] Mise à jour statut           │
       │                          ├─────────────────────────>│
       │                          │                          │
       │                    [2] Créer notification           │
       │                          │  {                       │
       │                          │    userEmail: "...",    │
       │                          │    titre: "Approuvé",   │
       │                          │    message: "...",      │
       │                          │    lue: false           │
       │                          │  }                      │
       │                          ├─────────────────────────>│
       │                          │                          │
       │                    [3] Envoyer FCM (si token)       │
       │                          │                          │
       │                          ├──> Firebase Cloud       │
       │                          │    Messaging            │
       │                          │    (si utilisateur      │
       │                          │     connecté)           │
       │                          │                          │
       │  200 OK                  │                          │
       │<─────────────────────────┤                          │
       │                          │                          │


┌─────────────────────────────────────────────────────────────────────┐
│            SCÉNARIO : Utilisateur hors ligne se reconnecte          │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────┐          ┌──────────────┐          ┌──────────────┐
│  MOBILE APP  │          │  FIRESTORE   │          │   ÉCRAN      │
│  (Vue.js)    │          │  (Database)  │          │  UTILISATEUR │
└──────┬───────┘          └──────┬───────┘          └──────┬───────┘
       │                          │                          │
       │  [1] Utilisateur         │                          │
       │      se connecte         │                          │
       │                          │                          │
       │  [2] handleLogin()       │                          │
       │      réussit             │                          │
       │                          │                          │
       │  [3] checkUnreadNotifications()                     │
       │      ├───────────────────>│                          │
       │      │                    │                          │
       │      │  [4] Query:        │                          │
       │      │  WHERE userEmail == "user@example.com"       │
       │      │  AND lue == false  │                          │
       │      │                    │                          │
       │      │  [5] Résultats     │                          │
       │      │<───────────────────┤                          │
       │      │  [                 │                          │
       │      │    {               │                          │
       │      │      titre: "✅ Approuvé",                   │
       │      │      message: "Signalement #123",            │
       │      │      lue: false    │                          │
       │      │    },              │                          │
       │      │    {               │                          │
       │      │      titre: "🎉 Résolu",                     │
       │      │      message: "Signalement #456",            │
       │      │      lue: false    │                          │
       │      │    }               │                          │
       │      │  ]                 │                          │
       │      │                    │                          │
       │  [6] Pour chaque notification :                     │
       │      │                    │                          │
       │      │  [7] Afficher toast│                          │
       │      ├────────────────────┼─────────────────────────>│
       │      │                    │  ┌──────────────────┐   │
       │      │                    │  │ ✅ Approuvé      │   │
       │      │                    │  │ Signalement #123 │   │
       │      │                    │  └──────────────────┘   │
       │      │                    │         (5 sec)          │
       │      │                    │                          │
       │      │  [8] Marquer lue   │                          │
       │      │  UPDATE notifications/abc123                  │
       │      │  SET lue = true    │                          │
       │      │      dateLecture = "2026-02-06T..."          │
       │      ├───────────────────>│                          │
       │      │                    │                          │
       │      │  [9] Délai 500ms   │                          │
       │      │                    │                          │
       │      │  [10] Afficher 2e toast                       │
       │      ├────────────────────┼─────────────────────────>│
       │      │                    │  ┌──────────────────┐   │
       │      │                    │  │ 🎉 Résolu        │   │
       │      │                    │  │ Signalement #456 │   │
       │      │                    │  └──────────────────┘   │
       │      │                    │         (5 sec)          │
       │      │                    │                          │
       │      │  [11] Marquer lue  │                          │
       │      ├───────────────────>│                          │
       │      │                    │                          │
       │  [12] ✅ Terminé          │                          │
       │                          │                          │


┌─────────────────────────────────────────────────────────────────────┐
│              STRUCTURE FIRESTORE - Collection "notifications"       │
└─────────────────────────────────────────────────────────────────────┘

notifications/
├── abc123/  ← Document ID auto-généré
│   ├── userEmail: "user@example.com"
│   ├── titre: "✅ Signalement Approuvé"
│   ├── message: "Votre signalement #123 a été approuvé"
│   ├── signalementId: 123
│   ├── lue: true  ← Changé à true après affichage
│   ├── dateCreation: "2026-02-06T10:00:00Z"
│   └── dateLecture: "2026-02-06T10:05:00Z"  ← Ajouté après lecture
│
├── def456/
│   ├── userEmail: "user@example.com"
│   ├── titre: "🎉 Signalement Résolu"
│   ├── message: "Votre signalement #456 a été résolu"
│   ├── signalementId: 456
│   ├── lue: true
│   ├── dateCreation: "2026-02-06T11:30:00Z"
│   └── dateLecture: "2026-02-06T11:35:00Z"
│
└── ghi789/
    ├── userEmail: "other@example.com"  ← Autre utilisateur
    ├── titre: "❌ Signalement Rejeté"
    ├── message: "Votre signalement #789 a été rejeté"
    ├── signalementId: 789
    ├── lue: false  ← Pas encore lu
    ├── dateCreation: "2026-02-06T12:00:00Z"
    └── dateLecture: null


┌─────────────────────────────────────────────────────────────────────┐
│                    INDEX FIRESTORE COMPOSITE                        │
└─────────────────────────────────────────────────────────────────────┘

Collection: notifications

Index Fields:
  1. userEmail    ↑ (Ascending)
  2. lue          ↑ (Ascending)
  3. dateCreation ↓ (Descending)

Permet la requête :
  WHERE userEmail == "user@example.com"
  AND lue == false
  ORDER BY dateCreation DESC


┌─────────────────────────────────────────────────────────────────────┐
│                      RÈGLES DE SÉCURITÉ                             │
└─────────────────────────────────────────────────────────────────────┘

✅ AUTORISÉ                          ❌ INTERDIT
─────────────────────────────────    ────────────────────────────────
• Lire ses propres notifications     • Lire les notifications d'autrui
• Marquer ses notifs comme lues      • Modifier titre/message
• Query avec son propre email        • Créer des notifications
                                     • Supprimer des notifications


┌─────────────────────────────────────────────────────────────────────┐
│                    AVANTAGES DU SYSTÈME                             │
└─────────────────────────────────────────────────────────────────────┘

┌──────────────────┐       ┌──────────────────┐
│  TEMPS RÉEL      │       │   PERSISTANCE    │
│  (FCM)           │   +   │   (Firestore)    │
└────────┬─────────┘       └────────┬─────────┘
         │                          │
         ├──> Si connecté           ├──> Toujours sauvegardé
         ├──> Notification push     ├──> Historique complet
         ├──> Immédiat              ├──> Récupérable plus tard
         └──> Peut échouer          └──> Garantie 100%


┌─────────────────────────────────────────────────────────────────────┐
│                  CAS D'UTILISATION TYPIQUES                         │
└─────────────────────────────────────────────────────────────────────┘

📱 UTILISATEUR CONNECTÉ
   → Reçoit FCM immédiatement
   → Notification aussi dans Firestore
   → Peut relire l'historique si besoin

📴 UTILISATEUR HORS LIGNE
   → FCM échoue (normal)
   → Notification sauvegardée dans Firestore
   → Affichée à la reconnexion

🔄 UTILISATEUR REVIENT APRÈS 3 JOURS
   → 5 notifications en attente
   → Toutes affichées une par une
   → Marquées comme lues automatiquement

🛜 RÉSEAU INSTABLE
   → FCM peut échouer
   → Firestore garantit la livraison
   → Pas de notifications perdues


┌─────────────────────────────────────────────────────────────────────┐
│                         LOGS ATTENDUS                               │
└─────────────────────────────────────────────────────────────────────┘

Console navigateur (lors de la connexion) :
───────────────────────────────────────────
🔔 Vérification des notifications non lues pour: user@example.com
📬 2 notification(s) non lue(s) trouvée(s)
📩 Affichage notification: {titre: "✅ Signalement Approuvé", ...}
📩 Affichage notification: {titre: "🎉 Signalement Résolu", ...}
✅ Toutes les notifications ont été affichées et marquées comme lues

Console backend (lors du changement de statut) :
────────────────────────────────────────────────
📨 Envoi notification pour: user@example.com
✅ Notification persistée avec ID: abc123
✅ Notification FCM envoyée

```

**Créé le** : 06 Février 2026  
**Par** : GitHub Copilot
