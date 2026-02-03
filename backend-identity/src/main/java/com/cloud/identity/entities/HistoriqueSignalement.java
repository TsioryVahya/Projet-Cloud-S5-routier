package com.cloud.identity.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "historique_signalement")
public class HistoriqueSignalement {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @OnDelete(action = OnDeleteAction.CASCADE)
    @JoinColumn(name = "signalement_id")
    private Signalement signalement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "statut_id")
    private StatutsSignalement statut;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "date_changement")
    private Instant dateChangement;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Signalement getSignalement() {
        return signalement;
    }

    public void setSignalement(Signalement signalement) {
        this.signalement = signalement;
    }

    public StatutsSignalement getStatut() {
        return statut;
    }

    public void setStatut(StatutsSignalement statut) {
        this.statut = statut;
    }

    public Instant getDateChangement() {
        return dateChangement;
    }

    public void setDateChangement(Instant dateChangement) {
        this.dateChangement = dateChangement;
    }

}
