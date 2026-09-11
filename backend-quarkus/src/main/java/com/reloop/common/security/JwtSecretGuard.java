package com.reloop.common.security;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;

/**
 * Fails startup in prod when the signing key is still the well-known dev
 * default from application.properties — with that secret anyone holding the
 * repository could mint ADMIN tokens.
 */
@ApplicationScoped
public class JwtSecretGuard {

    private static final Logger log = Logger.getLogger(JwtSecretGuard.class);

    // SHA-256 of the dev default secret in application.properties
    private static final String DEV_DEFAULT_SECRET_HASH =
            "3354cfcf65c7b2e6840ea6f880aede239750cb1d063950c4e8fd9fbf9ec73a32";

    @ConfigProperty(name = "jwt.secret")
    String secret;

    @ConfigProperty(name = "quarkus.profile", defaultValue = "dev")
    String profile;

    void onStart(@Observes StartupEvent ev) {
        if ("prod".equals(profile) && sha256(secret).equals(DEV_DEFAULT_SECRET_HASH)) {
            throw new IllegalStateException(
                    "JWT_SECRET is the published dev default; refusing to start in prod. Set a unique 32+ byte secret.");
        }
        if ("prod".equals(profile)) {
            log.info("JWT secret guard passed.");
        }
    }

    private static String sha256(String input) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(input.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
