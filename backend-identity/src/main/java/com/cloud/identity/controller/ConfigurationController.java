package com.cloud.identity.controller;

import com.cloud.identity.entities.Configuration;
import com.cloud.identity.repository.ConfigurationRepository;
import com.cloud.identity.service.FirestoreSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/configurations")
@CrossOrigin(origins = "*")
public class ConfigurationController {

    @Autowired
    private ConfigurationRepository repository;

    @Autowired
    private FirestoreSyncService syncService;

    @PostMapping("/sync-to-firebase")
    public ResponseEntity<?> syncToFirebase() {
        try {
            syncService.syncConfigurationsToFirestore();
            return ResponseEntity.ok().body("{\"message\": \"Configurations synchronisées vers Firestore\"}");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public List<Configuration> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{cle}")
    public ResponseEntity<Configuration> getById(@PathVariable String cle) {
        return repository.findById(cle)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Configuration create(@RequestBody Configuration entity) {
        return repository.save(entity);
    }

    @PutMapping("/{cle}")
    public ResponseEntity<Configuration> update(@PathVariable String cle, @RequestBody Configuration entity) {
        if (!repository.existsById(cle)) return ResponseEntity.notFound().build();
        entity.setCle(cle);
        return ResponseEntity.ok(repository.save(entity));
    }

    @DeleteMapping("/{cle}")
    public ResponseEntity<Void> delete(@PathVariable String cle) {
        if (!repository.existsById(cle)) return ResponseEntity.notFound().build();
        repository.deleteById(cle);
        return ResponseEntity.ok().build();
    }
}
