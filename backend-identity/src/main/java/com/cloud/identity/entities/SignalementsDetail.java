package com.cloud.identity.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "signalements_details")
public class SignalementsDetail {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "signalement_id")
    private Signalement signalement;

    @Column(name = "description", length = Integer.MAX_VALUE)
    private String description;

    @Column(name = "surface_m2")
    private Double surfaceM2;

    @Column(name = "budget", precision = 15, scale = 2)
    private BigDecimal budget;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "entreprise_id")
    private Entreprise entreprise;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "galerie_id")
    private GalerieSignalement galerie;

    @Column(name = "niveau")
    private Integer niveau;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Signalement getSignalement() {
        return signalement;
    }

    public void setSignalement(Signalement signalement) {
        this.signalement = signalement;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Double getSurfaceM2() {
        return surfaceM2;
    }

    public void setSurfaceM2(Double surfaceM2) {
        this.surfaceM2 = surfaceM2;
    }

    public BigDecimal getBudget() {
        return budget;
    }

    public void setBudget(BigDecimal budget) {
        this.budget = budget;
    }

    public Entreprise getEntreprise() {
        return entreprise;
    }

    public void setEntreprise(Entreprise entreprise) {
        this.entreprise = entreprise;
    }

    public GalerieSignalement getGalerie() {
        return galerie;
    }

    public void setGalerie(GalerieSignalement galerie) {
        this.galerie = galerie;
    }

    public Integer getNiveau() {
        return niveau;
    }

    public void setNiveau(Integer niveau) {
        this.niveau = niveau;
    }
}