package com.cloud.identity.controller;

import com.cloud.identity.entities.Utilisateur;
import com.cloud.identity.repository.UtilisateurRepository;
import com.cloud.identity.service.FirestoreSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/utilisateurs")
@CrossOrigin(origins = "*")
public class UtilisateurController {

    @Autowired
    private UtilisateurRepository repository;

    @Autowired
    private FirestoreSyncService syncService;

    @PostMapping("/sync")
    public ResponseEntity<?> sync() {
        try {
            return ResponseEntity.ok(syncService.syncUsersFromFirestoreToPostgres());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/sync-to-firebase")
    public ResponseEntity<?> syncToFirebase() {
        try {
            return ResponseEntity.ok(syncService.syncUsersFromPostgresToFirestore());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(java.util.Map.of("error", e.getMessage()));
        }
    }

    @GetMapping
    public List<Utilisateur> getAll() {
        return repository.findAll();
    }

    @GetMapping("/blocked")
    public List<Utilisateur> getBlocked() {
        return repository.findByStatutActuelNom("BLOQUE");
    }

    @GetMapping("/{firebaseUid}")
    public ResponseEntity<Utilisateur> getByFirebaseUid(@PathVariable String firebaseUid) {
        return repository.findByFirebaseUid(firebaseUid)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Utilisateur create(@RequestBody Utilisateur entity) {
        if (entity.getDateDerniereModification() == null) {
            entity.setDateDerniereModification(java.time.Instant.now());
        }
        return repository.save(entity);
    }

    @PutMapping("/{firebaseUid}")
    public ResponseEntity<Utilisateur> update(@PathVariable String firebaseUid, @RequestBody Utilisateur entity) {
        Utilisateur existingUser = repository.findByFirebaseUid(firebaseUid).orElse(null);
        if (existingUser == null) return ResponseEntity.notFound().build();
        
        // Mettre à jour les champs de l'objet existant au lieu de sauvegarder l'objet reçu
        existingUser.setEmail(entity.getEmail());
        if (entity.getMotDePasse() != null && !entity.getMotDePasse().isEmpty()) {
            existingUser.setMotDePasse(entity.getMotDePasse());
        }
        existingUser.setRole(entity.getRole());
        existingUser.setStatutActuel(entity.getStatutActuel());
        existingUser.setDateDerniereModification(java.time.Instant.now());
        
        return ResponseEntity.ok(repository.save(existingUser));
    }

    @DeleteMapping("/{firebaseUid}")
    public ResponseEntity<Void> delete(@PathVariable String firebaseUid) {
        return repository.findByFirebaseUid(firebaseUid).map(user -> {
            // Supprimer de Firestore d'abord (en utilisant l'ID et l'email pour être sûr)
            if (user.getFirebaseUid() != null) {
                syncService.deleteUserInFirestore(user.getFirebaseUid());
            }
            syncService.deleteUserInFirestore(user.getEmail());

            // Supprimer de Postgres
            repository.delete(user);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
