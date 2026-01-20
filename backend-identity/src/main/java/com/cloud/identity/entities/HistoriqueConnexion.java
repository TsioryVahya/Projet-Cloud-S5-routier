package com.cloud.identity.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "historique_connexions")
public class HistoriqueConnexion {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(name = "date_tentative")
    private LocalDateTime dateTentative = LocalDateTime.now();

    @Column(nullable = false)
    private Boolean succes;

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "raison_echec")
    private String raisonEchec;

    // Getters and Setters
    public Integer getId() { return id; }
    public void setId(Integer id) { this.id = id; }
    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }
    public LocalDateTime getDateTentative() { return dateTentative; }
    public void setDateTentative(LocalDateTime dateTentative) { this.dateTentative = dateTentative; }
    public Boolean getSucces() { return succes; }
    public void setSucces(Boolean succes) { this.succes = succes; }
    public String getIpAddress() { return ipAddress; }
    public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public String getRaisonEchec() { return raisonEchec; }
    public void setRaisonEchec(String raisonEchec) { this.raisonEchec = raisonEchec; }
}
