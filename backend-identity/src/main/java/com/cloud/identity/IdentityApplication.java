package com.cloud.identity;

import com.cloud.identity.service.FirestoreSyncService;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class IdentityApplication extends SpringBootServletInitializer {

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(IdentityApplication.class);
    }

    public static void main(String[] args) {
        SpringApplication.run(IdentityApplication.class, args);
    }

    @Bean
    public CommandLineRunner initialSync(FirestoreSyncService syncService) {
        return args -> {
            System.out.println("🚀 Synchronisation initiale des configurations vers Firestore...");
            syncService.syncConfigurationsToFirestore();
        };
    }
}
