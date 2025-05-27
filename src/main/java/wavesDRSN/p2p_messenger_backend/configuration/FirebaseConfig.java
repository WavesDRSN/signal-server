package wavesDRSN.p2p_messenger_backend.configuration;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;

@Configuration
public class FirebaseConfig {

    private static final Logger log = LoggerFactory.getLogger(FirebaseConfig.class);

    @Value("${app.firebase-configuration-file:classpath:firebase-service-account.json}")
    private String serviceAccountPath;

    @PostConstruct
    public void initialize() throws IOException {
        InputStream serviceAccountStream = null;
        try {
            Resource resource = new ClassPathResource(serviceAccountPath);

            if (!resource.exists()) {
                log.warn("Firebase service account file not found in classpath: {}. Attempting to load from filesystem.", serviceAccountPath);
                resource = new org.springframework.core.io.FileSystemResource(serviceAccountPath);
            }

            if (!resource.exists()) {
                String errorMessage = String.format(
                    "Firebase service account file not found at path: %s. " +
                    "Ensure the file is present (checked classpath and filesystem) or configure via 'app.firebase.service-account-path' property.",
                    serviceAccountPath
                );
                log.error(errorMessage);
                throw new IOException(errorMessage);
            }

            log.info("Loading Firebase service account from: {}", resource.getDescription());
            serviceAccountStream = resource.getInputStream();

            FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(GoogleCredentials.fromStream(serviceAccountStream))
                .build();

            if (FirebaseApp.getApps().isEmpty()) {
                FirebaseApp.initializeApp(options);
                log.info("Firebase Admin SDK initialized successfully using service account: {}", resource.getDescription());
            } else {
                log.info("Firebase Admin SDK already initialized.");
            }
        } catch (IOException e) {
            log.error("Failed to initialize Firebase Admin SDK using service account path '{}': {}", serviceAccountPath, e.getMessage(), e);
            throw e;
        } finally {
            if (serviceAccountStream != null) {
                try {
                    serviceAccountStream.close();
                } catch (IOException e) {
                    log.warn("Failed to close service account stream for {}", serviceAccountPath, e);
                }
            }
        }
    }
}
