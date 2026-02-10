package com.cloud.identity.entities;

import jakarta.persistence.*;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.GenericGenerator;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "galerie_signalement")
public class GalerieSignalement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", nullable = false)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_signalement")
    private Signalement signalement;

    @Column(name = "photo_url", nullable = false, length = Integer.MAX_VALUE)
    private String photoUrl;

    @ColumnDefault("CURRENT_TIMESTAMP")
    @Column(name = "date_ajout")
    private Instant dateAjout;

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

    public String getPhotoUrl() {
        return photoUrl;
    }

    public void setPhotoUrl(String photoUrl) {
        this.photoUrl = photoUrl;
    }

    public Instant getDateAjout() {
        return dateAjout;
    }

    public void setDateAjout(Instant dateAjout) {
        this.dateAjout = dateAjout;
    }
}
