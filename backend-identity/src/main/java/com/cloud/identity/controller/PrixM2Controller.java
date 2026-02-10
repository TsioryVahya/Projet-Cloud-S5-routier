package com.cloud.identity.controller;

import com.cloud.identity.entities.PrixM2;
import com.cloud.identity.repository.PrixM2Repository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prix-m2")
@CrossOrigin(origins = "*")
public class PrixM2Controller {

    @Autowired
    private PrixM2Repository prixM2Repository;

    @GetMapping
    public List<PrixM2> getAll() {
        return prixM2Repository.findAll();
    }

    @PostMapping
    public PrixM2 create(@RequestBody PrixM2 prixM2) {
        return prixM2Repository.save(prixM2);
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Integer id, @RequestBody PrixM2 prixM2Details) {
        return prixM2Repository.findById(id)
                .map(prixM2 -> {
                    prixM2.setMontant(prixM2Details.getMontant());
                    prixM2.setDateDebut(prixM2Details.getDateDebut());
                    prixM2.setDateFin(prixM2Details.getDateFin());
                    PrixM2 updated = prixM2Repository.save(prixM2);
                    return ResponseEntity.ok(updated);
                })
                .orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Integer id) {
        return prixM2Repository.findById(id)
                .map(prixM2 -> {
                    prixM2Repository.delete(prixM2);
                    return ResponseEntity.ok(Map.of("message", "Prix supprimé avec succès"));
                })
                .orElse(ResponseEntity.notFound().build());
    }
}
