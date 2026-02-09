package com.cloud.identity.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "utilisateurs")
public class Utilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "email", nullable = false)
    private String email;

    @Column(name = "mot_de_passe", nullable = false)
    private String motDePasse;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "role_id")
    private Role role;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "statut_actuel_id")
    private StatutUtilisateur statutActuel;

    @ColumnDefault("0")
    @Column(name = "tentatives_connexion")
    private Integer tentativesConnexion;

    @Column(name = "date_dernier_echec_connexion")
    private Instant dateDernierEchecConnexion;

    @Column(name = "date_deblocage_automatique")
    private Instant dateDeblocageAutomatique;

    @Column(name = "derniere_connexion")
    private Instant derniereConnexion;

    @Column(name = "date_derniere_modification")
    private Instant dateDerniereModification;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "date_creation")
    private Instant dateCreation;

    @Column(name = "firebase_uid")
    private String firebaseUid;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMotDePasse() {
        return motDePasse;
    }

    public void setMotDePasse(String motDePasse) {
        this.motDePasse = motDePasse;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public StatutUtilisateur getStatutActuel() {
        return statutActuel;
    }

    public void setStatutActuel(StatutUtilisateur statutActuel) {
        this.statutActuel = statutActuel;
    }

    public Integer getTentativesConnexion() {
        return tentativesConnexion;
    }

    public void setTentativesConnexion(Integer tentativesConnexion) {
        this.tentativesConnexion = tentativesConnexion;
    }

    public Instant getDateDernierEchecConnexion() {
        return dateDernierEchecConnexion;
    }

    public void setDateDernierEchecConnexion(Instant dateDernierEchecConnexion) {
        this.dateDernierEchecConnexion = dateDernierEchecConnexion;
    }

    public Instant getDateDeblocageAutomatique() {
        return dateDeblocageAutomatique;
    }

    public void setDateDeblocageAutomatique(Instant dateDeblocageAutomatique) {
        this.dateDeblocageAutomatique = dateDeblocageAutomatique;
    }

    public Instant getDerniereConnexion() {
        return derniereConnexion;
    }

    public void setDerniereConnexion(Instant derniereConnexion) {
        this.derniereConnexion = derniereConnexion;
    }

    public Instant getDateDerniereModification() {
        return dateDerniereModification;
    }

    public void setDateDerniereModification(Instant dateDerniereModification) {
        this.dateDerniereModification = dateDerniereModification;
    }

    public Instant getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(Instant dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getFirebaseUid() {
        return firebaseUid;
    }

    public void setFirebaseUid(String firebaseUid) {
        this.firebaseUid = firebaseUid;
    }
}