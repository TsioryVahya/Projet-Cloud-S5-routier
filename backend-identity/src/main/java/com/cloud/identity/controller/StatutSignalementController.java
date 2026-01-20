package com.cloud.identity.controller;

import com.cloud.identity.entities.StatutSignalement;
import com.cloud.identity.repository.StatutSignalementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statuts-signalement")
@CrossOrigin(origins = "*")
public class StatutSignalementController {

    @Autowired
    private StatutSignalementRepository repository;

    @GetMapping
    public List<StatutSignalement> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StatutSignalement> getById(@PathVariable Integer id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public StatutSignalement create(@RequestBody StatutSignalement entity) {
        return repository.save(entity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StatutSignalement> update(@PathVariable Integer id, @RequestBody StatutSignalement entity) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        entity.setId(id);
        return ResponseEntity.ok(repository.save(entity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
