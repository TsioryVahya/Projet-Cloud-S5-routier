package com.cloud.identity.listeners;

import com.cloud.identity.dto.SignalementDTO;
import com.cloud.identity.service.SignalementService;
import com.google.cloud.firestore.DocumentChange;
import com.google.cloud.firestore.Firestore;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class FirebaseSignalementListener {

    @Autowired
    private SignalementService signalementService;

    @Autowired(required = false)
    private Firestore firestore;

    @PostConstruct
    public void init() {
        System.out.println("ℹ️ Le Listener automatique Firebase est DÉSACTIVÉ (Synchronisation manuelle requise).");
        /*
        System.out.println("🔥🔥🔥 INITIALISATION DU LISTENER FIREBASE... 🔥🔥🔥");
        try {
            if (firestore == null) {
                System.err.println("❌ ERREUR : Le bean Firestore est NULL !");
                return;
            }
            
            firestore.collection("signalements")
                .addSnapshotListener((snapshots, e) -> {
                    System.out.println("🔔 Événement Firestore reçu !");
                    if (e != null) {
                        System.err.println("ERREUR CRITIQUE Firestore SnapshotListener : " + e.getMessage());
                        e.printStackTrace();
                        return;
                    }

                    if (snapshots != null) {
                        System.out.println("Firebase Listener : " + snapshots.getDocumentChanges().size() + " changements détectés.");
                        for (DocumentChange dc : snapshots.getDocumentChanges()) {
                            if (dc.getType() == DocumentChange.Type.ADDED) {
                                try {
                                    String docId = dc.getDocument().getId();
                                    System.out.println("Nouveau document Firebase détecté : " + docId);
                                    
                                    // Vérifier si le document contient déjà un postgresId (pour éviter les boucles)
                                    if (dc.getDocument().contains("postgresId") && dc.getDocument().get("postgresId") != null) {
                                        System.out.println("Document " + docId + " ignoré (déjà synchronisé avec PostgresId : " + dc.getDocument().get("postgresId") + ")");
                                        continue;
                                    }

                                    SignalementDTO dto = dc.getDocument().toObject(SignalementDTO.class);
                                    dto.setIdFirebase(docId);
                                    signalementService.enregistrerSignalement(dto);
                                } catch (Exception ex) {
                                    System.err.println("Erreur lors du traitement du document Firestore " + dc.getDocument().getId() + " : " + ex.getMessage());
                                    ex.printStackTrace();
                                }
                            }
                        }
                    } else {
                        System.out.println("Firebase Listener : snapshots est null.");
                    }
                });
            System.out.println("✅ LISTENER FIREBASE ATTACHÉ AVEC SUCCÈS !");
        } catch (Exception ex) {
            System.err.println("❌ ERREUR LORS DE L'ATTACHEMENT DU LISTENER FIREBASE : " + ex.getMessage());
            ex.printStackTrace();
        }
        */
    }
}
