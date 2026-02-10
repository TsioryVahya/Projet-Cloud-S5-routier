package com.cloud.identity.repository;

import com.cloud.identity.entities.GalerieSignalement;
import com.cloud.identity.entities.Signalement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface GalerieSignalementRepository extends JpaRepository<GalerieSignalement, UUID> {
    List<GalerieSignalement> findBySignalement(Signalement signalement);
}
