package com.cloud.identity.service;

import com.cloud.identity.entities.*;
import com.cloud.identity.repository.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@Service
public class AuthService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private StatutUtilisateurRepository statutRepository;

    @Autowired
    private SessionRepository sessionRepository;

    @Autowired
    private HistoriqueConnexionRepository historiqueRepository;

    @Autowired
    private ConfigurationRepository configurationRepository;

    @Autowired
    private FirestoreSyncService firestoreSyncService;

    @Transactional
    public Optional<Session> login(String email, String password) {
        return login(email, password, null, null);
    }

    @Transactional
    public Optional<Session> login(String email, String password, String ipAddress, String userAgent) {
        Optional<Utilisateur> userOpt = utilisateurRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            Utilisateur user = userOpt.get();

            // Vérifier si le compte est bloqué
            if (user.getStatutActuel() != null && "BLOQUE".equals(user.getStatutActuel().getNom())) {
                // Vérifier si le déblocage automatique est possible
                if (user.getDateDeblocageAutomatique() != null
                        && user.getDateDeblocageAutomatique().isBefore(Instant.now())) {
                    unblockUser(user);
                } else {
                    saveHistorique(user, false, ipAddress, userAgent, "compte_bloque");
                    throw new RuntimeException("Compte bloqué");
                }
            }

            if (user.getMotDePasse().equals(password)) {
                // Succès
                user.setTentativesConnexion(0);
                user.setDerniereConnexion(Instant.now());
                utilisateurRepository.save(user);

                // Synchroniser le succès de connexion (dernière connexion) vers Firestore
                try {
                    firestoreSyncService.syncUserToFirestore(user);
                } catch (Exception e) {
                }

                saveHistorique(user, true, ipAddress, userAgent, null);

                return Optional.of(createSession(user, ipAddress, userAgent));
            } else {
                // Échec
                int maxTentatives = Integer.parseInt(getConfig("max_tentatives_connexion", "3"));
                user.setTentativesConnexion(
                        (user.getTentativesConnexion() != null ? user.getTentativesConnexion() : 0) + 1);
                user.setDateDernierEchecConnexion(Instant.now());

                if (user.getTentativesConnexion() >= maxTentatives) {
                    blockUser(user);
                    saveHistorique(user, false, ipAddress, userAgent, "mot_de_passe_incorrect_blocage");
                } else {
                    saveHistorique(user, false, ipAddress, userAgent, "mot_de_passe_incorrect");
                }

                utilisateurRepository.save(user);

                // Synchroniser l'échec/blocage vers Firestore
                try {
                    firestoreSyncService.syncUserToFirestore(user);
                } catch (Exception e) {
                }
            }
        }
        return Optional.empty();
    }

    private void saveHistorique(Utilisateur user, boolean succes, String ip, String ua, String raison) {
        HistoriqueConnexion h = new HistoriqueConnexion();
        h.setUtilisateur(user);
        h.setSucces(succes);
        h.setIpAddress(ip);
        h.setUserAgent(ua);
        h.setRaisonEchec(raison);
        historiqueRepository.save(h);
    }

    private Session createSession(Utilisateur user, String ip, String ua) {
        double dureeHeures = Double.parseDouble(getConfig("duree_session_heures", "24"));

        Session session = new Session();
        session.setUtilisateur(user);
        session.setTokenAcces(UUID.randomUUID().toString());
        session.setRefreshToken(UUID.randomUUID().toString());
        session.setIpConnexion(ip);
        session.setUserAgent(ua);
        session.setDateExpiration(Instant.now().plusSeconds((long) (dureeHeures * 3600)));
        return sessionRepository.save(session);
    }

    private String getConfig(String cle, String parDefaut) {
        return configurationRepository.findById(cle)
                .map(Configuration::getValeur)
                .orElse(parDefaut);
    }

    private void blockUser(Utilisateur user) {
        StatutUtilisateur bloque = statutRepository.findByNom("BLOQUE")
                .orElseThrow(() -> new RuntimeException("Statut BLOQUE non trouvé"));
        user.setStatutActuel(bloque);
        user.setDateDerniereModification(Instant.now());

        int minutesBlocage = Integer.parseInt(getConfig("duree_blocage_minutes", "15"));
        user.setDateDeblocageAutomatique(Instant.now().plusSeconds(minutesBlocage * 60));
    }

    @Transactional
    public void unblockUser(String email) {
        System.out.println("🔓 Déblocage manuel de l'utilisateur : " + email);
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        unblockUser(user);
    }

    private void unblockUser(Utilisateur user) {
        StatutUtilisateur actif = statutRepository.findByNom("ACTIF")
                .orElseThrow(() -> new RuntimeException("Statut ACTIF non trouvé"));
        user.setStatutActuel(actif);
        user.setTentativesConnexion(0);
        user.setDateDeblocageAutomatique(null);
        user.setDateDerniereModification(Instant.now());
        utilisateurRepository.save(user);

        // Synchroniser automatiquement vers Firestore
        try {
            firestoreSyncService.syncUserToFirestore(user);
        } catch (Exception e) {
            System.err.println("Erreur sync auto après déblocage: " + e.getMessage());
        }
    }

    @Transactional
    public Utilisateur register(Utilisateur user) {
        if (utilisateurRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email déjà utilisé");
        }

        if (user.getRole() == null) {
            Role defaultRole = roleRepository.findByNom("UTILISATEUR")
                    .orElseThrow(() -> new RuntimeException("Rôle par défaut non trouvé"));
            user.setRole(defaultRole);
        }

        if (user.getStatutActuel() == null) {
            StatutUtilisateur defaultStatut = statutRepository.findByNom("ACTIF")
                    .orElseThrow(() -> new RuntimeException("Statut par défaut non trouvé"));
            user.setStatutActuel(defaultStatut);
        }

        user.setDateDerniereModification(Instant.now());
        Utilisateur saved = utilisateurRepository.save(user);

        // Synchroniser vers Firestore
        try {
            firestoreSyncService.syncUserToFirestore(saved);
        } catch (Exception e) {
            System.err.println("Erreur lors de la synchronisation Firestore après inscription : " + e.getMessage());
        }

        return saved;
    }
}
