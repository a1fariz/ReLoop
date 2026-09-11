package com.reloop.common.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;

/**
 * Replaces Spring's @AuthenticationPrincipal Long userId injection used by
 * the legacy controllers.
 */
@RequestScoped
public class CurrentUser {

    private final SecurityIdentity identity;

    @Inject
    public CurrentUser(SecurityIdentity identity) {
        this.identity = identity;
    }

    public Long id() {
        return Long.valueOf(identity.getPrincipal().getName());
    }

    public String role() {
        return identity.getRoles().stream().findFirst().orElse(null);
    }

    /** Email claim from the access token, or null for legacy tokens without it. */
    public String email() {
        Object email = identity.getAttribute("email");
        return email != null ? email.toString() : null;
    }
}
