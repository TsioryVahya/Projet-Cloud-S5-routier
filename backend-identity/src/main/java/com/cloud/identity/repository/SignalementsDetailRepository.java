package com.cloud.identity.repository;

import com.cloud.identity.entities.Signalement;
import com.cloud.identity.entities.SignalementsDetail;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface SignalementsDetailRepository extends JpaRepository<SignalementsDetail, UUID> {
    Optional<SignalementsDetail> findBySignalement(Signalement signalement);
}
