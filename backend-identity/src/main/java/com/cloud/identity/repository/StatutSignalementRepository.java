package com.cloud.identity.repository;

import com.cloud.identity.entities.StatutSignalement;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatutSignalementRepository extends JpaRepository<StatutSignalement, Integer> {
}
