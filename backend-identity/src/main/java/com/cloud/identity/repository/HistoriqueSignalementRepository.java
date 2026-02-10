package com.cloud.identity.repository;

import com.cloud.identity.entities.HistoriqueSignalement;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Map;

public interface HistoriqueSignalementRepository extends JpaRepository<HistoriqueSignalement, Integer> {
    
    @Query(value = "WITH transition_times AS ( " +
           "    SELECT " +
           "        h1.signalement_id, " +
           "        h1.date_changement as start_date, " +
           "        (SELECT MIN(h2.date_changement) FROM historique_signalement h2 " +
           "         WHERE h2.signalement_id = h1.signalement_id AND h2.date_changement > h1.date_changement) as end_date, " +
           "        s.nom as statut_nom " +
           "    FROM historique_signalement h1 " +
           "    JOIN statuts_signalement s ON h1.statut_id = s.id " +
           ") " +
           "SELECT statut_nom, AVG(EXTRACT(EPOCH FROM (end_date - start_date))) / 3600 as avg_hours " +
           "FROM transition_times " +
           "WHERE end_date IS NOT NULL " +
           "GROUP BY statut_nom", nativeQuery = true)
    List<Map<String, Object>> getAverageProcessingTimeByStatus();
}
