package com.cloud.identity.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "utilisateurs")
public class Utilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @ManyToOne
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne
    @JoinColumn(name = "statut_actuel_id")
    private StatutUtilisateur statutActuel;

    @Column(name = "tentatives_connexion")
    private int tentativesConnexion = 0;

    @Column(name = "date_dernier_echec_connexion")
    private LocalDateTime dateDernierEchecConnexion;

    @Column(name = "date_deblocage_automatique")
    private LocalDateTime dateDeblocageAutomatique;

    @Column(name = "derniere_connexion")
    private LocalDateTime derniereConnexion;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getMotDePasse() { return motDePasse; }
    public void setMotDePasse(String motDePasse) { this.motDePasse = motDePasse; }
    public Role getRole() { return role; }
    public void setRole(Role role) { this.role = role; }
    public StatutUtilisateur getStatutActuel() { return statutActuel; }
    public void setStatutActuel(StatutUtilisateur statutActuel) { this.statutActuel = statutActuel; }
    public int getTentativesConnexion() { return tentativesConnexion; }
    public void setTentativesConnexion(int tentativesConnexion) { this.tentativesConnexion = tentativesConnexion; }
    public LocalDateTime getDateDernierEchecConnexion() { return dateDernierEchecConnexion; }
    public void setDateDernierEchecConnexion(LocalDateTime dateDernierEchecConnexion) { this.dateDernierEchecConnexion = dateDernierEchecConnexion; }
    public LocalDateTime getDateDeblocageAutomatique() { return dateDeblocageAutomatique; }
    public void setDateDeblocageAutomatique(LocalDateTime dateDeblocageAutomatique) { this.dateDeblocageAutomatique = dateDeblocageAutomatique; }
    public LocalDateTime getDerniereConnexion() { return derniereConnexion; }
    public void setDerniereConnexion(LocalDateTime derniereConnexion) { this.derniereConnexion = derniereConnexion; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
}
