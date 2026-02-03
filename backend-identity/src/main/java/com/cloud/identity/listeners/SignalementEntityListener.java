package com.cloud.identity.listeners;
import com.cloud.identity.config.SpringContextHelper;
import com.cloud.identity.entities.Signalement;
import com.cloud.identity.service.FirestoreSyncService;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PostPersist;

public class SignalementEntityListener {

    @PostUpdate
    @PostPersist
    public void onPostUpdate(Signalement signalement) {
        System.out.println("⚡ SignalementEntityListener : Détection d'un changement sur " + signalement.getId());
        try {
            // Récupérer le service via le helper car le listener n'est pas géré par Spring
            FirestoreSyncService syncService = SpringContextHelper.getBean(FirestoreSyncService.class);
            if (syncService != null) {
                System.out.println("🔄 Déclenchement de la synchro Firebase pour " + signalement.getId());
                syncService.syncSignalementToFirebase(signalement);
            } else {
                System.err.println("❌ Impossible de récupérer FirestoreSyncService");
            }
        } catch (Exception e) {
            System.err.println("❌ Erreur dans SignalementEntityListener : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
