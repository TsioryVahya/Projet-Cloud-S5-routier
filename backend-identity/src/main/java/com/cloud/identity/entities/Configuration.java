package com.cloud.identity.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

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
    private LocalDateTime dateModification = LocalDateTime.now();

    // Getters and Setters
    public String getCle() { return cle; }
    public void setCle(String cle) { this.cle = cle; }
    public String getValeur() { return valeur; }
    public void setValeur(String valeur) { this.valeur = valeur; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public LocalDateTime getDateModification() { return dateModification; }
    public void setDateModification(LocalDateTime dateModification) { this.dateModification = dateModification; }
}
