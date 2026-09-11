package com.reloop.auth.controller;

import com.reloop.auth.repository.UserRepository;
import com.reloop.auth.service.LoginRateLimiter;
import com.reloop.common.dto.ApiResponse;
import com.reloop.common.exception.BusinessException;
import com.reloop.common.security.CorrelationContext;
import jakarta.annotation.security.RolesAllowed;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.resteasy.reactive.RestResponse;

/**
 * Admin account administration: unlocks an account that was auto-locked by
 * repeated failed logins. Deliberately outside /api/v1/auth/* (which is public
 * by path policy) so it always requires authentication + the ADMIN role.
 */
@Path("/api/v1/admin/users")
@Produces(MediaType.APPLICATION_JSON)
public class AdminUserController {
    private final UserRepository userRepository;
    private final LoginRateLimiter rateLimiter;
    private final CorrelationContext correlationContext;
    private final int lockoutWindowSeconds;

    @Inject
    public AdminUserController(
            UserRepository userRepository,
            LoginRateLimiter rateLimiter,
            CorrelationContext correlationContext,
            @ConfigProperty(name = "reloop.auth.lockout.window-seconds", defaultValue = "900") int lockoutWindowSeconds
    ) {
        this.userRepository = userRepository;
        this.rateLimiter = rateLimiter;
        this.correlationContext = correlationContext;
        this.lockoutWindowSeconds = lockoutWindowSeconds;
    }

    @POST
    @Path("/{id}/unlock")
    @RolesAllowed("ADMIN")
    @Transactional
    public RestResponse<ApiResponse<String>> unlock(@PathParam("id") Long userId) {
        var user = userRepository.findByIdOptional(userId)
                .orElseThrow(() -> new BusinessException("User not found", "USER_NOT_FOUND", 404));
        user.setLocked(false);
        userRepository.save(user);
        rateLimiter.resetFailures("lockout:fail:" + user.getEmail());
        return RestResponse.ok(ApiResponse.ok("unlocked", "Account unlocked", correlationContext.getCorrelationId()));
    }
}
