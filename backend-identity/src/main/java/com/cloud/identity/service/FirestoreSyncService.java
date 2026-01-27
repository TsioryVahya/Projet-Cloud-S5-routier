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

    /**
     * Synchronise les utilisateurs de Firestore vers PostgreSQL.
     */
    public Map<String, Integer> syncUsersFromFirestoreToPostgres() {
        int createdUsers = 0;
        try {
            ApiFuture<QuerySnapshot> future = firestore.collection("utilisateurs").get();
            List<QueryDocumentSnapshot> documents = future.get().getDocuments();

            for (QueryDocumentSnapshot document : documents) {
                String email = document.getString("email");
                if (email == null || email.isEmpty()) continue;

                if (utilisateurRepository.findByEmail(email).isPresent()) {
                    continue;
                }

                Utilisateur user = new Utilisateur();
                user.setEmail(email);
                user.setMotDePasse(document.getString("motDePasse") != null ? document.getString("motDePasse") : "default_password");
                
                // Rôle par défaut : UTILISATEUR
                user.setRole(roleRepository.findByNom("UTILISATEUR").orElse(null));
                
                // Statut par défaut : ACTIF
                user.setStatutActuel(statutUtilisateurRepository.findByNom("ACTIF").orElse(null));
                
                user.setDateCreation(java.time.Instant.now());
                
                utilisateurRepository.save(user);
                createdUsers++;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la synchronisation des utilisateurs Firestore -> Postgres : " + e.getMessage());
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("utilisateurs", createdUsers);
        return result;
    }

    /**
     * Synchronise les utilisateurs de PostgreSQL vers Firestore.
     */
    public Map<String, Integer> syncUsersFromPostgresToFirestore() {
        int syncedUsers = 0;
        try {
            List<Utilisateur> users = utilisateurRepository.findAll();
            CollectionReference usersCol = firestore.collection("utilisateurs");

            for (Utilisateur user : users) {
                Map<String, Object> data = new HashMap<>();
                data.put("postgresId", user.getId().toString());
                data.put("email", user.getEmail());
                data.put("motDePasse", user.getMotDePasse());
                
                if (user.getRole() != null) {
                    data.put("role", user.getRole().getNom());
                }
                
                if (user.getStatutActuel() != null) {
                    data.put("statut", user.getStatutActuel().getNom());
                }
                
                data.put("dateCreation", user.getDateCreation() != null ? user.getDateCreation().toString() : null);
                data.put("derniereConnexion", user.getDerniereConnexion() != null ? user.getDerniereConnexion().toString() : null);

                // Utiliser l'email comme ID de document dans Firestore pour éviter les doublons
                // ou un ID basé sur l'email si on veut rester consistent.
                usersCol.document(user.getEmail()).set(data).get();
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
