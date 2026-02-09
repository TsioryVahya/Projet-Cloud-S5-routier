package com.cloud.identity.controller;

import com.cloud.identity.dto.SignalementDTO;
import com.cloud.identity.entities.Signalement;
import com.cloud.identity.entities.SignalementsDetail;
import com.cloud.identity.repository.SignalementsDetailRepository;
import com.cloud.identity.service.SignalementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/signalements")
@CrossOrigin(origins = "*")
public class SignalementController {

    @Autowired
    private SignalementService signalementService;

    @Autowired
    private SignalementsDetailRepository detailsRepository;

    @PostMapping("/sync")
    public ResponseEntity<?> sync() {
        try {
            Map<String, Integer> result = signalementService.synchroniserDonnees();
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            return ResponseEntity.ok(signalementService.getAllSignalements());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error", "type", e.getClass().getName()));
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<Signalement> getById(@PathVariable UUID id) {
        return signalementService.getSignalementById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Map<String, Object> data) {
        try {
            Double latitude = data.get("latitude") != null ? Double.valueOf(data.get("latitude").toString()) : null;
            Double longitude = data.get("longitude") != null ? Double.valueOf(data.get("longitude").toString()) : null;
            String description = (String) data.get("description");
            String email = (String) data.get("email");
            
            Double surfaceM2 = data.get("surfaceM2") != null ? Double.valueOf(data.get("surfaceM2").toString()) : 
                               (data.get("surface_m2") != null ? Double.valueOf(data.get("surface_m2").toString()) : null);
            
            BigDecimal budget = data.get("budget") != null ? new BigDecimal(data.get("budget").toString()) : null;
            
            String entrepriseConcerne = data.get("entrepriseConcerne") != null ? (String) data.get("entrepriseConcerne") : (String) data.get("entreprise_concerne");
            
            String photoUrl = data.get("photoUrl") != null ? (String) data.get("photoUrl") : (String) data.get("photo_url");

            Integer typeId = data.get("typeId") != null ? Integer.valueOf(data.get("typeId").toString()) : 
                             (data.get("type_id") != null ? Integer.valueOf(data.get("type_id").toString()) : null);

            signalementService.creerSignalement(latitude, longitude, description, email, surfaceM2, budget, entrepriseConcerne, photoUrl, typeId);
            return ResponseEntity.ok(Map.of("message", "Signalement créé avec succès"));
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error", "type", e.getClass().getName()));
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable UUID id, @RequestBody Map<String, Object> data) {
        try {
            Double latitude = data.get("latitude") != null ? Double.valueOf(data.get("latitude").toString()) : null;
            Double longitude = data.get("longitude") != null ? Double.valueOf(data.get("longitude").toString()) : null;
            Integer statutId = data.get("statutId") != null ? Integer.valueOf(data.get("statutId").toString()) : 
                               (data.get("statut_id") != null ? Integer.valueOf(data.get("statut_id").toString()) : null);
            
            String description = (String) data.get("description");
            
            Double surfaceM2 = data.get("surfaceM2") != null ? Double.valueOf(data.get("surfaceM2").toString()) : 
                               (data.get("surface_m2") != null ? Double.valueOf(data.get("surface_m2").toString()) : null);
            
            BigDecimal budget = data.get("budget") != null ? new BigDecimal(data.get("budget").toString()) : null;
            
            String entrepriseConcerne = data.get("entrepriseConcerne") != null ? (String) data.get("entrepriseConcerne") : (String) data.get("entreprise_concerne");
            
            String photoUrl = data.get("photoUrl") != null ? (String) data.get("photoUrl") : (String) data.get("photo_url");

            Integer typeId = data.get("typeId") != null ? Integer.valueOf(data.get("typeId").toString()) : 
                             (data.get("type_id") != null ? Integer.valueOf(data.get("type_id").toString()) : null);

            signalementService.modifierSignalement(id, latitude, longitude, statutId, description, surfaceM2, budget, entrepriseConcerne, photoUrl, typeId);
            return ResponseEntity.ok("Signalement modifié avec succès");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable UUID id) {
        try {
            signalementService.supprimerSignalement(id);
            return ResponseEntity.ok("Signalement supprimé avec succès");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }
}
