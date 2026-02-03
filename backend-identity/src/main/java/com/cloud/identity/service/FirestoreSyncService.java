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

                Utilisateur user;
                boolean isNew = false;

                if (userOpt.isPresent()) {
                    user = userOpt.get();
                    
                    // --- LOGIQUE DE COMPARAISON DES DATES (Solution 2) ---
                    com.google.cloud.Timestamp firestoreTime = document.getTimestamp("date_derniere_modification");
                    if (firestoreTime != null && user.getDateDerniereModification() != null) {
                        java.time.Instant firestoreInstant = firestoreTime.toSqlTimestamp().toInstant();
                        java.time.Instant postgresInstant = user.getDateDerniereModification();
                        
                        // Si Postgres est plus récent, on ignore l'import pour cet utilisateur
                        if (postgresInstant.isAfter(firestoreInstant)) {
                            System.out.println("⏳ Sync ignorée pour " + email + " (Postgres est plus récent : " + postgresInstant + " > " + firestoreInstant + ")");
                            continue; 
                        }
                    }
                } else {
                    user = new Utilisateur();
                    // Si on a un postgresIdStr mais qu'il n'existe pas en base, on peut soit l'ignorer, 
                    // soit le créer avec cet ID. Ici on le crée avec cet ID si possible.
                    if (postgresIdStr != null && !postgresIdStr.isEmpty()) {
                        try {
                            user.setId(java.util.UUID.fromString(postgresIdStr));
                        } catch (Exception e) {}
                    }
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
                if (isNew) createdUsers++;
                else updatedUsers++;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la synchronisation des utilisateurs Firestore -> Postgres : " + e.getMessage());
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
            System.err.println("Erreur lors de la synchronisation des utilisateurs Postgres -> Firestore : " + e.getMessage());
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
                if (dateObj == null) dateObj = document.get("dateSignalement");
                
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
                if (photoUrl == null) photoUrl = document.getString("photo_url");
                
                Double surfaceM2 = getAsDouble(document, "surfaceM2");
                if (surfaceM2 == null) surfaceM2 = getAsDouble(document, "surface_m2");
                
                String entrepriseConcerne = document.getString("entrepriseConcerne");
                if (entrepriseConcerne == null) entrepriseConcerne = document.getString("entreprise_concerne");
                
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

                // Créer le signalement dans Postgres
                Signalement s = new Signalement();
                s.setIdFirebase(idFirebase);
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

                // Gérer l'utilisateur
                final String finalEmail = email;
                s.setUtilisateur(utilisateurRepository.findByEmail(finalEmail)
                        .orElseGet(() -> {
                            var newUser = new com.cloud.identity.entities.Utilisateur();
                            newUser.setEmail(finalEmail);
                            newUser.setMotDePasse("default_password");
                            return utilisateurRepository.save(newUser);
                        }));

                s = signalementRepository.save(s);

                // Détails
                SignalementsDetail details = new SignalementsDetail();
                details.setSignalement(s);
                details.setDescription(description);
                details.setSurfaceM2(surfaceM2);
                details.setBudget(budget);
                details.setEntrepriseConcerne(entrepriseConcerne);
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
        if (val instanceof Double) return (Double) val;
        if (val instanceof Long) return ((Long) val).doubleValue();
        if (val instanceof Integer) return ((Integer) val).doubleValue();
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
     * Crée un nouveau signalement dans Firestore à partir d'un signalement PostgreSQL.
     */
    public String createSignalementInFirestore(Signalement signalement, SignalementsDetail details) {
        try {
            Map<String, Object> data = new HashMap<>();
            data.put("postgresId", signalement.getId().toString());
            data.put("latitude", signalement.getLatitude());
            data.put("longitude", signalement.getLongitude());
            data.put("dateSignalement", signalement.getDateSignalement() != null ? signalement.getDateSignalement().toString() : null);
            
            if (signalement.getStatut() != null) {
                data.put("statut", signalement.getStatut().getNom());
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
                
                data.put("entrepriseConcerne", details.getEntrepriseConcerne());
                data.put("entreprise_concerne", details.getEntrepriseConcerne());
                
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
}
