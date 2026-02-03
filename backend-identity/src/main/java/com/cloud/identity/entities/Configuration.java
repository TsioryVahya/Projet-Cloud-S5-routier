package com.cloud.identity.entities;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "configurations")
public class Configuration {
    @Id
    @Column(length = 100)
    private String cle;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String valeur;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "date_modification")
    private Instant dateModification = Instant.now();

    // Getters and Setters
    public String getCle() { return cle; }
    public void setCle(String cle) { this.cle = cle; }
    public String getValeur() { return valeur; }
    public void setValeur(String valeur) { this.valeur = valeur; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Instant getDateModification() { return dateModification; }
    public void setDateModification(Instant dateModification) { this.dateModification = dateModification; }
}
