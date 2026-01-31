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
    private EntrepriseRepository entrepriseRepository;

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
                if (email == null || email.isEmpty())
                    continue;

                // On cherche d'abord par email pour voir s'il existe déjà dans Postgres
                Optional<Utilisateur> existingUserOpt = utilisateurRepository.findByEmail(email);

                Utilisateur user;
                if (existingUserOpt.isPresent()) {
                    user = existingUserOpt.get();
                    System.out.println("🔄 Mise à jour utilisateur existant (sync Firestore -> Postgres) : " + email);
                } else {
                    user = new Utilisateur();
                    user.setEmail(email);
                    user.setDateCreation(java.time.Instant.now());
                    System.out.println("➕ Création nouvel utilisateur (sync Firestore -> Postgres) : " + email);
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
            }
        } catch (Exception e) {
            System.err.println(
                    "Erreur lors de la synchronisation des utilisateurs Firestore -> Postgres : " + e.getMessage());
        }

        Map<String, Integer> result = new HashMap<>();
        result.put("utilisateurs", createdUsers);
        return result;
    }

    /**
     * Synchronise un utilisateur unique de PostgreSQL vers Firestore.
     */
    public void syncSingleUserToFirestore(Utilisateur user) {
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
        int syncedUsers = 0;
        try {
            List<Utilisateur> users = utilisateurRepository.findAll();
            for (Utilisateur user : users) {
                syncSingleUserToFirestore(user);
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
                Optional<Signalement> existingSignalement = signalementRepository.findByIdFirebase(idFirebase);
                if (existingSignalement.isPresent()) {
                    // Si le signalement existe déjà, on ne fait rien pour l'instant
                    // (on évite d'écraser les données locales par des données Firestore
                    // potentiellement incomplètes)
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
                System.out.println("👤 Vérification de l'utilisateur pour email : " + finalEmail);

                // On cherche l'utilisateur par email. S'il existe, on l'utilise.
                Optional<Utilisateur> userOpt = utilisateurRepository.findByEmail(finalEmail);
                if (userOpt.isPresent()) {
                    System.out.println("✅ Utilisateur existant trouvé : " + finalEmail);
                    s.setUtilisateur(userOpt.get());
                } else {
                    System.out.println("➕ Création d'un nouvel utilisateur : " + finalEmail);
                    var newUser = new com.cloud.identity.entities.Utilisateur();
                    newUser.setEmail(finalEmail);
                    newUser.setMotDePasse("default_password");

                    // Attribuer un rôle par défaut (UTILISATEUR)
                    roleRepository.findByNom("UTILISATEUR").ifPresent(newUser::setRole);

                    // Attribuer un statut par défaut (ACTIF)
                    statutUtilisateurRepository.findByNom("ACTIF").ifPresent(newUser::setStatutActuel);

                    newUser.setDateCreation(java.time.Instant.now());
                    s.setUtilisateur(utilisateurRepository.save(newUser));
                }

                s = signalementRepository.save(s);

                // Détails
                SignalementsDetail details = new SignalementsDetail();
                details.setSignalement(s);
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
