package com.cloud.identity.repository;

import com.cloud.identity.entities.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
import java.util.UUID;

public interface SessionRepository extends JpaRepository<Session, UUID> {
    Optional<Session> findByTokenAcces(String tokenAcces);
}
