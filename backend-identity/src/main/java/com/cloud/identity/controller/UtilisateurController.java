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
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @PostMapping("/sync-to-firebase")
    public ResponseEntity<?> syncToFirebase() {
        try {
            return ResponseEntity.ok(syncService.syncUsersFromPostgresToFirestore());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @GetMapping
    public List<Utilisateur> getAll() {
        return repository.findAll();
    }

    @GetMapping("/{id}")
    public ResponseEntity<Utilisateur> getById(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping
    public Utilisateur create(@RequestBody Utilisateur entity) {
        Utilisateur saved = repository.save(entity);
        // Synchroniser immédiatement vers Firebase
        syncService.syncSingleUserToFirestore(saved);
        return saved;
    }

    @PutMapping("/{id}")
    public ResponseEntity<Utilisateur> update(@PathVariable UUID id, @RequestBody Utilisateur entity) {
        return repository.findById(id).map(existingUser -> {
            String oldEmail = existingUser.getEmail();
            existingUser.setEmail(entity.getEmail());

            // Ne mettre à jour le mot de passe que s'il est fourni et non vide
            if (entity.getMotDePasse() != null && !entity.getMotDePasse().trim().isEmpty()) {
                existingUser.setMotDePasse(entity.getMotDePasse());
            }

            if (entity.getRole() != null) {
                existingUser.setRole(entity.getRole());
            }

            if (entity.getStatutActuel() != null) {
                existingUser.setStatutActuel(entity.getStatutActuel());
            }

            Utilisateur updated = repository.save(existingUser);

            // Si l'email a changé, on gère la cascade dans Firestore
            if (!oldEmail.equals(updated.getEmail())) {
                // 1. Supprimer l'ancien document utilisateur (ID était l'email auparavant)
                syncService.deleteUserInFirestore(oldEmail);

                // 2. Mettre à jour l'email dans tous les signalements de cet utilisateur
                syncService.updateEmailInFirestoreSignalements(oldEmail, updated.getEmail());
            }

            // Synchroniser immédiatement vers Firebase pour éviter le bug de "retour
            // arrière"
            syncService.syncSingleUserToFirestore(updated);

            return ResponseEntity.ok(updated);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        return repository.findById(id).map(user -> {
            // Supprimer de Firestore d'abord (en utilisant l'ID et l'email pour être sûr)
            syncService.deleteUserInFirestore(user.getId().toString());
            syncService.deleteUserInFirestore(user.getEmail());

            // Supprimer de Postgres
            repository.delete(user);
            return ResponseEntity.ok().<Void>build();
        }).orElse(ResponseEntity.notFound().build());
    }
}
