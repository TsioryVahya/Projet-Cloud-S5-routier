package com.cloud.identity.controller;

import com.cloud.identity.entities.HistoriqueSignalement;
import com.cloud.identity.repository.HistoriqueSignalementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/historique-signalements")
@CrossOrigin(origins = "*")
public class HistoriqueSignalementController {

    @Autowired
    private HistoriqueSignalementRepository repository;

    @GetMapping
    public List<HistoriqueSignalement> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<HistoriqueSignalement> getById(@PathVariable Integer id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public HistoriqueSignalement create(@RequestBody HistoriqueSignalement entity) {
        return repository.save(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!repository.existsById(id)) return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
