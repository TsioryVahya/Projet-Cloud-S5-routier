package com.cloud.identity.config;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.firestore.Firestore;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.cloud.FirestoreClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
public class FirestoreConfig {

    @Bean
    public Firestore firestore() {
        System.out.println("🔥 Initialisation Firebase...");

        try {
            if (FirebaseApp.getApps().isEmpty()) {
                GoogleCredentials credentials;
                String credentialsPath = System.getenv("GOOGLE_APPLICATION_CREDENTIALS");
                
                if (credentialsPath != null && new java.io.File(credentialsPath).exists()) {
                    System.out.println("✅ Utilisation des identifiants via GOOGLE_APPLICATION_CREDENTIALS : " + credentialsPath);
                    credentials = GoogleCredentials.fromStream(new java.io.FileInputStream(credentialsPath));
                } else {
                    System.out.println("🔍 Recherche de serviceAccountKey.json dans le classpath...");
                    org.springframework.core.io.ClassPathResource resource = new org.springframework.core.io.ClassPathResource("serviceAccountKey.json");
                    if (resource.exists()) {
                        System.out.println("✅ serviceAccountKey.json trouvé dans le classpath.");
                        credentials = GoogleCredentials.fromStream(resource.getInputStream());
                    } else {
                        System.out.println("⚠️ serviceAccountKey.json non trouvé, tentative via Application Default Credentials...");
                        credentials = GoogleCredentials.getApplicationDefault();
                    }
                }

                FirebaseOptions options = FirebaseOptions.builder()
                        .setCredentials(credentials)
                        .setProjectId("projet-cloud-s5-routier")
                        .build();

                FirebaseApp.initializeApp(options);
                System.out.println("✅ FirebaseApp initialisé avec succès.");
            }

            return FirestoreClient.getFirestore(FirebaseApp.getInstance());

        } catch (Exception e) {
            System.err.println("⚠️ ERREUR lors de l'initialisation Firebase : " + e.getMessage());
            e.printStackTrace();
            // Au lieu de retourner null, on pourrait lancer une exception si Firebase est critique
            // Mais pour l'instant, on laisse null et on s'assure que c'est géré ou on corrige l'erreur
            return null;
        }
    }
}
