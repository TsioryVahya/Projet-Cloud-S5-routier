package com.cloud.identity.repository;

import com.cloud.identity.entities.HistoriqueSignalement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface HistoriqueSignalementRepository extends JpaRepository<HistoriqueSignalement, Integer> {
}
