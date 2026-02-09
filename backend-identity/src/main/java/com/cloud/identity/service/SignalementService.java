package com.cloud.identity.service;

import com.cloud.identity.dto.SignalementDTO;
import com.cloud.identity.entities.Entreprise;
import com.cloud.identity.entities.Signalement;
import com.cloud.identity.entities.SignalementsDetail;
import com.cloud.identity.entities.StatutsSignalement;
import com.cloud.identity.entities.TypeSignalement;
import com.cloud.identity.entities.Utilisateur;
import com.cloud.identity.repository.EntrepriseRepository;
import com.cloud.identity.repository.SignalementRepository;
import com.cloud.identity.repository.SignalementsDetailRepository;
import com.cloud.identity.repository.StatutsSignalementRepository;
import com.cloud.identity.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import com.cloud.identity.repository.TypeSignalementRepository;
import com.cloud.identity.repository.UtilisateurRepository;

@Service
public class SignalementService {

    @Autowired
    private SignalementRepository signalementRepository;

    @Autowired
    private SignalementsDetailRepository detailsRepository;

    @Autowired
    private StatutsSignalementRepository statutRepository;

    @Autowired
    private TypeSignalementRepository typeSignalementRepository;

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private FirestoreSyncService firestoreSyncService;

    @Autowired
    private FcmNotificationService fcmNotificationService;

    @Transactional
    public Map<String, Integer> synchroniserDonnees() {
        System.out.println("🚀 Début de l'opération de synchronisation globale...");

        // 0. Synchroniser les utilisateurs d'abord (important pour lier les
        // signalements)
        System.out.println("👥 Étape 0 : Synchronisation des utilisateurs...");
        firestoreSyncService.syncUsersFromFirestoreToPostgres();

        // 1. D'abord on ramène ce qui est nouveau sur Mobile vers Postgres
        Map<String, Integer> result = firestoreSyncService.syncFromFirestoreToPostgres();
        System.out.println("✅ Étape 1 terminée : " + result.getOrDefault("signalements", 0)
                + " signalements récupérés de Firestore.");

        // 2. Ensuite on s'assure que ce qui a été modifié sur le Web est renvoyé vers
        // Firestore
        System.out.println("🔄 Étape 2 : Synchronisation des modifications locales vers Firestore...");
        List<Signalement> signalementsWithFirebase = signalementRepository.findAll();
        int syncedBack = 0;
        for (Signalement s : signalementsWithFirebase) {
            if (s.getIdFirebase() != null && !s.getIdFirebase().isEmpty()) {
                firestoreSyncService.syncSignalementToFirebase(s);
                syncedBack++;
            }
        }
        System.out.println("✅ Étape 2 terminée : " + syncedBack + " signalements mis à jour dans Firestore.");

        return result;
    }

    @Transactional(readOnly = true)
    public List<SignalementDTO> getAllSignalements() {
        return signalementRepository.findAllWithDetails().stream().map(s -> {
            SignalementDTO dto = new SignalementDTO();
            dto.setPostgresId(s.getId().toString());
            dto.setLatitude(s.getLatitude());
            dto.setLongitude(s.getLongitude());
            dto.setIdFirebase(s.getIdFirebase());
            dto.setDateSignalement(s.getDateSignalement());

            // On renvoie le NOM du statut pour le dashboard web
            if (s.getStatut() != null) {
                dto.setStatut(s.getStatut().getNom());
            } else {
                dto.setStatut("nouveau");
            }

            // Gérer le type
            if (s.getType() != null) {
                dto.setTypeNom(s.getType().getNom());
                dto.setTypeIcone(s.getType().getIconePath());
                dto.setTypeCouleur(s.getType().getCouleur());
            }

            // Récupérer les détails via la relation fetchée ou fallback repository
            SignalementsDetail d = s.getDetails();
            if (d == null) {
                // Fallback si la relation n'est pas chargée (peut arriver selon l'état de
                // l'entité)
                d = detailsRepository.findBySignalement(s).orElse(null);
            }

            if (d != null) {
                System.out.println("🔍 Signalement " + s.getId() + " - Description trouvée: " + d.getDescription());
                dto.setDescription(d.getDescription());
                dto.setSurfaceM2(d.getSurfaceM2());
                dto.setBudget(d.getBudget());
                dto.setEntrepriseId(d.getEntreprise() != null ? d.getEntreprise().getId() : null);
                dto.setEntrepriseNom(d.getEntreprise() != null ? d.getEntreprise().getNom() : null);
                dto.setPhotoUrl(d.getPhotoUrl());
            } else {
                System.out.println("⚠️ Aucun détail trouvé pour le signalement : " + s.getId());
            }

            // Récupérer l'utilisateur si disponible
            if (s.getUtilisateur() != null) {
                SignalementDTO.UtilisateurDTO userDto = new SignalementDTO.UtilisateurDTO();
                userDto.setEmail(s.getUtilisateur().getEmail());
                dto.setUtilisateur(userDto);
            }

            return dto;
        }).collect(Collectors.toList());
    }

    public Optional<Signalement> getSignalementById(UUID id) {
        return signalementRepository.findById(id);
    }

    public List<StatutsSignalement> getAllStatuts() {
        return statutRepository.findAll();
    }

    @Transactional
    public void creerSignalement(Double latitude, Double longitude, String description, String email,
            Double surfaceM2, BigDecimal budget, String entrepriseNom, String photoUrl,
            Integer typeId) throws Exception {
        Utilisateur utilisateur = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new Exception("Utilisateur non trouvé"));

        StatutsSignalement statut = statutRepository.findByNom("nouveau")
                .orElseThrow(() -> new Exception("Statut par défaut non trouvé"));

        TypeSignalement type = null;
        if (typeId != null) {
            type = typeSignalementRepository.findById(typeId)
                    .orElseThrow(() -> new Exception("Type de signalement non trouvé"));
        }

        Signalement s = new Signalement();
        s.setLatitude(latitude);
        s.setLongitude(longitude);
        s.setStatut(statut);
        s.setUtilisateur(utilisateur);
        s.setType(type);
        s.setDateSignalement(Instant.now());

        signalementRepository.save(s);

        SignalementsDetail details = new SignalementsDetail();
        details.setSignalement(s);
        details.setDescription(description);
        details.setSurfaceM2(surfaceM2);
        details.setBudget(budget);

        if (entrepriseNom != null && !entrepriseNom.isEmpty()) {
            Entreprise entreprise = entrepriseRepository.findByNom(entrepriseNom)
                    .orElseGet(() -> {
                        Entreprise e = new Entreprise();
                        e.setNom(entrepriseNom);
                        return entrepriseRepository.save(e);
                    });
            details.setEntreprise(entreprise);
        }

        details.setPhotoUrl(photoUrl);

        detailsRepository.save(details);
        s.setDetails(details);

        // Synchronisation Firebase
        String idFirebase = firestoreSyncService.createSignalementInFirestore(s, details);
        if (idFirebase != null) {
            s.setIdFirebase(idFirebase);
            signalementRepository.save(s);
        }
    }

    @Transactional
    public void modifierSignalement(UUID id, Double latitude, Double longitude, Integer statutId,
            String description, Double surfaceM2, BigDecimal budget,
            String entrepriseNom, String photoUrl, Integer typeId) throws Exception {
        Signalement s = signalementRepository.findById(id)
                .orElseThrow(() -> new Exception("Signalement non trouvé"));

        // Récupérer l'ancien statut pour la notification
        String oldStatut = s.getStatut() != null ? s.getStatut().getNom() : null;

        StatutsSignalement statut = statutRepository.findById(statutId)
                .orElseThrow(() -> new Exception("Statut non trouvé"));

        if (typeId != null) {
            TypeSignalement type = typeSignalementRepository.findById(typeId)
                    .orElseThrow(() -> new Exception("Type de signalement non trouvé"));
            s.setType(type);
        }

        s.setLatitude(latitude);
        s.setLongitude(longitude);
        s.setStatut(statut);

        signalementRepository.save(s);

        // Mettre à jour les détails
        SignalementsDetail details = detailsRepository.findBySignalement(s)
                .orElseGet(() -> {
                    SignalementsDetail newDetails = new SignalementsDetail();
                    newDetails.setSignalement(s);
                    return newDetails;
                });

        details.setDescription(description);
        details.setSurfaceM2(surfaceM2);
        details.setBudget(budget);

        if (entrepriseNom != null && !entrepriseNom.isEmpty()) {
            Entreprise entreprise = entrepriseRepository.findByNom(entrepriseNom)
                    .orElseGet(() -> {
                        Entreprise e = new Entreprise();
                        e.setNom(entrepriseNom);
                        return entrepriseRepository.save(e);
                    });
            details.setEntreprise(entreprise);
        } else {
            details.setEntreprise(null);
        }

        if (photoUrl != null && !photoUrl.isEmpty()) {
            details.setPhotoUrl(photoUrl);
        }

        s.setDetails(details);
        detailsRepository.save(details);

        // Synchronisation Firebase
        firestoreSyncService.syncSignalementToFirebase(s);

        // Envoyer une notification si le statut a changé
        String newStatut = statut.getNom();
        if (oldStatut != null && !oldStatut.equals(newStatut)) {
            sendStatusChangeNotification(s, oldStatut, newStatut);
        }
    }

    @Transactional
    public void supprimerSignalement(UUID id) throws Exception {
        if (!signalementRepository.existsById(id)) {
            throw new Exception("Signalement non trouvé");
        }
        signalementRepository.deleteById(id);
    }

    @Transactional
    public void enregistrerSignalement(SignalementDTO dto) {
        // Vérifier si le signalement existe déjà par son ID Firebase
        if (signalementRepository.findByIdFirebase(dto.getIdFirebase()).isPresent()) {
            System.out.println("Signalement déjà existant dans Postgres : " + dto.getIdFirebase());
            return;
        }

        System.out.println("Enregistrement d'un nouveau signalement depuis Firebase : " + dto.getIdFirebase());

        // 1️⃣ Enregistrer signalement de base
        Signalement s = new Signalement();
        s.setLatitude(dto.getLatitude());
        s.setLongitude(dto.getLongitude());
        s.setIdFirebase(dto.getIdFirebase());

        if (dto.getDateSignalement() != null) {
            try {
                Object dateObj = dto.getDateSignalement();
                if (dateObj instanceof java.util.Date) {
                    s.setDateSignalement(((java.util.Date) dateObj).toInstant());
                } else {
                    s.setDateSignalement(Instant.parse(dateObj.toString()));
                }
            } catch (Exception e) {
                System.err.println("Erreur lors du parsing de la date : " + dto.getDateSignalement()
                        + ". Utilisation de la date actuelle.");
                s.setDateSignalement(Instant.now());
            }
        } else {
            s.setDateSignalement(Instant.now());
        }

        // Gérer le statut
        String nomStatut = (dto.getStatut() != null) ? dto.getStatut() : "nouveau";
        StatutsSignalement statut = statutRepository.findByNom(nomStatut)
                .orElseGet(() -> {
                    StatutsSignalement newStatut = new StatutsSignalement();
                    newStatut.setNom(nomStatut);
                    return statutRepository.save(newStatut);
                });
        s.setStatut(statut);

        // Gérer le type
        if (dto.getIdTypeSignalement() != null) {
            typeSignalementRepository.findById(dto.getIdTypeSignalement()).ifPresent(s::setType);
        }

        // Gérer l'utilisateur
        String email = null;
        UUID utilisateurId = null;

        if (dto.getUtilisateurId() != null && !dto.getUtilisateurId().isEmpty()) {
            try {
                utilisateurId = UUID.fromString(dto.getUtilisateurId());
            } catch (Exception e) {
                System.err.println("Erreur conversion utilisateurId UUID : " + dto.getUtilisateurId());
            }
        }

        if (dto.getUtilisateur() != null && dto.getUtilisateur().getEmail() != null) {
            email = dto.getUtilisateur().getEmail();
        } else if (dto.getEmail() != null) {
            email = dto.getEmail();
        }

        if (utilisateurId != null) {
            Optional<Utilisateur> optUser = utilisateurRepository.findById(utilisateurId);
            if (optUser.isPresent()) {
                s.setUtilisateur(optUser.get());
            } else if (email != null) {
                // Fallback sur l'email si l'ID n'est pas trouvé
                final String finalEmail = email;
                Utilisateur utilisateur = utilisateurRepository.findByEmail(finalEmail)
                        .orElseGet(() -> {
                            Utilisateur newUser = new Utilisateur();
                            newUser.setEmail(finalEmail);
                            newUser.setMotDePasse("default_password");
                            return utilisateurRepository.save(newUser);
                        });
                s.setUtilisateur(utilisateur);
            }
        } else if (email != null) {
            final String finalEmail = email;
            Utilisateur utilisateur = utilisateurRepository.findByEmail(finalEmail)
                    .orElseGet(() -> {
                        Utilisateur newUser = new Utilisateur();
                        newUser.setEmail(finalEmail);
                        newUser.setMotDePasse("default_password");
                        return utilisateurRepository.save(newUser);
                    });
            s.setUtilisateur(utilisateur);
        }

        s = signalementRepository.save(s);

        // 2️⃣ Enregistrer détails
        SignalementsDetail details = new SignalementsDetail();
        details.setSignalement(s);
        details.setDescription(dto.getDescription());

        // Gestion de la surface (peut être Long ou Double dans Firestore)
        if (dto.getSurfaceM2() != null) {
            try {
                details.setSurfaceM2(Double.valueOf(dto.getSurfaceM2().toString()));
            } catch (Exception e) {
                System.err.println("Erreur conversion surfaceM2 : " + dto.getSurfaceM2());
            }
        }

        // Gestion du budget (peut être String ou Number)
        if (dto.getBudget() != null) {
            try {
                details.setBudget(new BigDecimal(dto.getBudget().toString()));
            } catch (Exception e) {
                System.err.println("Erreur conversion budget : " + dto.getBudget());
            }
        }

        if (dto.getEntrepriseNom() != null && !dto.getEntrepriseNom().isEmpty()) {
            final String entNom = dto.getEntrepriseNom();
            Entreprise entreprise = entrepriseRepository.findByNom(entNom)
                    .orElseGet(() -> {
                        Entreprise e = new Entreprise();
                        e.setNom(entNom);
                        return entrepriseRepository.save(e);
                    });
            details.setEntreprise(entreprise);
        }

        details.setPhotoUrl(dto.getPhotoUrl());

        s.setDetails(details);
        detailsRepository.save(details);
        System.out.println("Signalement " + dto.getIdFirebase() + " enregistré avec succès dans Postgres.");
    }

    // Valider un signalement depuis l'admin
    @Transactional
    public void validerSignalement(UUID signalementId) throws Exception {
        Signalement s = signalementRepository.findById(signalementId)
                .orElseThrow(() -> new Exception("Signalement non trouvé"));

        // Récupérer l'ancien statut
        String oldStatut = s.getStatut() != null ? s.getStatut().getNom() : null;

        StatutsSignalement statutEnCours = statutRepository.findByNom("en cours")
                .orElseGet(() -> {
                    StatutsSignalement newStatut = new StatutsSignalement();
                    newStatut.setNom("en cours");
                    return statutRepository.save(newStatut);
                });

        s.setStatut(statutEnCours);
        signalementRepository.save(s);
        // La mise à jour Firebase est maintenant automatique via
        // SignalementEntityListener

        // Envoyer une notification de changement de statut
        sendStatusChangeNotification(s, oldStatut, "en cours");
    }

    /**
     * Envoie une notification de changement de statut à l'utilisateur
     */
    private void sendStatusChangeNotification(Signalement signalement, String oldStatus, String newStatus) {
        try {
            System.out.println("🔔 sendStatusChangeNotification appelé");
            System.out.println("   - Signalement ID: " + signalement.getId());
            System.out.println("   - ID Firebase: " + signalement.getIdFirebase());
            System.out.println("   - Utilisateur: "
                    + (signalement.getUtilisateur() != null ? signalement.getUtilisateur().getEmail() : "NULL"));
            System.out.println("   - Changement: " + oldStatus + " -> " + newStatus);

            if (signalement.getUtilisateur() == null) {
                System.err.println("❌ Pas d'utilisateur associé au signalement");
                return;
            }

            if (signalement.getIdFirebase() == null || signalement.getIdFirebase().isEmpty()) {
                System.err.println("❌ Pas d'ID Firebase pour le signalement");
                return;
            }

            // Utiliser l'ID Postgres de l'utilisateur comme UID Firebase
            // car le document dans la collection "users" est nommé avec cet ID
            String userId = signalement.getUtilisateur().getId().toString();
            System.out.println("🆔 Firebase UID (Postgres ID): " + userId);

            if (userId == null || userId.isEmpty()) {
                System.err.println("❌ Impossible de déterminer l'UID Firebase (ID Postgres manquant)");
                return;
            }

            // Envoyer la notification via le service FCM
            System.out.println("📤 Envoi de la notification via FcmNotificationService...");
            fcmNotificationService.notifyStatusChange(
                    signalement.getIdFirebase(),
                    oldStatus,
                    newStatus,
                    userId);
            System.out.println("✅ Notification envoyée avec succès");
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors de l'envoi de la notification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Récupère l'ID Firebase d'un utilisateur à partir de son email
     * 
     * @deprecated Utiliser directement l'ID Postgres qui est l'ID du document dans
     *             Firestore
     */
    @Deprecated
    private String getUserFirebaseId(String email) {
        try {
            System.out.println("🔍 Recherche de l'UID Firebase pour l'email: " + email);

            com.google.cloud.firestore.Firestore db = com.google.firebase.cloud.FirestoreClient.getFirestore();
            com.google.cloud.firestore.QuerySnapshot querySnapshot = db.collection("users")
                    .whereEqualTo("email", email)
                    .limit(1)
                    .get()
                    .get();

            if (!querySnapshot.isEmpty()) {
                String uid = querySnapshot.getDocuments().get(0).getId();
                System.out.println("✅ UID trouvé: " + uid);
                return uid;
            } else {
                System.err.println("❌ Aucun utilisateur trouvé dans Firestore avec l'email: " + email);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors de la récupération de l'ID Firebase: " + e.getMessage());
            e.printStackTrace();
        }
        return null;
    }
}
