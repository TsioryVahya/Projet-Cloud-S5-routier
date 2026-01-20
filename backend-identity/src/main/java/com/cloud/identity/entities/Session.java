package com.cloud.identity.entities;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "sessions")
public class Session {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @Column(name = "token_acces", nullable = false)
    private String tokenAcces;

    @Column(name = "refresh_token")
    private String refreshToken;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation = LocalDateTime.now();

    @Column(name = "date_expiration", nullable = false)
    private LocalDateTime dateExpiration;

    @Column(name = "date_derniere_activite")
    private LocalDateTime dateDerniereActivite = LocalDateTime.now();

    @Column(name = "ip_connexion")
    private String ipConnexion;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "est_active")
    private Boolean estActive = true;

    // Getters and Setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public Utilisateur getUtilisateur() { return utilisateur; }
    public void setUtilisateur(Utilisateur utilisateur) { this.utilisateur = utilisateur; }
    public String getTokenAcces() { return tokenAcces; }
    public void setTokenAcces(String tokenAcces) { this.tokenAcces = tokenAcces; }
    public String getRefreshToken() { return refreshToken; }
    public void setRefreshToken(String refreshToken) { this.refreshToken = refreshToken; }
    public LocalDateTime getDateCreation() { return dateCreation; }
    public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
    public LocalDateTime getDateExpiration() { return dateExpiration; }
    public void setDateExpiration(LocalDateTime dateExpiration) { this.dateExpiration = dateExpiration; }
    public LocalDateTime getDateDerniereActivite() { return dateDerniereActivite; }
    public void setDateDerniereActivite(LocalDateTime dateDerniereActivite) { this.dateDerniereActivite = dateDerniereActivite; }
    public String getIpConnexion() { return ipConnexion; }
    public void setIpConnexion(String ipConnexion) { this.ipConnexion = ipConnexion; }
    public String getUserAgent() { return userAgent; }
    public void setUserAgent(String userAgent) { this.userAgent = userAgent; }
    public Boolean getEstActive() { return estActive; }
    public void setEstActive(Boolean estActive) { this.estActive = estActive; }
}
