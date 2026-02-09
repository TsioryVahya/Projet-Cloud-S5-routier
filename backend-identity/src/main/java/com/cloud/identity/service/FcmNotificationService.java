package com.cloud.identity.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentReference;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@Service
public class FcmNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(FcmNotificationService.class);

    /**
     * Envoie une notification FCM à un utilisateur
     * 
     * @param fcmToken Token FCM de l'utilisateur
     * @param titre    Titre de la notification
     * @param corps    Corps de la notification
     * @param data     Données supplémentaires
     */
    public void sendNotification(String fcmToken, String titre, String corps, Map<String, String> data) {
        try {
            if (fcmToken == null || fcmToken.isEmpty()) {
                logger.warn("⚠️ Token FCM vide, impossible d'envoyer la notification");
                return;
            }

            // Construire la notification
            Notification notification = Notification.builder()
                    .setTitle(titre)
                    .setBody(corps)
                    .build();

            // Construire le message
            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification);

            // Ajouter les données si présentes
            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            Message message = messageBuilder.build();

            // Envoyer le message
            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("✅ Notification FCM envoyée avec succès: {}", response);

        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'envoi de la notification FCM", e);
        }
    }

    /**
     * Envoie une notification de changement de statut
     * 
     * @param userId        ID de l'utilisateur Firebase
     * @param signalementId ID du signalement
     * @param oldStatus     Ancien statut
     * @param newStatus     Nouveau statut
     */
    public void sendStatusChangeNotification(String userId, String signalementId,
            String oldStatus, String newStatus) {
        try {
            logger.info("📬 Préparation notification pour userId={}, signalement={}, {} -> {}",
                    userId, signalementId, oldStatus, newStatus);

            // Récupérer le FCM token de l'utilisateur depuis Firestore
            // On essaie d'abord la collection "users" (mobile) puis "utilisateurs" (web/sync)
            Firestore db = FirestoreClient.getFirestore();
            
            String fcmToken = null;
            
            // 1. Tenter dans la collection "users" avec le userId fourni (qui peut être l'UUID ou l'UID Firebase)
            DocumentReference userDoc = db.collection("users").document(userId);
            try {
                Map<String, Object> userData = userDoc.get().get().getData();
                
                // 2. Si non trouvé dans "users", on essaie "utilisateurs"
                if (userData == null) {
                    logger.info("ℹ️ Utilisateur non trouvé dans 'users', tentative dans 'utilisateurs' pour userId={}", userId);
                    userDoc = db.collection("utilisateurs").document(userId);
                    userData = userDoc.get().get().getData();
                }

                if (userData != null && userData.containsKey("fcmToken")) {
                    fcmToken = (String) userData.get("fcmToken");
                }
            } catch (InterruptedException | ExecutionException e) {
                logger.error("❌ Erreur lors de la récupération du FCM token", e);
                return;
            }

            // 3. Si toujours pas de token, et que l'ID ressemble à un UUID, on peut tenter une recherche par email
            // ou par d'autres identifiants si nécessaire. Mais ici le userId est censé être l'ID du document.

            // 1. Toujours créer l'enregistrement dans la collection notifications Firestore
            // Cela permet à l'utilisateur de voir la notification dans l'app mobile même si
            // le push FCM échoue
            createNotificationRecord(userId, signalementId, oldStatus, newStatus);

            // 2. Tenter d'envoyer la notification push FCM
            if (fcmToken == null || fcmToken.isEmpty()) {
                logger.warn(
                        "⚠️ Aucun FCM token trouvé pour l'utilisateur {} (cherché dans 'users' et 'utilisateurs'). " +
                        "La notification push ne sera pas envoyée, mais elle est enregistrée dans l'historique Firestore.",
                        userId);
                return;
            }

            // Préparer le titre et le message
            String titre = "Changement de statut";
            String corps = String.format("Votre signalement est maintenant \"%s\"", newStatus);

            // Préparer les données supplémentaires
            Map<String, String> data = new HashMap<>();
            data.put("type", "status_change");
            data.put("signalementId", signalementId);
            data.put("oldStatus", oldStatus != null ? oldStatus : "");
            data.put("newStatus", newStatus);

            // Envoyer la notification FCM
            sendNotification(fcmToken, titre, corps, data);
        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'envoi de la notification de changement de statut", e);
        }
    }

    /**
     * Crée un enregistrement de notification dans Firestore
     */
    private void createNotificationRecord(String userId, String signalementId,
            String oldStatus, String newStatus) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("userId", userId);
            notificationData.put("signalementId", signalementId);
            notificationData.put("titre", "Changement de statut");
            notificationData.put("message", String.format("Votre signalement est maintenant \"%s\"", newStatus));
            notificationData.put("type", "status_change");
            notificationData.put("oldStatus", oldStatus != null ? oldStatus : "");
            notificationData.put("newStatus", newStatus);
            notificationData.put("dateCreation", com.google.cloud.Timestamp.now());
            notificationData.put("lu", false);

            // Ajouter à la collection notifications
            db.collection("notifications").add(notificationData).get();

            logger.info("✅ Notification enregistrée dans Firestore pour userId={}", userId);

        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'enregistrement de la notification dans Firestore", e);
        }
    }

    /**
     * Envoie une notification à tous les utilisateurs ayant signalé un problème
     * 
     * @param signalementId ID du signalement
     * @param nouveauStatut Nouveau statut
     */
    public void notifyStatusChange(String signalementId, String oldStatus, String newStatus, String userId) {
        logger.info("🔔 Notification de changement de statut: {} -> {} pour signalement {} (user: {})",
                oldStatus, newStatus, signalementId, userId);

        if (userId != null && !userId.isEmpty()) {
            sendStatusChangeNotification(userId, signalementId, oldStatus, newStatus);
        } else {
            logger.warn("⚠️ UserId manquant, impossible d'envoyer la notification");
        }
    }
}
