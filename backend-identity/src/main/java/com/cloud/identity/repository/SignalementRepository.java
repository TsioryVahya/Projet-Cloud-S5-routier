package com.cloud.identity.repository;

import com.cloud.identity.entities.Signalement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import java.util.List;

@Repository
public interface SignalementRepository extends JpaRepository<Signalement, UUID> {
    Optional<Signalement> findByIdFirebase(String idFirebase);

    @Query("SELECT DISTINCT s FROM Signalement s LEFT JOIN FETCH s.details LEFT JOIN FETCH s.statut LEFT JOIN FETCH s.utilisateur LEFT JOIN FETCH s.type")
    List<Signalement> findAllWithDetails();
}
