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
            return ResponseEntity.ok(syncService.syncTypesSignalementToFirestore());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public List<TypeSignalement> getAll() {
        return typeSignalementRepository.findAll();
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
