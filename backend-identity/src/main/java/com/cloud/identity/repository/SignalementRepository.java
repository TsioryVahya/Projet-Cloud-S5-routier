package com.cloud.identity.repository;

import com.cloud.identity.entities.Signalement;
import org.springframework.data.jpa.repository.JpaRepository;
public interface SignalementRepository extends JpaRepository<Signalement, Integer> {
}
