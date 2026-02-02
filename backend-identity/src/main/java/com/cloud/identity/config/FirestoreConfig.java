package com.cloud.identity.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirestoreConfig {

    @Bean
    public Firestore firestore() {
        System.out.println("🔥 Initialisation Firebase via Classpath...");

        try {
            // Charger le fichier de credentials
            System.out.println("🔍 Recherche du fichier serviceAccountKey.json dans le classpath...");
            ClassPathResource resource = new ClassPathResource("serviceAccountKey.json");
            
            if (!resource.exists()) {
                System.err.println("⚠️ ATTENTION : Fichier serviceAccountKey.json introuvable !");
                System.err.println("⚠️ La synchronisation Firebase sera désactivée.");
                return null;
            }

            InputStream serviceAccount = resource.getInputStream();
            System.out.println("✅ Fichier serviceAccountKey.json trouvé.");

            // Créer les credentials
            GoogleCredentials credentials = GoogleCredentials.fromStream(serviceAccount);

            // Initialiser Firebase Admin SDK
            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("✅ FirebaseApp initialisé avec succès.");
            }

            return FirestoreClient.getFirestore();

        } catch (Exception e) {
            System.err.println("⚠️ ERREUR lors de l'initialisation Firebase (Désactivée) : " + e.getMessage());
            return null;
        }
    }
}
