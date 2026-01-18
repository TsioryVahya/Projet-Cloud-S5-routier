package com.cloud.identity.service;

import com.cloud.identity.model.Utilisateur;
import com.cloud.identity.repository.UtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UtilisateurRepository utilisateurRepository;

    public Optional<Utilisateur> login(String email, String password) {
        Optional<Utilisateur> userOpt = utilisateurRepository.findByEmail(email);
        
        if (userOpt.isPresent()) {
            Utilisateur user = userOpt.get();
            
            if ("BLOQUE".equals(user.getStatut())) {
                throw new RuntimeException("Compte bloqué");
            }
            
            if (user.getMotDePasse().equals(password)) {
                user.setTentativesConnexion(0);
                utilisateurRepository.save(user);
                return Optional.of(user);
            } else {
                user.setTentativesConnexion(user.getTentativesConnexion() + 1);
                if (user.getTentativesConnexion() >= 3) {
                    user.setStatut("BLOQUE");
                }
                utilisateurRepository.save(user);
            }
        }
        return Optional.empty();
    }

    public Utilisateur register(Utilisateur user) {
        if (utilisateurRepository.findByEmail(user.getEmail()).isPresent()) {
            throw new RuntimeException("Email déjà utilisé");
        }
        return utilisateurRepository.save(user);
    }
    
    public void unblockUser(String email) {
        Utilisateur user = utilisateurRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("Utilisateur non trouvé"));
        user.setStatut("ACTIF");
        user.setTentativesConnexion(0);
        utilisateurRepository.save(user);
    }
}
