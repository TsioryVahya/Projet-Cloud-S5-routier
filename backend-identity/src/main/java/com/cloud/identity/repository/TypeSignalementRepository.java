package com.cloud.identity.repository;

import com.cloud.identity.entities.TypeSignalement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TypeSignalementRepository extends JpaRepository<TypeSignalement, Integer> {
    Optional<TypeSignalement> findByNom(String nom);
}
