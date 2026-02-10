package com.cloud.identity.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.DocumentSnapshot;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

@Service
public class FcmNotificationService {

    private static final Logger logger = LoggerFactory.getLogger(FcmNotificationService.class);

    /**
     * Envoie une notification FCM à un utilisateur
     */
    public void sendNotification(String fcmToken, String titre, String corps, Map<String, String> data) {
        try {
            if (fcmToken == null || fcmToken.isEmpty()) {
                logger.warn("⚠️ Token FCM vide, impossible d'envoyer la notification");
                return;
            }

            Notification notification = Notification.builder()
                    .setTitle(titre)
                    .setBody(corps)
                    .build();

            Message.Builder messageBuilder = Message.builder()
                    .setToken(fcmToken)
                    .setNotification(notification);

            if (data != null && !data.isEmpty()) {
                messageBuilder.putAllData(data);
            }

            Message message = messageBuilder.build();
            String response = FirebaseMessaging.getInstance().send(message);
            logger.info("✅ Notification FCM envoyée avec succès: {}", response);

        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'envoi de la notification FCM", e);
        }
    }

    /**
     * Envoie une notification de changement de statut
     */
    public void sendStatusChangeNotification(String userId, String userEmail, String signalementId,
            String typeSignalement, String oldStatus, String newStatus) {
        try {
            logger.info("📬 Préparation notification pour userId={}, email={}, signalement={}, type={}, {} -> {}",
                    userId, userEmail, signalementId, typeSignalement, oldStatus, newStatus);

            Firestore db = FirestoreClient.getFirestore();
            String finalUserId = userId;
            String fcmToken = null;

            // 1. Tenter de récupérer par UID
            if (finalUserId != null && !finalUserId.isEmpty()) {
                fcmToken = findFcmTokenByUid(db, finalUserId);
            }

            // 2. Si non trouvé par UID, tenter par Email
            if ((fcmToken == null || fcmToken.isEmpty()) && userEmail != null && !userEmail.isEmpty()) {
                logger.info("🔍 Recherche du FCM token via email: {}", userEmail);

                // On cherche uniquement dans la collection 'utilisateurs'
                String[] collections = { "utilisateurs" };
                for (String coll : collections) {
                    try {
                        // On ne met pas de limite pour trouver celui qui a un token
                        QuerySnapshot query = db.collection(coll).whereEqualTo("email", userEmail).get().get();
                        for (DocumentSnapshot doc : query.getDocuments()) {
                            if (doc.contains("fcmToken")) {
                                String token = doc.getString("fcmToken");
                                if (token != null && !token.isEmpty()) {
                                    fcmToken = token;
                                    finalUserId = doc.getId(); // On utilise l'ID du document qui a le token!
                                    logger.info("✅ FCM token trouvé via Email [{}] dans la collection '{}' (UID: {})",
                                            userEmail, coll, finalUserId);
                                    break;
                                }
                            }
                        }
                        if (fcmToken != null)
                            break;
                    } catch (Exception e) {
                        logger.warn("⚠️ Recherche Email dans '{}' échouée: {}", coll, e.getMessage());
                    }
                }
            }

            // 3. Toujours créer l'enregistrement dans la collection notifications Firestore
            createNotificationRecord(finalUserId, signalementId, typeSignalement, oldStatus, newStatus);

            // 4. Tenter d'envoyer la notification push FCM
            if (fcmToken == null || fcmToken.isEmpty()) {
                logger.warn("⚠️ Aucun FCM token trouvé pour l'utilisateur {}. Push non envoyé.", finalUserId);
                return;
            }

            String titre = "Mise à jour de votre signalement";
            String corps = String.format("Votre signalement \"%s\" est passé de %s à \"%s\".",
                    typeSignalement != null ? typeSignalement : "Signalement",
                    oldStatus != null ? oldStatus : "en attente",
                    newStatus);

            Map<String, String> data = new HashMap<>();
            data.put("type", "status_change");
            data.put("signalementId", signalementId);
            data.put("typeSignalement", typeSignalement != null ? typeSignalement : "");
            data.put("oldStatus", oldStatus != null ? oldStatus : "");
            data.put("newStatus", newStatus);

            sendNotification(fcmToken, titre, corps, data);
        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'envoi de la notification de changement de statut", e);
        }
    }

    /**
     * Crée un enregistrement de notification dans Firestore
     */
    private void createNotificationRecord(String userId, String signalementId, String typeSignalement,
            String oldStatus, String newStatus) {
        try {
            Firestore db = FirestoreClient.getFirestore();

            Map<String, Object> notificationData = new HashMap<>();
            notificationData.put("userId", userId);
            notificationData.put("signalementId", signalementId);
            notificationData.put("titre", "Mise à jour de signalement");
            notificationData.put("message", String.format("Votre signalement \"%s\" est maintenant \"%s\"",
                    typeSignalement != null ? typeSignalement : "Signalement", newStatus));
            notificationData.put("typeSignalement", typeSignalement != null ? typeSignalement : "");
            notificationData.put("type", "status_change");
            notificationData.put("oldStatus", oldStatus != null ? oldStatus : "");
            notificationData.put("newStatus", newStatus);
            notificationData.put("dateCreation", com.google.cloud.Timestamp.now());
            notificationData.put("lu", false);

            db.collection("notifications").add(notificationData).get();
            logger.info("✅ Notification enregistrée dans Firestore pour userId={}", userId);

        } catch (Exception e) {
            logger.error("❌ Erreur lors de l'enregistrement de la notification dans Firestore", e);
        }
    }

    /**
     * Envoie une notification à tous les utilisateurs ayant signalé un problème
     */
    public void notifyStatusChange(String signalementId, String typeSignalement, String oldStatus, String newStatus,
            String userId, String userEmail) {
        logger.info(
                "🔔 Notification de changement de statut: {} -> {} pour signalement {} (type: {}, user: {}, email: {})",
                oldStatus, newStatus, signalementId, typeSignalement, userId, userEmail);

        if ((userId != null && !userId.isEmpty()) || (userEmail != null && !userEmail.isEmpty())) {
            sendStatusChangeNotification(userId, userEmail, signalementId, typeSignalement, oldStatus, newStatus);
        } else {
            logger.warn("⚠️ UserId et Email manquants, impossible d'envoyer la notification");
        }
    }

    private String findFcmTokenByUid(Firestore db, String uid) {
        String[] collections = { "utilisateurs" };
        for (String coll : collections) {
            try {
                // 1. Tenter par ID de document (Firebase UID ou Postgres ID selon le cas)
                DocumentSnapshot doc = db.collection(coll).document(uid).get().get();
                if (doc.exists() && doc.contains("fcmToken")) {
                    String token = doc.getString("fcmToken");
                    if (token != null && !token.isEmpty()) {
                        logger.info("✅ FCM token trouvé via ID document [{}] dans la collection '{}'", uid, coll);
                        return token;
                    }
                }

                // 2. Si non trouvé par ID doc, chercher dans les champs firebaseUid ou
                // postgresId à l'intérieur du document
                // Cela aide si l'ID passé est le postgresId mais que le doc est indexé par
                // firebaseUid (ou vice-versa)
                QuerySnapshot queryPostgres = db.collection(coll).whereEqualTo("postgresId", uid).get().get();
                for (DocumentSnapshot d : queryPostgres.getDocuments()) {
                    if (d.contains("fcmToken")) {
                        String token = d.getString("fcmToken");
                        if (token != null && !token.isEmpty()) {
                            logger.info("✅ FCM token trouvé via champ postgresId [{}] dans la collection '{}'", uid,
                                    coll);
                            return token;
                        }
                    }
                }

                QuerySnapshot queryFirebase = db.collection(coll).whereEqualTo("firebaseUid", uid).get().get();
                for (DocumentSnapshot d : queryFirebase.getDocuments()) {
                    if (d.contains("fcmToken")) {
                        String token = d.getString("fcmToken");
                        if (token != null && !token.isEmpty()) {
                            logger.info("✅ FCM token trouvé via champ firebaseUid [{}] dans la collection '{}'", uid,
                                    coll);
                            return token;
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("⚠️ Recherche UID [{}] dans '{}' échouée: {}", uid, coll, e.getMessage());
            }
        }
        return null;
    }

    private String findFcmTokenByEmail(Firestore db, String email) {
        String[] collections = { "utilisateurs" };
        for (String coll : collections) {
            try {
                // Recherche de tous les documents avec cet email pour trouver celui qui a un
                // token
                QuerySnapshot query = db.collection(coll).whereEqualTo("email", email).get().get();
                for (DocumentSnapshot doc : query.getDocuments()) {
                    if (doc.contains("fcmToken")) {
                        String token = doc.getString("fcmToken");
                        if (token != null && !token.isEmpty()) {
                            logger.info("✅ FCM token trouvé via Email [{}] dans la collection '{}'", email, coll);
                            return token;
                        }
                    }
                }
            } catch (Exception e) {
                logger.warn("⚠️ Recherche Email dans '{}' échouée: {}", coll, e.getMessage());
            }
        }
        return null;
    }

    private String findUidByEmail(Firestore db, String email) {
        String[] collections = { "utilisateurs" };
        for (String coll : collections) {
            try {
                // Recherche de tous les documents avec cet email pour trouver celui qui a un
                // token (le plus probable d'être l'actif)
                QuerySnapshot query = db.collection(coll).whereEqualTo("email", email).get().get();
                String firstUid = null;
                for (DocumentSnapshot doc : query.getDocuments()) {
                    if (firstUid == null)
                        firstUid = doc.getId();

                    if (doc.contains("fcmToken")) {
                        String token = doc.getString("fcmToken");
                        if (token != null && !token.isEmpty()) {
                            logger.info("✅ UID trouvé via Email [{}] (avec token) dans la collection '{}': {}", email,
                                    coll, doc.getId());
                            return doc.getId();
                        }
                    }
                }

                if (firstUid != null) {
                    logger.info("✅ UID trouvé via Email [{}] (sans token) dans la collection '{}': {}", email, coll,
                            firstUid);
                    return firstUid;
                }
            } catch (Exception e) {
                logger.warn("⚠️ Recherche UID/Email dans '{}' échouée: {}", coll, e.getMessage());
            }
        }
        return null;
    }
}