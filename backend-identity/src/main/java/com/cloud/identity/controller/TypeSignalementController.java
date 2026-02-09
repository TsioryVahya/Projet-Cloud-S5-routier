package com.cloud.identity.controller;

import com.cloud.identity.entities.TypeSignalement;
import com.cloud.identity.repository.TypeSignalementRepository;
import com.cloud.identity.service.FirestoreSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/types-signalement")
@CrossOrigin(origins = "*")
public class TypeSignalementController {

    @Autowired
    private TypeSignalementRepository typeSignalementRepository;

    @Autowired
    private FirestoreSyncService syncService;

    @PostMapping("/sync-to-firebase")
    public ResponseEntity<?> syncToFirebase() {
        try {
            String message = syncService.syncTypesSignalementToFirestore();
            return ResponseEntity.ok(java.util.Map.of("message", message));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public ResponseEntity<?> getAll() {
        try {
            return ResponseEntity.ok(typeSignalementRepository.findAll());
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of("error", e.getMessage() != null ? e.getMessage() : "Unknown error", "type", e.getClass().getName()));
        }
    }

    @GetMapping("/{id}")
    public TypeSignalement getById(@PathVariable Integer id) {
        return typeSignalementRepository.findById(id).orElse(null);
    }

    @PostMapping
    public TypeSignalement create(@RequestBody TypeSignalement typeSignalement) {
        TypeSignalement saved = typeSignalementRepository.save(typeSignalement);
        syncService.syncSingleTypeSignalementToFirestore(saved);
        return saved;
    }

    @PutMapping("/{id}")
    public TypeSignalement update(@PathVariable Integer id, @RequestBody TypeSignalement typeSignalement) {
        typeSignalement.setId(id);
        TypeSignalement updated = typeSignalementRepository.save(typeSignalement);
        syncService.syncSingleTypeSignalementToFirestore(updated);
        return updated;
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Integer id) {
        syncService.deleteTypeSignalementInFirestore(id);
        typeSignalementRepository.deleteById(id);
    }
}
