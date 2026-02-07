package com.cloud.identity.service;

import com.google.cloud.firestore.Firestore;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Service de notifications avec persistance dans Firestore
 * 
 * Ce service envoie des notifications FCM ET les persiste dans Firestore
 * pour que les utilisateurs hors ligne puissent les récupérer à leur prochaine
 * connexion.
 */
@Service
public class NotificationPersistanceService {

    @Autowired
    private Firestore firestore;

    /**
     * Envoie une notification FCM et la persiste dans Firestore
     * 
     * @param userEmail     Email de l'utilisateur destinataire
     * @param fcmToken      Token FCM de l'utilisateur (peut être null si
     *                      l'utilisateur est hors ligne)
     * @param titre         Titre de la notification
     * @param message       Message de la notification
     * @param signalementId ID du signalement concerné (optionnel)
     * @throws Exception Si une erreur survient
     */
    public void envoyerNotificationAvecPersistance(
            String userEmail,
            String fcmToken,
            String titre,
            String message,
            Integer signalementId) throws Exception {

        System.out.println("📨 Envoi notification pour: " + userEmail);

        // 1. Persister d'abord dans Firestore (garantie de livraison)
        String notificationId = persisterNotification(userEmail, titre, message, signalementId);
        System.out.println("✅ Notification persistée avec ID: " + notificationId);

        // 2. Tenter d'envoyer via FCM (peut échouer si l'utilisateur est hors ligne)
        if (fcmToken != null && !fcmToken.isEmpty()) {
            try {
                envoyerNotificationFCM(fcmToken, titre, message, signalementId);
                System.out.println("✅ Notification FCM envoyée");
            } catch (Exception e) {
                // Si FCM échoue, la notification reste dans Firestore
                System.err.println("⚠️ Échec FCM mais notification persistée: " + e.getMessage());
            }
        } else {
            System.out.println("ℹ️ Pas de token FCM, notification uniquement persistée");
        }
    }

    /**
     * Persiste une notification dans Firestore
     * 
     * @return L'ID du document créé
     */
    private String persisterNotification(
            String userEmail,
            String titre,
            String message,
            Integer signalementId) throws Exception {

        Map<String, Object> notification = new HashMap<>();
        notification.put("userEmail", userEmail);
        notification.put("titre", titre);
        notification.put("message", message);
        notification.put("signalementId", signalementId);
        notification.put("lue", false);
        notification.put("dateCreation", Instant.now().toString());
        notification.put("dateLecture", null);

        return firestore.collection("notifications")
                .add(notification)
                .get()
                .getId();
    }

    /**
     * Envoie une notification FCM
     */
    private void envoyerNotificationFCM(
            String fcmToken,
            String titre,
            String message,
            Integer signalementId) throws Exception {

        Notification notification = Notification.builder()
                .setTitle(titre)
                .setBody(message)
                .build();

        Map<String, String> data = new HashMap<>();
        if (signalementId != null) {
            data.put("signalementId", signalementId.toString());
        }

        Message fcmMessage = Message.builder()
                .setToken(fcmToken)
                .setNotification(notification)
                .putAllData(data)
                .build();

        FirebaseMessaging.getInstance().send(fcmMessage);
    }

    /**
     * Méthode helper : Notification pour changement de statut
     */
    public void notifierChangementStatut(
            String userEmail,
            String fcmToken,
            Integer signalementId,
            String nouveauStatut) throws Exception {

        String titre;
        String message;

        switch (nouveauStatut.toUpperCase()) {
            case "APPROUVE":
            case "APPROUVÉ":
                titre = "✅ Signalement Approuvé";
                message = "Votre signalement #" + signalementId + " a été approuvé";
                break;
            case "REJETE":
            case "REJETÉ":
                titre = "❌ Signalement Rejeté";
                message = "Votre signalement #" + signalementId + " a été rejeté";
                break;
            case "RESOLU":
            case "RÉSOLU":
                titre = "🎉 Signalement Résolu";
                message = "Votre signalement #" + signalementId + " a été résolu";
                break;
            case "EN_COURS":
            case "EN COURS":
                titre = "🔄 Signalement En Cours";
                message = "Votre signalement #" + signalementId + " est en cours de traitement";
                break;
            default:
                titre = "📋 Statut Modifié";
                message = "Le statut de votre signalement #" + signalementId + " a changé: " + nouveauStatut;
        }

        envoyerNotificationAvecPersistance(userEmail, fcmToken, titre, message, signalementId);
    }
}
