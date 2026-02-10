package com.cloud.identity.repository;

import com.cloud.identity.entities.Signalement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.Query;
import java.util.List;
import java.util.Map;

@Repository
public interface SignalementRepository extends JpaRepository<Signalement, UUID> {
    Optional<Signalement> findByIdFirebase(String idFirebase);

    @Query("SELECT DISTINCT s FROM Signalement s LEFT JOIN FETCH s.details LEFT JOIN FETCH s.statut LEFT JOIN FETCH s.utilisateur LEFT JOIN FETCH s.type LEFT JOIN FETCH s.galerie")
    List<Signalement> findAllWithDetails();

    @Query(value = "SELECT " +
           "    COUNT(s.id) as nb_points, " +
           "    COALESCE(SUM(sd.surface_m2), 0) as total_surface, " +
           "    COALESCE(SUM(sd.budget), 0) as total_budget, " +
           "    ROUND(CAST( " +
           "        (COUNT(CASE WHEN st.nom IN ('terminé', 'termine', 'achevé', 'acheve') THEN 1 END) * 100.0 + " +
           "         COUNT(CASE WHEN st.nom IN ('en cours', 'en_cours') THEN 1 END) * 50.0) / " +
           "        NULLIF(COUNT(s.id), 0) AS numeric), 2) as avancement_pourcentage " +
           "FROM signalements s " +
           "LEFT JOIN signalements_details sd ON s.id = sd.signalement_id " +
           "LEFT JOIN statuts_signalement st ON s.statut_id = st.id", nativeQuery = true)
    Map<String, Object> getRecapitulatifGlobal();
}
