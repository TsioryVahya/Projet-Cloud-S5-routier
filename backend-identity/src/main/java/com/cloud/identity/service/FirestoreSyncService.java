package com.cloud.identity.service;

import com.cloud.identity.entities.Signalement;
import com.cloud.identity.entities.SignalementsDetail;
import com.cloud.identity.entities.Utilisateur;
import com.cloud.identity.repository.*;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Service
public class FirestoreSyncService {

    @Autowired(required = false)
    private Firestore firestore;

    @Autowired
    private SignalementRepository signalementRepository;

    @Autowired
    private SignalementsDetailRepository detailsRepository;

    @Autowired
    private StatutsSignalementRepository statutRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StatutUtilisateurRepository statutUtilisateurRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private TypeSignalementRepository typeSignalementRepository;
    private ConfigurationRepository configurationRepository;

    /**
     * Synchronise les utilisateurs de Firestore vers PostgreSQL.
     */
    public Map<String, Integer> syncUsersFromFirestoreToPostgres() {
        if (firestore == null) {
            System.err.println("⚠️ Impossible de synchroniser : Firestore n'est pas initialisé.");
            return Map.of("created", 0, "total", 0);
        }
        int createdUsers = 0;
        int updatedUsers = 0;
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("utilisateurs").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (QueryDocumentSnapshot document : documents) {
                String email = document.getString("email");
                String idStr = document.getString("id");
                if (idStr == null) idStr = document.getId(); // Fallback sur l'ID du document
                
                if (email == null || email.isEmpty())
                    continue;

                Utilisateur user = null;
                
                // 1. Chercher par ID d'abord (si présent)
                if (idStr != null && !idStr.isEmpty()) {
                    try {
                        UUID uuid = UUID.fromString(idStr);
                        Optional<Utilisateur> existingUserById = utilisateurRepository.findById(uuid);
                        if (existingUserById.isPresent()) {
                            user = existingUserById.get();
                            System.out.println("🔄 Mise à jour utilisateur existant par ID : " + idStr);
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("⚠️ ID utilisateur Firestore invalide : " + idStr);
                    }
                }
                
                // 2. Chercher par email si non trouvé par ID
                if (user == null) {
                    Optional<Utilisateur> existingUserByEmail = utilisateurRepository.findByEmail(email);
                    if (existingUserByEmail.isPresent()) {
                        user = existingUserByEmail.get();
                        System.out.println("🔄 Mise à jour utilisateur existant par email : " + email);
                    }
                }

                if (user == null) {
                    user = new Utilisateur();
                    user.setEmail(email);
                    
                    // Si on a un ID dans Firestore, on essaie de le préserver
                    if (idStr != null && !idStr.isEmpty()) {
                        try {
                            user.setId(UUID.fromString(idStr));
                        } catch (Exception e) {
                            // Ignorer si ID invalide
                        }
                    }
                    
                    user.setDateCreation(java.time.Instant.now());
                    System.out.println("➕ Création nouvel utilisateur : " + email);
                    createdUsers++;
                }

                // Mettre à jour les champs
                String mdp = document.getString("motDePasse");
                if (mdp != null && !mdp.isEmpty()) {
                    user.setMotDePasse(mdp);
                } else if (user.getMotDePasse() == null) {
                    user.setMotDePasse("default_password");
                }

                String roleNom = document.getString("role");
                if (roleNom != null) {
                    roleRepository.findByNom(roleNom.toUpperCase()).ifPresent(user::setRole);
                } else if (user.getRole() == null) {
                    roleRepository.findByNom("UTILISATEUR").ifPresent(user::setRole);
                }

                String statutNom = document.getString("statut");
                if (statutNom != null) {
                    statutUtilisateurRepository.findByNom(statutNom.toUpperCase()).ifPresent(user::setStatutActuel);
                } else if (user.getStatutActuel() == null) {
                    statutUtilisateurRepository.findByNom("ACTIF").ifPresent(user::setStatutActuel);
                }

                utilisateurRepository.save(user);
                String postgresIdStr = document.getString("postgresId");
                
                if (email == null || email.isEmpty()) continue;

                Optional<Utilisateur> userOpt = Optional.empty();
                
                // Priorité à l'ID Postgres pour éviter les doublons si l'email a changé
                if (postgresIdStr != null && !postgresIdStr.isEmpty()) {
                    try {
                        userOpt = utilisateurRepository.findById(java.util.UUID.fromString(postgresIdStr));
                    } catch (Exception e) {
                        System.err.println("ID Postgres invalide dans Firestore: " + postgresIdStr);
                    }
                }
                
                // Si non trouvé par ID, on cherche par email
                if (userOpt.isEmpty()) {
                    userOpt = utilisateurRepository.findByEmail(email);
                }

                Utilisateur userToSync;
                boolean isNew = false;

                if (userOpt.isPresent()) {
                    userToSync = userOpt.get();
                    
                    // --- LOGIQUE DE COMPARAISON DES DATES (Solution 2) ---
                    com.google.cloud.Timestamp firestoreTimeMod = document.getTimestamp("date_derniere_modification");
                    if (firestoreTimeMod != null && userToSync.getDateDerniereModification() != null) {
                        java.time.Instant firestoreInstant = firestoreTimeMod.toSqlTimestamp().toInstant();
                        java.time.Instant postgresInstant = userToSync.getDateDerniereModification();
                        
                        // Si Postgres est plus récent, on ignore l'import pour cet utilisateur
                        if (postgresInstant.isAfter(firestoreInstant)) {
                            System.out.println("⏳ Sync ignorée pour " + email + " (Postgres est plus récent : " + postgresInstant + " > " + firestoreInstant + ")");
                            continue; 
                        }
                    }
                } else {
                    userToSync = new Utilisateur();
                    // Si on a un postgresIdStr mais qu'il n'existe pas en base, on peut soit l'ignorer, 
                    // soit le créer avec cet ID. Ici on le crée avec cet ID si possible.
                    if (postgresIdStr != null && !postgresIdStr.isEmpty()) {
                        try {
                            userToSync.setId(java.util.UUID.fromString(postgresIdStr));
                        } catch (Exception e) {}
                    }
                    userToSync.setEmail(email);
                    userToSync.setDateCreation(java.time.Instant.now());
                    userToSync.setRole(roleRepository.findByNom("UTILISATEUR").orElse(null));
                    isNew = true;
                }

                // Mettre à jour l'email au cas où il aurait changé
                userToSync.setEmail(email);

                // Synchroniser le mot de passe si présent
                if (document.getString("motDePasse") != null) {
                    userToSync.setMotDePasse(document.getString("motDePasse"));
                } else if (isNew) {
                    userToSync.setMotDePasse("default_password");
                }

                // Synchroniser le statut depuis Firestore
                String firestoreStatut = document.getString("statut");
                if (firestoreStatut != null) {
                    final String statusToSearch = firestoreStatut;
                    userToSync.setStatutActuel(statutUtilisateurRepository.findByNom(statusToSearch)
                            .orElseGet(() -> statutUtilisateurRepository.findByNom("ACTIF").orElse(null)));
                } else if (isNew) {
                    userToSync.setStatutActuel(statutUtilisateurRepository.findByNom("ACTIF").orElse(null));
                }

                // Synchroniser les tentatives de connexion
                Long tentatives = document.getLong("tentatives_connexion");
                if (tentatives != null) {
                    userToSync.setTentativesConnexion(tentatives.intValue());
                }

                // Mettre à jour la date de modification locale avec celle de Firestore
                com.google.cloud.Timestamp firestoreTimeFinal = document.getTimestamp("date_derniere_modification");
                if (firestoreTimeFinal != null) {
                    userToSync.setDateDerniereModification(firestoreTimeFinal.toSqlTimestamp().toInstant());
                }

                utilisateurRepository.save(userToSync);
                if (isNew) createdUsers++;
                else updatedUsers++;
            }
        } catch (Exception e) {
            System.err.println(
                    "Erreur lors de la synchronisation des utilisateurs Firestore -> Postgres : " + e.getMessage());
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("utilisateurs_crees", createdUsers);
        result.put("utilisateurs_mis_a_jour", updatedUsers);
        return result;
    }

    /**
     * Synchronise un utilisateur unique de PostgreSQL vers Firestore.
     */
    public void syncSingleUserToFirestore(Utilisateur user) {
        if (firestore == null) {
            System.err.println("⚠️ Impossible de synchroniser l'utilisateur : Firestore n'est pas initialisé.");
            return;
        }
        try {
            CollectionReference usersCol = firestore.collection("utilisateurs");
            Map<String, Object> data = new HashMap<>();
            data.put("id", user.getId().toString());
            data.put("email", user.getEmail());
            data.put("motDePasse", user.getMotDePasse());

            if (user.getRole() != null) {
                data.put("role", user.getRole().getNom());
            }

            if (user.getStatutActuel() != null) {
                data.put("statut", user.getStatutActuel().getNom());
            }

            data.put("dateCreation", user.getDateCreation() != null ? user.getDateCreation().toString() : null);
            data.put("derniereConnexion",
                    user.getDerniereConnexion() != null ? user.getDerniereConnexion().toString() : null);

            // Utiliser l'ID Postgres comme ID de document Firestore
            usersCol.document(user.getId().toString()).set(data).get();
            System.out.println("🚀 Synchronisation immédiate vers Firestore réussie pour : " + user.getEmail());
        } catch (Exception e) {
            System.err.println("Erreur lors de la synchronisation immédiate vers Firestore : " + e.getMessage());
        }
    }

    /**
     * Synchronise les utilisateurs de PostgreSQL vers Firestore.
     */
    public Map<String, Integer> syncUsersFromPostgresToFirestore() {
        if (firestore == null) {
            System.err.println("⚠️ Impossible de synchroniser les utilisateurs : Firestore n'est pas initialisé.");
            return Map.of("syncedUsers", 0);
        }
        int syncedUsers = 0;
        try {
            List<Utilisateur> users = utilisateurRepository.findAll();
            for (Utilisateur user : users) {
                syncSingleUserToFirestore(user);
                syncUserToFirestore(user);
                syncedUsers++;
            }
        } catch (Exception e) {
            System.err.println(
                    "Erreur lors de la synchronisation des utilisateurs Postgres -> Firestore : " + e.getMessage());
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("syncedUsers", syncedUsers);
        return result;
    }

    /**
     * Met à jour l'email de l'utilisateur dans tous ses signalements dans
     * Firestore.
     */
    public void updateEmailInFirestoreSignalements(String oldEmail, String newEmail) {
        if (firestore == null) {
            System.err.println("⚠️ Impossible de mettre à jour Firestore : Firestore n'est pas initialisé.");
            return;
        }
        System.out.println("🔍 Recherche de signalements à mettre à jour : " + oldEmail + " -> " + newEmail);
        try {
            // 1. Chercher dans utilisateur.email (structure imbriquée)
            ApiFuture<QuerySnapshot> futureNested = firestore.collection("signalements")
                    .whereEqualTo("utilisateur.email", oldEmail)
                    .get();

            List<QueryDocumentSnapshot> docsNested = futureNested.get().getDocuments();
            for (QueryDocumentSnapshot document : docsNested) {
                document.getReference().update("utilisateur.email", newEmail).get();
                System.out.println("✅ Signalement " + document.getId() + " mis à jour (utilisateur.email)");
            }

            // 2. Chercher dans email (structure à la racine, au cas où)
            ApiFuture<QuerySnapshot> futureRoot = firestore.collection("signalements")
                    .whereEqualTo("email", oldEmail)
                    .get();

            List<QueryDocumentSnapshot> docsRoot = futureRoot.get().getDocuments();
            for (QueryDocumentSnapshot document : docsRoot) {
                document.getReference().update("email", newEmail).get();
                System.out.println("✅ Signalement " + document.getId() + " mis à jour (email racine)");
            }

            int total = docsNested.size() + docsRoot.size();
            if (total > 0) {
                System.out.println("📧 Cascade réussie : " + total + " signalements mis à jour dans Firestore.");
            } else {
                System.out.println("ℹ️ Aucun signalement trouvé dans Firestore avec l'email : " + oldEmail);
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la mise à jour en cascade des emails Firestore : " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Supprime un utilisateur dans Firestore.
     */
    public void deleteUserInFirestore(String userIdOrEmail) {
        if (firestore == null) {
            System.err.println("⚠️ Impossible de supprimer dans Firestore : Firestore n'est pas initialisé.");
            return;
        }
        try {
            // On essaie de supprimer par ID d'abord (recommandé car plus stable)
            firestore.collection("utilisateurs").document(userIdOrEmail).delete().get();
            System.out.println(
                    "🗑️ Tentative de suppression de l'utilisateur dans Firestore (ID/Email) : " + userIdOrEmail);

            // Si c'était un email et qu'on utilise maintenant des IDs, on peut aussi
            // chercher le document par le champ email
            ApiFuture<QuerySnapshot> future = firestore.collection("utilisateurs").whereEqualTo("email", userIdOrEmail)
                    .get();
            for (DocumentSnapshot doc : future.get().getDocuments()) {
                doc.getReference().delete().get();
                System.out.println("🗑️ Utilisateur supprimé de Firestore via son champ email : " + userIdOrEmail);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression de l'utilisateur dans Firestore : " + e.getMessage());
        }
    }

    /**
     * Synchronise un seul type de signalement vers Firestore.
     */
    public void syncSingleTypeSignalementToFirestore(com.cloud.identity.entities.TypeSignalement type) {
        if (firestore == null) return;
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("id", type.getId());
            data.put("nom", type.getNom());
            data.put("description", type.getDescription());
            data.put("icone_path", type.getIconePath());
            data.put("couleur", type.getCouleur());

            firestore.collection("types_signalement").document(String.valueOf(type.getId())).set(data).get();
            System.out.println("🚀 Synchronisation immédiate du type vers Firestore réussie : " + type.getNom());
        } catch (Exception e) {
            System.err.println("Erreur lors de la synchronisation immédiate du type vers Firestore : " + e.getMessage());
        }
    }

    /**
     * Supprime un type de signalement dans Firestore.
     */
    public void deleteTypeSignalementInFirestore(Integer typeId) {
        if (firestore == null) return;
        try {
            firestore.collection("types_signalement").document(String.valueOf(typeId)).delete().get();
            System.out.println("🗑️ Suppression du type dans Firestore réussie : " + typeId);
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression du type dans Firestore : " + e.getMessage());
        }
    }

    /**
     * Synchronise les types de signalement de PostgreSQL vers Firestore.
     */
    public Map<String, Integer> syncTypesSignalementToFirestore() {
        if (firestore == null) {
            System.err.println("⚠️ Impossible de synchroniser les types : Firestore n'est pas initialisé.");
            return Map.of("syncedTypes", 0);
        }
        int syncedTypes = 0;
        try {
            List<com.cloud.identity.entities.TypeSignalement> types = typeSignalementRepository.findAll();
            for (com.cloud.identity.entities.TypeSignalement type : types) {
                Map<String, Object> data = new HashMap<>();
                data.put("id", type.getId());
                data.put("nom", type.getNom());
                data.put("description", type.getDescription());
                data.put("icone_path", type.getIconePath());
                data.put("couleur", type.getCouleur());

                // Utiliser le nom comme ID de document ou l'ID numérique
                firestore.collection("types_signalement").document(String.valueOf(type.getId())).set(data).get();
                syncedTypes++;
            }
            System.out.println("🚀 Synchronisation des types vers Firestore réussie : " + syncedTypes);
        } catch (Exception e) {
            System.err.println("Erreur lors de la synchronisation des types vers Firestore : " + e.getMessage());
        }
        return Map.of("syncedTypes", syncedTypes);
    }
     /* Synchronise un utilisateur spécifique vers Firestore.
     */
    public void syncUserToFirestore(Utilisateur user) {
        try {
            CollectionReference usersCol = firestore.collection("utilisateurs");
            Map<String, Object> data = new HashMap<>();
            data.put("postgresId", user.getId().toString());
            data.put("email", user.getEmail());
            data.put("motDePasse", user.getMotDePasse());
            data.put("tentatives_connexion", user.getTentativesConnexion() != null ? user.getTentativesConnexion() : 0);

            if (user.getRole() != null) {
                data.put("role", user.getRole().getNom());
            }

            if (user.getStatutActuel() != null) {
                data.put("statut", user.getStatutActuel().getNom());
            }

            data.put("dateCreation", user.getDateCreation() != null ? user.getDateCreation().toString() : null);
            data.put("derniereConnexion", user.getDerniereConnexion() != null ? user.getDerniereConnexion().toString() : null);
            data.put("date_derniere_modification", user.getDateDerniereModification() != null ? com.google.cloud.Timestamp.of(java.sql.Timestamp.from(user.getDateDerniereModification())) : null);

            // LOG POUR DEBUG : On affiche ce qu'on envoie
            System.out.println("📤 Sync vers Firestore [" + user.getEmail() + "] - MDP: " + user.getMotDePasse());

            // Utiliser l'ID Postgres comme ID de document dans Firestore pour éviter les doublons si l'email change
            usersCol.document(user.getId().toString()).set(data).get();
        } catch (Exception e) {
            System.err.println("Erreur lors de la synchronisation de l'utilisateur " + user.getEmail() + " vers Firestore : " + e.getMessage());
        }
    }

    /**
     * Synchronise les configurations de PostgreSQL vers Firestore.
     */
    public void syncConfigurationsToFirestore() {
        try {
            List<com.cloud.identity.entities.Configuration> configs = configurationRepository.findAll();
            CollectionReference configCol = firestore.collection("configurations");

            for (com.cloud.identity.entities.Configuration config : configs) {
                Map<String, Object> data = new HashMap<>();
                data.put("valeur", config.getValeur());
                data.put("description", config.getDescription());
                configCol.document(config.getCle()).set(data).get();
            }
            System.out.println("✅ Configurations synchronisées vers Firestore.");
        } catch (Exception e) {
            System.err.println("Erreur lors de la synchronisation des configurations vers Firestore : " + e.getMessage());
        }
    }

    /**
     * Synchronise les données de Firestore vers PostgreSQL.
     */
    public Map<String, Integer> syncFromFirestoreToPostgres() {
        if (firestore == null) {
            System.err.println("⚠️ Impossible de synchroniser : Firestore n'est pas initialisé.");
            return Map.of("signalements", 0);
        }
        int syncedSignalements = 0;
        int createdUsers = 0;

        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("signalements").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (QueryDocumentSnapshot document : documents) {
                String idFirebase = document.getId();

                // Vérifier si le signalement existe déjà dans Postgres
                Optional<Signalement> existingSignalement = signalementRepository.findByIdFirebase(idFirebase);
                
                // Extraire l'ID utilisateur Firestore
                String utilisateurIdStr = document.getString("utilisateur_id");
                if (utilisateurIdStr == null) {
                    utilisateurIdStr = document.getString("utilisateurId");
                }

                if (existingSignalement.isPresent()) {
                    Signalement s = existingSignalement.get();
                    // Si le signalement existe déjà, on vérifie si l'utilisateur est anonyme
                    // Si oui, on essaie de le lier au bon utilisateur maintenant
                    if (s.getUtilisateur() != null && "anonyme@routier.mg".equals(s.getUtilisateur().getEmail()) && utilisateurIdStr != null) {
                        System.out.println("🔄 Mise à jour de l'auteur pour le signalement existant : " + idFirebase);
                        // On continue le traitement pour mettre à jour l'utilisateur
                    } else {
                        continue;
                    }
                }

                // Extraire les données
                Double latitude = document.getDouble("latitude");
                Double longitude = document.getDouble("longitude");
                String description = document.getString("description");
                String statutNom = document.getString("statut");
                
                // Gérer le type (ID numérique ou Nom)
                Long typeIdLong = document.getLong("id_type_signalement");
                String typeNom = document.getString("type");
                if (typeNom == null) typeNom = document.getString("type_nom");

                // Gérer la date
                java.time.Instant dateSignalement = java.time.Instant.now();
                Object dateObj = document.get("date_signalement");
                if (dateObj == null)
                    dateObj = document.get("dateSignalement");

                if (dateObj instanceof com.google.cloud.Timestamp) {
                    dateSignalement = ((com.google.cloud.Timestamp) dateObj).toSqlTimestamp().toInstant();
                } else if (dateObj instanceof String) {
                    try {
                        dateSignalement = java.time.Instant.parse((String) dateObj);
                    } catch (Exception e) {
                        System.err.println("Erreur parsing date string: " + dateObj);
                    }
                }

                // Support both camelCase and snake_case from Firestore
                String photoUrl = document.getString("photoUrl");
                if (photoUrl == null)
                    photoUrl = document.getString("photo_url");

                Double surfaceM2 = getAsDouble(document, "surfaceM2");
                if (surfaceM2 == null)
                    surfaceM2 = getAsDouble(document, "surface_m2");

                String entrepriseConcerne = document.getString("entrepriseConcerne");
                if (entrepriseConcerne == null)
                    entrepriseConcerne = document.getString("entreprise_concerne");
                if (entrepriseConcerne == null)
                    entrepriseConcerne = document.getString("entreprise");

                Object budgetObj = document.get("budget");
                BigDecimal budget = null;
                if (budgetObj != null && !budgetObj.toString().isEmpty()) {
                    try {
                        budget = new BigDecimal(budgetObj.toString());
                    } catch (Exception e) {
                        System.err.println("Erreur conversion budget pour doc " + idFirebase + " : " + budgetObj);
                    }
                }

                Map<String, Object> userMap = (Map<String, Object>) document.get("utilisateur");
                String email = "anonyme@routier.mg";
                if (userMap != null && userMap.get("email") != null) {
                    email = (String) userMap.get("email");
                }

                // Créer ou récupérer le signalement dans Postgres
                Signalement s;
                if (existingSignalement.isPresent()) {
                    s = existingSignalement.get();
                } else {
                    s = new Signalement();
                    s.setIdFirebase(idFirebase);
                }
                
                s.setLatitude(latitude != null ? latitude : 0.0);
                s.setLongitude(longitude != null ? longitude : 0.0);
                s.setDateSignalement(dateSignalement);

                // Gérer le statut
                String finalStatutNom = (statutNom != null) ? statutNom.toLowerCase() : "nouveau";
                s.setStatut(statutRepository.findByNom(finalStatutNom)
                        .orElseGet(() -> {
                            var newStatut = new com.cloud.identity.entities.StatutsSignalement();
                            newStatut.setNom(finalStatutNom);
                            return statutRepository.save(newStatut);
                        }));

                // Gérer le type
                if (typeIdLong != null) {
                    // Priorité 1 : Recherche par ID (nouveau format numérique)
                    typeSignalementRepository.findById(typeIdLong.intValue()).ifPresent(s::setType);
                } else if (typeNom != null && !typeNom.isEmpty()) {
                    // Priorité 2 : Recherche par Nom (ancien format texte)
                    final String finalTypeNom = typeNom;
                    typeSignalementRepository.findByNom(finalTypeNom).ifPresent(s::setType);
                }

                // Gérer l'utilisateur
                Utilisateur userToSet = null;

                // 1. Essayer par utilisateur_id (UUID)
                if (utilisateurIdStr != null && !utilisateurIdStr.isEmpty()) {
                    try {
                        UUID uuid = UUID.fromString(utilisateurIdStr);
                        Optional<Utilisateur> userById = utilisateurRepository.findById(uuid);
                        if (userById.isPresent()) {
                            System.out.println("✅ Utilisateur trouvé par ID : " + utilisateurIdStr);
                            userToSet = userById.get();
                        }
                    } catch (IllegalArgumentException e) {
                        System.err.println("⚠️ utilisateur_id invalide : " + utilisateurIdStr);
                    }
                }

                // 2. Si non trouvé par ID, essayer par email
                if (userToSet == null) {
                    final String finalEmail = email;
                    System.out.println("👤 Recherche de l'utilisateur par email : " + finalEmail);
                    Optional<Utilisateur> userByEmail = utilisateurRepository.findByEmail(finalEmail);
                    if (userByEmail.isPresent()) {
                        System.out.println("✅ Utilisateur trouvé par email : " + finalEmail);
                        userToSet = userByEmail.get();
                    }
                }

                // 3. Si toujours pas trouvé, créer un nouvel utilisateur
                if (userToSet == null) {
                    final String finalEmail = email;
                    final String finalUserId = utilisateurIdStr;
                    System.out.println("➕ Création d'un nouvel utilisateur : " + finalEmail);
                    var newUser = new com.cloud.identity.entities.Utilisateur();
                    newUser.setEmail(finalEmail);
                    newUser.setMotDePasse("default_password");

                    // Préserver l'ID si disponible
                    if (finalUserId != null && !finalUserId.isEmpty()) {
                        try {
                            newUser.setId(UUID.fromString(finalUserId));
                        } catch (Exception e) {
                            System.err.println("⚠️ Impossible d'utiliser l'ID Firestore pour le nouvel utilisateur: " + finalUserId);
                        }
                    }

                    // Attribuer un rôle par défaut (UTILISATEUR)
                    roleRepository.findByNom("UTILISATEUR").ifPresent(newUser::setRole);

                    // Attribuer un statut par défaut (ACTIF)
                    statutUtilisateurRepository.findByNom("ACTIF").ifPresent(newUser::setStatutActuel);

                    newUser.setDateCreation(java.time.Instant.now());
                    userToSet = utilisateurRepository.save(newUser);
                }

                s.setUtilisateur(userToSet);
                s = signalementRepository.save(s);

                // Détails
                SignalementsDetail details = s.getDetails();
                if (details == null) {
                    details = new SignalementsDetail();
                    details.setSignalement(s);
                }
                
                details.setDescription(description);
                details.setSurfaceM2(surfaceM2);
                details.setBudget(budget);

                if (entrepriseConcerne != null && !entrepriseConcerne.isEmpty()) {
                    final String entNom = entrepriseConcerne;
                    com.cloud.identity.entities.Entreprise entreprise = entrepriseRepository.findByNom(entNom)
                            .orElseGet(() -> {
                                com.cloud.identity.entities.Entreprise e = new com.cloud.identity.entities.Entreprise();
                                e.setNom(entNom);
                                return entrepriseRepository.save(e);
                            });
                    details.setEntreprise(entreprise);
                }

                if (photoUrl != null && !photoUrl.isEmpty()) {
                    details.setPhotoUrl(photoUrl);
                }

                s.setDetails(details);
                detailsRepository.save(details);

                syncedSignalements++;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la synchronisation Firestore -> Postgres : " + e.getMessage());
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("signalements", syncedSignalements);
        return result;
    }

    private Double getAsDouble(DocumentSnapshot doc, String field) {
        Object val = doc.get(field);
        if (val instanceof Double)
            return (Double) val;
        if (val instanceof Long)
            return ((Long) val).doubleValue();
        if (val instanceof Integer)
            return ((Integer) val).doubleValue();
        if (val instanceof String && !((String) val).isEmpty()) {
            try {
                return Double.valueOf((String) val);
            } catch (Exception e) {
                return null;
            }
        }
        return null;
    }

    /**
     * Crée un nouveau signalement dans Firestore à partir d'un signalement
     * PostgreSQL.
     */
    public String createSignalementInFirestore(Signalement signalement, SignalementsDetail details) {
        if (firestore == null) {
            System.err.println("⚠️ Impossible de créer dans Firestore : Firestore n'est pas initialisé.");
            return null;
        }
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("postgresId", signalement.getId().toString());
            data.put("latitude", signalement.getLatitude());
            data.put("longitude", signalement.getLongitude());
            data.put("dateSignalement",
                    signalement.getDateSignalement() != null ? signalement.getDateSignalement().toString() : null);

            if (signalement.getStatut() != null) {
                data.put("statut", signalement.getStatut().getNom());
            }

            if (signalement.getType() != null) {
                data.put("type", signalement.getType().getNom());
                data.put("id_type_signalement", signalement.getType().getId());
            }

            if (signalement.getUtilisateur() != null) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("email", signalement.getUtilisateur().getEmail());
                data.put("utilisateur", userMap);
            }

            if (details != null) {
                data.put("description", details.getDescription());
                data.put("surface_m2", details.getSurfaceM2());
                data.put("budget", details.getBudget() != null ? details.getBudget().toString() : null);

                if (details.getEntreprise() != null) {
                    data.put("entreprise", details.getEntreprise().getNom());
                }

                data.put("photo_url", details.getPhotoUrl());
            }

            // Ajouter à Firestore et récupérer l'ID généré
            DocumentReference docRef = firestore.collection("signalements").document();
            data.put("idFirebase", docRef.getId()); // Ajouter l'ID dans le document lui-même
            docRef.set(data).get(); // .get() pour attendre la fin de l'opération

            System.out.println("Signalement créé dans Firestore avec ID : " + docRef.getId());
            return docRef.getId();
        } catch (Exception e) {
            System.err.println("Erreur lors de la création Firestore : " + e.getMessage());
            return null;
        }
    }

    /**
     * Synchronise l'état d'un signalement de PostgreSQL vers Firebase.
     */
    public void syncSignalementToFirebase(Signalement signalement) {
        if (signalement.getIdFirebase() == null) {
            return;
        }

        try {
            Map<String, Object> updates = new HashMap<>();
            if (signalement.getStatut() != null) {
                updates.put("statut", signalement.getStatut().getNom());
            }

            if (signalement.getType() != null) {
                updates.put("type", signalement.getType().getNom());
                updates.put("id_type_signalement", signalement.getType().getId());
            }

            updates.put("postgresId", signalement.getId().toString());
            updates.put("latitude", signalement.getLatitude());
            updates.put("longitude", signalement.getLongitude());

            if (signalement.getDetails() != null) {
                updates.put("description", signalement.getDetails().getDescription());
                updates.put("surface_m2", signalement.getDetails().getSurfaceM2());
                updates.put("budget",
                        signalement.getDetails().getBudget() != null ? signalement.getDetails().getBudget().toString()
                                : null);

                if (signalement.getDetails().getEntreprise() != null) {
                    updates.put("entreprise", signalement.getDetails().getEntreprise().getNom());
                } else {
                    updates.put("entreprise", null);
                }

                // Ne mettre à jour la photo que si elle n'est pas nulle
                if (signalement.getDetails().getPhotoUrl() != null
                        && !signalement.getDetails().getPhotoUrl().isEmpty()) {
                    updates.put("photo_url", signalement.getDetails().getPhotoUrl());
                }
            }

            // On met à jour le document Firebase correspondant
            firestore.collection("signalements")
                    .document(signalement.getIdFirebase())
                    .update(updates);

            System.out.println("Synchronisation Firebase réussie pour le signalement : " + signalement.getId());
        } catch (Exception e) {
            System.err.println("Erreur lors de la synchronisation Firebase : " + e.getMessage());
        }
    }
}
