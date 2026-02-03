package com.cloud.identity.controller;

import com.cloud.identity.entities.StatutUtilisateur;
import com.cloud.identity.repository.StatutUtilisateurRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/statuts-utilisateur")
@CrossOrigin(origins = "*")
public class StatutUtilisateurController {

    @Autowired
    private StatutUtilisateurRepository repository;

    @GetMapping
    public List<StatutUtilisateur> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<StatutUtilisateur> getById(@PathVariable Integer id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public StatutUtilisateur create(@RequestBody StatutUtilisateur entity) {
        return repository.save(entity);
    }

    @PutMapping("/{id}")
    public ResponseEntity<StatutUtilisateur> update(@PathVariable Integer id, @RequestBody StatutUtilisateur entity) {
        if (!repository.existsById(id))
            return ResponseEntity.notFound().build();
        entity.setId(id);
        return ResponseEntity.ok(repository.save(entity));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Integer id) {
        if (!repository.existsById(id))
            return ResponseEntity.notFound().build();
        repository.deleteById(id);
        return ResponseEntity.ok().build();
    }
}
