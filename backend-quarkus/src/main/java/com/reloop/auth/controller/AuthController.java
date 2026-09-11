package com.reloop.auth.controller;

import com.reloop.auth.dto.AuthResponse;
import com.reloop.auth.dto.LoginRequest;
import com.reloop.auth.dto.RefreshTokenRequest;
import com.reloop.auth.dto.RegisterRequest;
import com.reloop.auth.service.AuthService;
import com.reloop.common.dto.ApiResponse;
import com.reloop.common.security.CorrelationContext;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import io.vertx.core.http.HttpServerRequest;
import org.jboss.resteasy.reactive.RestResponse;

@Path("/api/v1/auth")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AuthController {
    private final AuthService authService;
    private final CorrelationContext correlationContext;

    @Inject
    public AuthController(AuthService authService, CorrelationContext correlationContext) {
        this.authService = authService;
        this.correlationContext = correlationContext;
    }

    @POST
    @Path("/register")
    public RestResponse<ApiResponse<AuthResponse>> register(@Valid RegisterRequest request, HttpServerRequest httpRequest) {
        AuthResponse response = authService.register(request, clientIp(httpRequest));
        return RestResponse.status(RestResponse.Status.CREATED,
                ApiResponse.ok(response, "User registered successfully", correlationContext.getCorrelationId()));
    }

    @POST
    @Path("/login")
    public RestResponse<ApiResponse<AuthResponse>> login(@Valid LoginRequest request, HttpServerRequest httpRequest) {
        AuthResponse response = authService.login(request, clientIp(httpRequest));
        return RestResponse.ok(ApiResponse.ok(response, "Login successful", correlationContext.getCorrelationId()));
    }

    /** Prefers the gateway-provided X-Real-IP so throttling keys on the real client. */
    private static String clientIp(HttpServerRequest request) {
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        var remote = request.remoteAddress();
        return remote != null ? remote.hostAddress() : null;
    }

    @POST
    @Path("/refresh")
    public RestResponse<ApiResponse<AuthResponse>> refreshToken(@Valid RefreshTokenRequest request, HttpServerRequest httpRequest) {
        AuthResponse response = authService.refreshToken(request.refreshToken(), clientIp(httpRequest));
        return RestResponse.ok(ApiResponse.ok(response, "Token refreshed", correlationContext.getCorrelationId()));
    }
}
