package com.cloud.identity.repository;

import com.cloud.identity.entities.PrixM2;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PrixM2Repository extends JpaRepository<PrixM2, Integer> {
}
