package com.cloud.identity.events;

import com.cloud.identity.entities.Signalement;
import com.cloud.identity.service.FirestoreSyncService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

@Component
public class SignalementEventListener {

    @Autowired
    private FirestoreSyncService firestoreSyncService;

    @Async
    @EventListener
    public void handleSignalementSaved(SignalementSavedEvent event) {
        Signalement signalement = event.getSignalement();
        System.out.println("⚡ SignalementEventListener : Détection d'un changement sur " + signalement.getId());

        try {
            System.out.println("🔄 Déclenchement de la synchro Firebase pour " + signalement.getId());
            firestoreSyncService.syncSignalementToFirebase(signalement);
        } catch (Exception e) {
            System.err.println("❌ Erreur dans SignalementEventListener : " + e.getMessage());
            e.printStackTrace();
        }
    }
}
