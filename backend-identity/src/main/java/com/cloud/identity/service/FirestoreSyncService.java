package com.cloud.identity.service;

import com.cloud.identity.entities.Entreprise;
import com.cloud.identity.entities.Signalement;
import com.cloud.identity.entities.SignalementsDetail;
import com.cloud.identity.entities.TypeSignalement;
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

@Service
public class FirestoreSyncService {

    @Autowired
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
    private ConfigurationRepository configurationRepository;

    @Autowired
    private TypeSignalementRepository typeSignalementRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    /**
     * Synchronise les utilisateurs de Firestore vers PostgreSQL.
     */
    public Map<String, Integer> syncUsersFromFirestoreToPostgres() {
        int createdUsers = 0;
        int updatedUsers = 0;
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("utilisateurs").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (QueryDocumentSnapshot document : documents) {
                String email = document.getString("email");
                String firebaseUid = document.getId(); // L'ID du document est le Firebase UID

                if (email == null || email.isEmpty())
                    continue;

                Optional<Utilisateur> userOpt = utilisateurRepository.findByFirebaseUid(firebaseUid);

                // Si non trouvé par Firebase UID, on cherche par email (pour la transition)
                if (userOpt.isEmpty()) {
                    userOpt = utilisateurRepository.findByEmail(email);
                }

                Utilisateur user;
                boolean isNew = false;

                if (userOpt.isPresent()) {
                    user = userOpt.get();
                    // Assigner le firebaseUid s'il n'était pas encore là
                    if (user.getFirebaseUid() == null) {
                        user.setFirebaseUid(firebaseUid);
                    }

                    // --- LOGIQUE DE COMPARAISON DES DATES (Solution 2) ---
                    com.google.cloud.Timestamp firestoreTime = document.getTimestamp("date_derniere_modification");
                    if (firestoreTime != null && user.getDateDerniereModification() != null) {
                        java.time.Instant firestoreInstant = firestoreTime.toSqlTimestamp().toInstant();
                        java.time.Instant postgresInstant = user.getDateDerniereModification();

                        // Si Postgres est plus récent, on ignore l'import pour cet utilisateur
                        if (postgresInstant.isAfter(firestoreInstant)) {
                            System.out.println("⏳ Sync ignorée pour " + email + " (Postgres est plus récent : "
                                    + postgresInstant + " > " + firestoreInstant + ")");
                            continue;
                        }
                    }
                } else {
                    user = new Utilisateur();
                    user.setFirebaseUid(firebaseUid);
                    user.setEmail(email);
                    user.setDateCreation(java.time.Instant.now());
                    user.setRole(roleRepository.findByNom("UTILISATEUR").orElse(null));
                    isNew = true;
                }

                // Mettre à jour l'email au cas où il aurait changé
                user.setEmail(email);

                // Synchroniser le mot de passe si présent
                if (document.getString("motDePasse") != null) {
                    user.setMotDePasse(document.getString("motDePasse"));
                } else if (isNew) {
                    user.setMotDePasse("default_password");
                }

                // Synchroniser le statut depuis Firestore
                String firestoreStatut = document.getString("statut");
                if (firestoreStatut != null) {
                    final String statusToSearch = firestoreStatut;
                    user.setStatutActuel(statutUtilisateurRepository.findByNom(statusToSearch)
                            .orElseGet(() -> statutUtilisateurRepository.findByNom("ACTIF").orElse(null)));
                } else if (isNew) {
                    user.setStatutActuel(statutUtilisateurRepository.findByNom("ACTIF").orElse(null));
                }

                // Synchroniser les tentatives de connexion
                Long tentatives = document.getLong("tentatives_connexion");
                if (tentatives != null) {
                    user.setTentativesConnexion(tentatives.intValue());
                }

                // Mettre à jour la date de modification locale avec celle de Firestore
                com.google.cloud.Timestamp firestoreTime = document.getTimestamp("date_derniere_modification");
                if (firestoreTime != null) {
                    user.setDateDerniereModification(firestoreTime.toSqlTimestamp().toInstant());
                }

                utilisateurRepository.save(user);
                if (isNew)
                    createdUsers++;
                else
                    updatedUsers++;
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
     * Synchronise les utilisateurs de PostgreSQL vers Firestore.
     */
    public Map<String, Integer> syncUsersFromPostgresToFirestore() {
        int syncedUsers = 0;
        try {
            List<Utilisateur> users = utilisateurRepository.findAll();
            for (Utilisateur user : users) {
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
     * Synchronise un utilisateur spécifique vers Firestore.
     */
    public void syncUserToFirestore(Utilisateur user) {
        try {
            CollectionReference usersCol = firestore.collection("utilisateurs");
            Map<String, Object> data = new HashMap<>();
            data.put("postgresId", user.getId().toString());
            data.put("firebaseUid", user.getFirebaseUid());
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
            data.put("derniereConnexion",
                    user.getDerniereConnexion() != null ? user.getDerniereConnexion().toString() : null);
            data.put("date_derniere_modification",
                    user.getDateDerniereModification() != null
                            ? com.google.cloud.Timestamp.of(java.sql.Timestamp.from(user.getDateDerniereModification()))
                            : null);
            data.put("date_deblocage_automatique",
                    user.getDateDeblocageAutomatique() != null ? user.getDateDeblocageAutomatique().toString() : null);

            // LOG POUR DEBUG : On affiche ce qu'on envoie
            System.out.println(
                    "📤 Sync vers Firestore [" + user.getEmail() + "] - FirebaseUID: " + user.getFirebaseUid());

            // Utiliser le Firebase UID comme ID de document si disponible, sinon l'ID
            // Postgres
            String documentId = user.getFirebaseUid() != null ? user.getFirebaseUid() : user.getId().toString();
            usersCol.document(documentId).set(data).get();
        } catch (Exception e) {
            System.err.println("Erreur lors de la synchronisation de l'utilisateur " + user.getEmail()
                    + " vers Firestore : " + e.getMessage());
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
            System.err
                    .println("Erreur lors de la synchronisation des configurations vers Firestore : " + e.getMessage());
        }
    }

    /**
     * Synchronise les données de Firestore vers PostgreSQL.
     */
    public Map<String, Integer> syncFromFirestoreToPostgres() {
        int syncedSignalements = 0;
        int createdUsers = 0;

        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("signalements").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (QueryDocumentSnapshot document : documents) {
                String idFirebase = document.getId();

                // Vérifier si le signalement existe déjà dans Postgres
                if (signalementRepository.findByIdFirebase(idFirebase).isPresent()) {
                    continue;
                }

                // Extraire les données
                Double latitude = document.getDouble("latitude");
                Double longitude = document.getDouble("longitude");
                String description = document.getString("description");
                String statutNom = document.getString("statut");

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

                Object budgetObj = document.get("budget");
                BigDecimal budget = null;
                if (budgetObj != null && !budgetObj.toString().isEmpty()) {
                    try {
                        budget = new BigDecimal(budgetObj.toString());
                    } catch (Exception e) {
                        System.err.println("Erreur conversion budget pour doc " + idFirebase + " : " + budgetObj);
                    }
                }

                // Créer le signalement dans Postgres
                Signalement s = new Signalement();
                s.setIdFirebase(idFirebase);
                s.setLatitude(latitude != null ? latitude : 0.0);
                s.setLongitude(longitude != null ? longitude : 0.0);
                s.setDateSignalement(dateSignalement);
                s.setDateDerniereModification(dateSignalement); // Par défaut la même que la création pour un import initial

                // Gérer le statut
                String finalStatutNom = (statutNom != null) ? statutNom.toLowerCase() : "nouveau";
                s.setStatut(statutRepository.findByNom(finalStatutNom)
                        .orElseGet(() -> {
                            var newStatut = new com.cloud.identity.entities.StatutsSignalement();
                            newStatut.setNom(finalStatutNom);
                            return statutRepository.save(newStatut);
                        }));

                // Gérer l'utilisateur (Priorité au Firebase UID car il est stable même si
                // l'email change)
                String firebaseUidUtilisateur = document.getString("firebase_uid_utilisateur");
                if (firebaseUidUtilisateur != null && !firebaseUidUtilisateur.isEmpty()) {
                    s.setFirebaseUidUtilisateur(firebaseUidUtilisateur); // On stocke le UID dans le signalement
                                                                         // Postgres
                    utilisateurRepository.findByFirebaseUid(firebaseUidUtilisateur).ifPresent(s::setUtilisateur);
                }

                // Fallback sur l'email si le Firebase UID n'est pas présent
                if (s.getUtilisateur() == null) {
                    String emailUtilisateur = document.getString("email_utilisateur");
                    if (emailUtilisateur != null && !emailUtilisateur.isEmpty()) {
                        utilisateurRepository.findByEmail(emailUtilisateur).ifPresent(s::setUtilisateur);
                    }
                }

                // Fallback sur l'ancien champ utilisateur_id si l'email n'est pas présent
                if (s.getUtilisateur() == null) {
                    String utilisateurIdStr = document.getString("utilisateur_id");
                    if (utilisateurIdStr != null && !utilisateurIdStr.isEmpty()) {
                        try {
                            java.util.UUID utilisateurId = java.util.UUID.fromString(utilisateurIdStr);
                            utilisateurRepository.findById(utilisateurId).ifPresent(s::setUtilisateur);
                        } catch (Exception e) {
                            System.err.println("ID utilisateur invalide dans Firestore: " + utilisateurIdStr);
                        }
                    }
                }

                // Fallback sur l'objet utilisateur imbriqué (si présent)
                if (s.getUtilisateur() == null) {
                    Map<String, Object> userMap = (Map<String, Object>) document.get("utilisateur");
                    if (userMap != null && userMap.get("email") != null) {
                        String email = (String) userMap.get("email");
                        utilisateurRepository.findByEmail(email).ifPresent(s::setUtilisateur);
                    }
                }

                // Gérer le type de signalement
                Object typeIdObj = document.get("id_type_signalement");
                if (typeIdObj != null) {
                    try {
                        Integer typeId = Integer.valueOf(typeIdObj.toString());
                        s.setType(typeSignalementRepository.findById(typeId).orElse(null));
                    } catch (Exception e) {
                        System.err.println("Erreur conversion type_id pour doc " + idFirebase + " : " + typeIdObj);
                    }
                }

                s = signalementRepository.save(s);

                // Détails
                SignalementsDetail details = new SignalementsDetail();
                details.setSignalement(s);
                details.setDescription(description);
                details.setSurfaceM2(surfaceM2);
                details.setBudget(budget);
                
                if (entrepriseConcerne != null && !entrepriseConcerne.isEmpty()) {
                    final String finalEntrepriseNom = entrepriseConcerne;
                    details.setEntreprise(entrepriseRepository.findByNom(finalEntrepriseNom)
                        .orElseGet(() -> {
                            Entreprise e = new Entreprise();
                            e.setNom(finalEntrepriseNom);
                            return entrepriseRepository.save(e);
                        }));
                }
                
                details.setPhotoUrl(photoUrl);

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
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("postgresId", signalement.getId().toString());
            data.put("latitude", signalement.getLatitude());
            data.put("longitude", signalement.getLongitude());
            data.put("dateSignalement",
                    signalement.getDateSignalement() != null ? signalement.getDateSignalement().toString() : null);
            data.put("date_derniere_modification",
                    signalement.getDateDerniereModification() != null ? signalement.getDateDerniereModification().toString() : null);

            if (signalement.getStatut() != null) {
                data.put("statut", signalement.getStatut().getNom());
            }

            if (signalement.getType() != null) {
                data.put("id_type_signalement", signalement.getType().getId());
            }

            if (signalement.getUtilisateur() != null) {
                Map<String, Object> userMap = new HashMap<>();
                userMap.put("email", signalement.getUtilisateur().getEmail());
                data.put("utilisateur", userMap);
            }

            if (details != null) {
                data.put("description", details.getDescription());

                // Write both camelCase and snake_case for compatibility
                data.put("surfaceM2", details.getSurfaceM2());
                data.put("surface_m2", details.getSurfaceM2());

                data.put("budget", details.getBudget() != null ? details.getBudget().toString() : null);

                String entrepriseNom = details.getEntreprise() != null ? details.getEntreprise().getNom() : null;
                data.put("entrepriseConcerne", entrepriseNom);
                data.put("entreprise_concerne", entrepriseNom);

                data.put("photoUrl", details.getPhotoUrl());
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
            if (signalement.getDateDerniereModification() != null) {
                updates.put("date_derniere_modification", signalement.getDateDerniereModification().toString());
            }
            updates.put("postgresId", signalement.getId().toString());
            updates.put("latitude", signalement.getLatitude());
            updates.put("longitude", signalement.getLongitude());

            // On met à jour le document Firebase correspondant
            firestore.collection("signalements")
                    .document(signalement.getIdFirebase())
                    .update(updates);

            System.out.println("Synchronisation Firebase réussie pour le signalement : " + signalement.getId());
        } catch (Exception e) {
            System.err.println("Erreur lors de la synchronisation Firebase : " + e.getMessage());
        }
    }

    /**
     * Synchronise tous les types de signalement vers Firestore.
     */
    public String syncTypesSignalementToFirestore() {
        try {
            List<TypeSignalement> types = typeSignalementRepository.findAll();
            for (TypeSignalement type : types) {
                syncSingleTypeSignalementToFirestore(type);
            }
            return "✅ " + types.size() + " types de signalement synchronisés vers Firestore.";
        } catch (Exception e) {
            return "❌ Erreur sync types signalement : " + e.getMessage();
        }
    }

    /**
     * Synchronise un seul type de signalement vers Firestore.
     */
    public void syncSingleTypeSignalementToFirestore(TypeSignalement type) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("id", type.getId());
            data.put("nom", type.getNom());
            data.put("icone_path", type.getIconePath()); // Changé de "icone" à "icone_path" pour correspondre au
                                                         // frontend mobile
            data.put("couleur", type.getCouleur());

            firestore.collection("types_signalement")
                    .document(type.getId().toString())
                    .set(data).get();
        } catch (Exception e) {
            System.err.println("Erreur sync type " + type.getNom() + " : " + e.getMessage());
        }
    }

    /**
     * Supprime un type de signalement dans Firestore.
     */
    public void deleteTypeSignalementInFirestore(Integer id) {
        try {
            firestore.collection("types_signalement")
                    .document(id.toString())
                    .delete().get();
        } catch (Exception e) {
            System.err.println("Erreur suppression type signalement " + id + " : " + e.getMessage());
        }
    }

    /**
     * Supprime un utilisateur dans Firestore.
     */
    public void deleteUserInFirestore(String idOrEmail) {
        try {
            // On essaie de supprimer par ID de document (qui est souvent l'ID Postgres ou
            // l'email)
            firestore.collection("utilisateurs")
                    .document(idOrEmail)
                    .delete().get();
        } catch (Exception e) {
            System.err.println("Erreur suppression utilisateur " + idOrEmail + " : " + e.getMessage());
        }
    }
}
