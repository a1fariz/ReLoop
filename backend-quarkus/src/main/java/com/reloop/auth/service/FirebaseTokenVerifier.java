package com.reloop.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.reloop.common.exception.BusinessException;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

@ApplicationScoped
public class FirebaseTokenVerifier {
    private final String apiKey;
    private final String projectId;
    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;

    public FirebaseTokenVerifier(
            @ConfigProperty(name = "reloop.firebase.api-key", defaultValue = "AIzaSyBgu6-9sThvxVTmJnbtcr3Z9DCE7o0PUNI") String apiKey,
            @ConfigProperty(name = "reloop.firebase.project-id", defaultValue = "reloop-ffa54") String projectId,
            ObjectMapper objectMapper) {
        this.apiKey = apiKey;
        this.projectId = projectId;
        this.objectMapper = objectMapper;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .build();
    }

    public record FirebaseUser(String uid, String email, String name, boolean emailVerified) {}

    public FirebaseUser verifyToken(String idToken) {
        if (idToken == null || idToken.isBlank()) {
            throw new BusinessException("Missing Firebase ID token", "INVALID_FIREBASE_TOKEN", 401);
        }

        try {
            String endpoint = "https://identitytoolkit.googleapis.com/v1/accounts:lookup?key=" + apiKey;
            String requestBody = objectMapper.writeValueAsString(Map.of("idToken", idToken));

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(endpoint))
                    .header("Content-Type", "application/json")
                    .timeout(Duration.ofSeconds(8))
                    .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new BusinessException("Firebase authentication failed", "INVALID_FIREBASE_TOKEN", 401);
            }

            JsonNode root = objectMapper.readTree(response.body());
            JsonNode usersNode = root.path("users");
            if (!usersNode.isArray() || usersNode.isEmpty()) {
                throw new BusinessException("User not found in Firebase", "FIREBASE_USER_NOT_FOUND", 401);
            }

            JsonNode userNode = usersNode.get(0);
            String uid = userNode.path("localId").asText();
            String email = userNode.path("email").asText();
            boolean emailVerified = userNode.path("emailVerified").asBoolean(false);
            String displayName = userNode.path("displayName").asText(null);

            if (email == null || email.isBlank()) {
                throw new BusinessException("Firebase user has no email", "FIREBASE_EMAIL_REQUIRED", 400);
            }

            return new FirebaseUser(uid, email, displayName, emailVerified);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            throw new BusinessException("Error verifying Firebase token: " + e.getMessage(), "FIREBASE_VERIFICATION_ERROR", 500);
        }
    }
}
