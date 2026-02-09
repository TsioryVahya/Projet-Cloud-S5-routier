package com.cloud.identity.entities;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "signalements")
@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })
public class Signalement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @Column(name = "id_firebase")
    private String idFirebase;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "date_signalement")
    private Instant dateSignalement;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "statut_id")
    private StatutsSignalement statut;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "type_id")
    private TypeSignalement type;

    @Column(name = "latitude", nullable = false)
    private Double latitude;

    @Column(name = "longitude", nullable = false)
    private Double longitude;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(name = "firebase_uid_utilisateur")
    private String firebaseUidUtilisateur;

    @Column(name = "date_derniere_modification")
    private Instant dateDerniereModification;

    @OneToOne(mappedBy = "signalement", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private SignalementsDetail details;

    public SignalementsDetail getDetails() {
        return details;
    }

    public void setDetails(SignalementsDetail details) {
        this.details = details;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getIdFirebase() {
        return idFirebase;
    }

    public void setIdFirebase(String idFirebase) {
        this.idFirebase = idFirebase;
    }

    public Instant getDateSignalement() {
        return dateSignalement;
    }

    public void setDateSignalement(Instant dateSignalement) {
        this.dateSignalement = dateSignalement;
    }

    public StatutsSignalement getStatut() {
        return statut;
    }

    public void setStatut(StatutsSignalement statut) {
        this.statut = statut;
    }

    public TypeSignalement getType() {
        return type;
    }

    public void setType(TypeSignalement type) {
        this.type = type;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public String getFirebaseUidUtilisateur() {
        return firebaseUidUtilisateur;
    }

    public void setFirebaseUidUtilisateur(String firebaseUidUtilisateur) {
        this.firebaseUidUtilisateur = firebaseUidUtilisateur;
    }

    public Instant getDateDerniereModification() {
        return dateDerniereModification;
    }

    public void setDateDerniereModification(Instant dateDerniereModification) {
        this.dateDerniereModification = dateDerniereModification;
    }

}
