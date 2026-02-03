package com.cloud.identity.repository;

import com.cloud.identity.entities.HistoriqueConnexion;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoriqueConnexionRepository extends JpaRepository<HistoriqueConnexion, Integer> {
}