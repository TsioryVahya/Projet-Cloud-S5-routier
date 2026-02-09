package com.cloud.identity.repository;

import com.cloud.identity.entities.Utilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UtilisateurRepository extends JpaRepository<Utilisateur, UUID> {
    Optional<Utilisateur> findByEmail(String email);
    Optional<Utilisateur> findByFirebaseUid(String firebaseUid);
    List<Utilisateur> findByStatutActuelNom(String nom);
}
