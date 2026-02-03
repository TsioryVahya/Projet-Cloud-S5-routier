package com.cloud.identity.controller;

import com.cloud.identity.service.SignalementService;
import com.cloud.identity.repository.StatutSignalementRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.UUID;

@Controller
@RequestMapping("/signalements")
public class SignalementViewController {

    @Autowired
    private SignalementService signalementService;

    @Autowired
    private StatutSignalementRepository statutSignalementRepository;

    @GetMapping
    public String liste() {
        return "listeSignalements";
    }

    @GetMapping("/nouveau")
    public String nouveau() {
        return "nouveauSignalement";
    }

    @GetMapping("/modifier/{id}")
    public String modifier(@PathVariable UUID id, Model model) {
        model.addAttribute("signalementId", id);
        return "modifierSignalement";
    }
}
