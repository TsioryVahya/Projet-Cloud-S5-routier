package com.cloud.identity.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;

@Entity
@Table(name = "historique_utilisateur")
public class HistoriqueUtilisateur {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "utilisateur_id")
    private Utilisateur utilisateur;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statut_id")
    private StatutsUtilisateur statut;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "date_changement")
    private Instant dateChangement;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Utilisateur getUtilisateur() {
        return utilisateur;
    }

    public void setUtilisateur(Utilisateur utilisateur) {
        this.utilisateur = utilisateur;
    }

    public StatutsUtilisateur getStatut() {
        return statut;
    }

    public void setStatut(StatutsUtilisateur statut) {
        this.statut = statut;
    }

    public Instant getDateChangement() {
        return dateChangement;
    }

    public void setDateChangement(Instant dateChangement) {
        this.dateChangement = dateChangement;
    }

}