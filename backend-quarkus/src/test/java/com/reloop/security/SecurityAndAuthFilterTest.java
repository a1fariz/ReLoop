package com.reloop.security;

import com.reloop.auth.domain.User;
import com.reloop.auth.service.JwtService;
import com.reloop.common.security.JwtAuthenticationMechanism;
import com.reloop.support.TestFields;
import io.quarkus.security.identity.IdentityProviderManager;
import io.quarkus.security.identity.SecurityIdentity;
import io.quarkus.vertx.http.runtime.security.ChallengeData;
import io.smallrye.mutiny.Uni;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.ext.web.RoutingContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SecurityAndAuthFilterTest {

    private JwtService jwtService;
    private JwtAuthenticationMechanism mechanism;

    @Mock
    private IdentityProviderManager identityProviderManager;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 15);
        mechanism = new JwtAuthenticationMechanism(jwtService);
    }

    private RoutingContext routingContextWithHeader(String headerValue) {
        RoutingContext context = mock(RoutingContext.class);
        HttpServerRequest request = mock(HttpServerRequest.class);
        when(context.request()).thenReturn(request);
        when(request.getHeader("Authorization")).thenReturn(headerValue);
        return context;
    }

    @Test
    @DisplayName("Valid bearer token yields a SecurityIdentity with userId principal and role")
    void validTokenProducesIdentity() {
        User user = new User("alice@reloop.com", "hash", "Alice", "123", User.Role.CUSTOMER);
        TestFields.set(user, "id", 42L);
        String token = jwtService.generateAccessToken(user);

        SecurityIdentity identity = mechanism
                .authenticate(routingContextWithHeader("Bearer " + token), identityProviderManager)
                .await().indefinitely();

        assertThat(identity).isNotNull();
        assertThat(identity.getPrincipal().getName()).isEqualTo("42");
        assertThat(identity.getRoles()).containsExactly("CUSTOMER");
    }

    @Test
    @DisplayName("Missing Authorization header yields no identity (anonymous)")
    void missingHeaderYieldsAnonymous() {
        Uni<SecurityIdentity> result = mechanism.authenticate(routingContextWithHeader(null), identityProviderManager);
        assertThat(result.await().indefinitely()).isNull();
    }

    @Test
    @DisplayName("Tampered token degrades to anonymous identity (public paths stay accessible)")
    void tamperedTokenFails() {
        User user = new User("bob@reloop.com", "hash", "Bob", null, User.Role.SELLER);
        TestFields.set(user, "id", 7L);
        String token = jwtService.generateAccessToken(user);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        SecurityIdentity identity = mechanism
                .authenticate(routingContextWithHeader("Bearer " + tampered), identityProviderManager)
                .await().indefinitely();

        // null identity = anonymous; protected paths still 401 via the 'authenticated' path policy
        assertThat(identity).isNull();
    }

    @Test
    @DisplayName("Invalid signature yields anonymous identity, not an error")
    void invalidSignatureYieldsAnonymous() {
        User user = new User("carol@reloop.com", "hash", "Carol", null, User.Role.ADMIN);
        TestFields.set(user, "id", 9L);
        String token = jwtService.generateAccessToken(user);

        assertThat(jwtService.isTokenValid(token)).isTrue();

        // Signed with a different secret -> parse failure -> anonymous, no exception
        JwtService otherKeyService = new JwtService("FFFFFFFF404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 15);
        assertThat(otherKeyService.isTokenValid(token)).isFalse();

        SecurityIdentity identity = mechanism
                .authenticate(routingContextWithHeader("Bearer " + token + "x"), identityProviderManager)
                .await().indefinitely();
        assertThat(identity).isNull();
    }

    @Test
    @DisplayName("Challenge advertises Bearer scheme with 401")
    void challengeIsBearer401() {
        ChallengeData challenge = mechanism.getChallenge(mock(RoutingContext.class)).await().indefinitely();
        assertThat(challenge.status).isEqualTo(401);
        assertThat(challenge.headerName).isEqualTo("WWW-Authenticate");
    }
}
