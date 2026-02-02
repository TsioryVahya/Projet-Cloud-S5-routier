package com.cloud.identity.controller;

import com.cloud.identity.entities.TypeSignalement;
import com.cloud.identity.repository.TypeSignalementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/types-signalement")
@CrossOrigin(origins = "*")
public class TypeSignalementController {

    @Autowired
    private TypeSignalementRepository typeSignalementRepository;

    @GetMapping
    public List<TypeSignalement> getAll() {
        return typeSignalementRepository.findAll();
    }
}
