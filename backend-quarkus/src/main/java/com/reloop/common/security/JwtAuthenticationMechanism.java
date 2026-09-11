package com.reloop.common.security;

import com.reloop.auth.service.JwtService;
import io.jsonwebtoken.Claims;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.security.identity.request.AuthenticationRequest;
import io.quarkus.security.runtime.QuarkusSecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.quarkus.vertx.http.runtime.security.HttpAuthenticationMechanism;
import io.smallrye.mutiny.Uni;
import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Set;

/**
 * Bearer-token authentication replacing the legacy Spring JwtAuthenticationFilter +
 * SecurityFilterChain. Tokens are HS256 JWTs with sub=user id and a "role" claim,
 * byte-for-byte compatible with the tokens issued by the legacy backend.
 */
@ApplicationScoped
public class JwtAuthenticationMechanism implements HttpAuthenticationMechanism {

    private final JwtService jwtService;

    @Inject
    public JwtAuthenticationMechanism(JwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    public Uni<SecurityIdentity> authenticate(RoutingContext context, IdentityProviderManager identityProviderManager) {
        String authorization = context.request().getHeader("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return Uni.createFrom().nullItem();
        }

        try {
            Claims claims = jwtService.parseAndValidate(authorization.substring(7));
            Long userId = Long.parseLong(claims.getSubject());
            String role = claims.get("role", String.class);

            QuarkusSecurityIdentity.Builder builder = QuarkusSecurityIdentity.builder()
                    .setPrincipal(new QuarkusUserIdPrincipal(userId))
                    .addCredential(new BearerTokenCredential(authorization.substring(7)));
            if (role != null) {
                builder.addRole(role);
            }
            String email = claims.get("email", String.class);
            if (email != null) {
                builder.addAttribute("email", email);
            }
            return Uni.createFrom().item(builder.build());
        } catch (Exception e) {
            // Degrade to anonymous instead of failing: public paths stay accessible
            // with a stale/garbage token (legacy JwtAuthenticationFilter behaviour),
            // protected paths still get 401 from the 'authenticated' path policy.
            return Uni.createFrom().nullItem();
        }
    }

    @Override
    public Uni<ChallengeData> getChallenge(RoutingContext context) {
        return Uni.createFrom().item(new ChallengeData(401, "WWW-Authenticate", "Bearer"));
    }

    @Override
    public Set<Class<? extends AuthenticationRequest>> getCredentialTypes() {
        return Set.of();
    }

    public static class BearerTokenCredential extends io.quarkus.security.credential.TokenCredential {
        public BearerTokenCredential(String token) {
            super(token, "Bearer");
        }
    }

    public static class QuarkusUserIdPrincipal implements java.security.Principal {
        private final Long userId;

        public QuarkusUserIdPrincipal(Long userId) {
            this.userId = userId;
        }

        @Override
        public String getName() {
            return userId.toString();
        }

        public Long getUserId() {
            return userId;
        }
    }
}
