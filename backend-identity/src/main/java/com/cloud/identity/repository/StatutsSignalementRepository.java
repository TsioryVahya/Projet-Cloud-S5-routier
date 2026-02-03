package com.cloud.identity.repository;

import com.cloud.identity.entities.StatutsSignalement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface StatutsSignalementRepository extends JpaRepository<StatutsSignalement, Integer> {
    Optional<StatutsSignalement> findByNom(String nom);
}
