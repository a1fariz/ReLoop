package com.reloop.security;

import com.reloop.auth.domain.User;
import com.reloop.auth.service.JwtService;
import com.reloop.common.security.CorrelationIdFilter;
import com.reloop.common.security.JwtAuthenticationFilter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class SecurityAndAuthFilterTest {

    private JwtService jwtService;
    private JwtAuthenticationFilter jwtFilter;
    private CorrelationIdFilter correlationIdFilter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
        jwtService = new JwtService("404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970", 15);
        jwtFilter = new JwtAuthenticationFilter(jwtService);
        correlationIdFilter = new CorrelationIdFilter();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void jwtFilterSetsSecurityContextForValidBearerToken() throws Exception {
        User user = new User("alice@reloop.com", "hash", "Alice", "123", User.Role.CUSTOMER);
        org.springframework.test.util.ReflectionTestUtils.setField(user, "id", 42L);
        String token = jwtService.generateAccessToken(user);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + token);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        jwtFilter.doFilter(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        assertNotNull(auth);
        assertEquals(42L, auth.getPrincipal());
        assertTrue(auth.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_CUSTOMER")));
    }

    @Test
    void correlationIdFilterRejectsInvalidUuid() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", "invalid-uuid-format");
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        correlationIdFilter.doFilter(request, response, filterChain);

        assertEquals(HttpServletResponse.SC_BAD_REQUEST, response.getStatus());
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    void correlationIdFilterAcceptsValidUuid() throws Exception {
        String validUuid = UUID.randomUUID().toString();
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-Correlation-ID", validUuid);
        MockHttpServletResponse response = new MockHttpServletResponse();
        FilterChain filterChain = mock(FilterChain.class);

        correlationIdFilter.doFilter(request, response, filterChain);

        assertEquals(validUuid, response.getHeader("X-Correlation-ID"));
        verify(filterChain).doFilter(request, response);
    }
}
