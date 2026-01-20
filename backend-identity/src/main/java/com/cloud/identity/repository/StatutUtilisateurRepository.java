package com.cloud.identity.repository;

import com.cloud.identity.entities.StatutUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface StatutUtilisateurRepository extends JpaRepository<StatutUtilisateur, Integer> {
    Optional<StatutUtilisateur> findByNom(String nom);
}
