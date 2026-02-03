package com.cloud.identity.controller;

import com.cloud.identity.entities.Entreprise;
import com.cloud.identity.repository.EntrepriseRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/entreprises")
@CrossOrigin(origins = "*")
public class EntrepriseController {

    @Autowired
    private EntrepriseRepository entrepriseRepository;

    @GetMapping
    public List<Entreprise> getAll() {
        return entrepriseRepository.findAll();
    }
}
