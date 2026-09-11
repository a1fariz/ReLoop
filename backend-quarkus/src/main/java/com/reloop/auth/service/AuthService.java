package com.reloop.auth.service;

import com.reloop.auth.domain.RefreshToken;
import com.reloop.auth.domain.User;
import com.reloop.auth.dto.AuthResponse;
import com.reloop.auth.dto.LoginRequest;
import com.reloop.auth.dto.RegisterRequest;
import com.reloop.auth.repository.RefreshTokenRepository;
import com.reloop.auth.repository.UserRepository;
import com.reloop.common.exception.BusinessException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.persistence.PersistenceException;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class AuthService {
    /**
     * Constant cost-12 BCrypt hash matched against when the email is unknown, so
     * both branches of the login check burn the same CPU — removes the timing
     * oracle used for account enumeration.
     */
    private static final String DUMMY_PASSWORD_HASH =
            new BCryptPasswordEncoder(12).encode("reloop-timing-equalizer");

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final LoginRateLimiter loginRateLimiter;
    private final long refreshTokenExpirationDays;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            JwtService jwtService,
            LoginRateLimiter loginRateLimiter,
            @ConfigProperty(name = "jwt.refresh-token-expiration-days", defaultValue = "7") long refreshTokenExpirationDays,
            @ConfigProperty(name = "reloop.rate-limit.login.max-attempts", defaultValue = "10") int loginMaxAttempts,
            @ConfigProperty(name = "reloop.rate-limit.login.window-seconds", defaultValue = "60") int loginWindowSeconds,
            @ConfigProperty(name = "reloop.rate-limit.register.max-attempts", defaultValue = "5") int registerMaxAttempts,
            @ConfigProperty(name = "reloop.rate-limit.register.window-seconds", defaultValue = "3600") int registerWindowSeconds,
            @ConfigProperty(name = "reloop.rate-limit.refresh.max-attempts", defaultValue = "30") int refreshMaxAttempts,
            @ConfigProperty(name = "reloop.rate-limit.refresh.window-seconds", defaultValue = "60") int refreshWindowSeconds,
            @ConfigProperty(name = "reloop.auth.lockout.max-failures", defaultValue = "5") int lockoutMaxFailures,
            @ConfigProperty(name = "reloop.auth.lockout.window-seconds", defaultValue = "900") int lockoutWindowSeconds
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        // Same BCrypt work factor (12) as the legacy SecurityConfig, so seeded and
        // existing password hashes verify identically.
        this.passwordEncoder = new BCryptPasswordEncoder(12);
        this.jwtService = jwtService;
        this.loginRateLimiter = loginRateLimiter;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
        this.loginMaxAttempts = loginMaxAttempts;
        this.loginWindowSeconds = loginWindowSeconds;
        this.registerMaxAttempts = registerMaxAttempts;
        this.registerWindowSeconds = registerWindowSeconds;
        this.refreshMaxAttempts = refreshMaxAttempts;
        this.refreshWindowSeconds = refreshWindowSeconds;
        this.lockoutMaxFailures = lockoutMaxFailures;
        this.lockoutWindowSeconds = lockoutWindowSeconds;
    }

    private final int loginMaxAttempts;
    private final int loginWindowSeconds;
    private final int registerMaxAttempts;
    private final int registerWindowSeconds;
    private final int refreshMaxAttempts;
    private final int refreshWindowSeconds;
    private final int lockoutMaxFailures;
    private final int lockoutWindowSeconds;

    @Transactional
    public AuthResponse register(RegisterRequest request, String clientIp) {
        // IP-keyed only: email-keying here would let an attacker pre-lock the
        // registration of a victim's address (registration DoS).
        if (!loginRateLimiter.isAllowed(
                java.util.List.of("ratelimit:register:ip:" + (clientIp == null || clientIp.isBlank() ? "unknown" : clientIp)),
                registerMaxAttempts, registerWindowSeconds)) {
            throw new BusinessException("Too many registration attempts. Please try again later.", "RATE_LIMIT_EXCEEDED", 429);
        }

        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("Email already registered", "EMAIL_ALREADY_EXISTS", 409);
        }

        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                request.phoneNumber(),
                User.Role.CUSTOMER
        );
        try {
            user = userRepository.save(user);
        } catch (PersistenceException e) {
            // Two concurrent registers for the same email race past existsByEmail()
            // and collide on the users.email unique constraint.
            if (isEmailUniqueViolation(e)) {
                throw new BusinessException("Email already registered", "EMAIL_ALREADY_EXISTS", 409);
            }
            throw e;
        }

        return createAuthSession(user, UUID.randomUUID());
    }

    private boolean isEmailUniqueViolation(Throwable e) {
        while (e != null) {
            if (e instanceof ConstraintViolationException cve
                    && cve.getConstraintName() != null
                    && cve.getConstraintName().toLowerCase().contains("email")) {
                return true;
            }
            e = e.getCause();
        }
        return false;
    }

    @Transactional
    public AuthResponse login(LoginRequest request, String clientIp) {
        String normalizedEmail = request.email().trim().toLowerCase();

        // Throttle BEFORE any lookup so enumeration attempts against unknown
        // addresses are counted too. Keyed by both email and source IP.
        if (!loginRateLimiter.isAllowed(List.of(
                "ratelimit:login:email:" + normalizedEmail,
                "ratelimit:login:ip:" + (clientIp == null || clientIp.isBlank() ? "unknown" : clientIp)),
                loginMaxAttempts, loginWindowSeconds)) {
            throw new BusinessException("Too many login attempts. Please try again later.", "RATE_LIMIT_EXCEEDED", 429);
        }

        Optional<User> found = userRepository.findByEmail(normalizedEmail);
        // Always burn the BCrypt cost, even for unknown emails (constant time)
        boolean passwordMatches = passwordEncoder.matches(
                request.password(),
                found.map(User::getPasswordHash).orElse(DUMMY_PASSWORD_HASH));

        if (found.isEmpty() || !passwordMatches) {
            found.ifPresent(user -> {
                // Account lockout: N consecutive failures within the window locks the
                // account until an admin unlocks it.
                long failures = loginRateLimiter.recordFailure("lockout:fail:" + normalizedEmail, lockoutWindowSeconds);
                if (failures >= lockoutMaxFailures && !user.isLocked()) {
                    user.setLocked(true);
                    userRepository.save(user);
                }
            });
            throw new BusinessException("Invalid email or password", "INVALID_CREDENTIALS", 401);
        }

        User user = found.get();
        if (user.isLocked()) {
            throw new BusinessException("Account is locked", "ACCOUNT_LOCKED", 403);
        }

        loginRateLimiter.resetFailures("lockout:fail:" + normalizedEmail);
        return createAuthSession(user, UUID.randomUUID());
    }

    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken, String clientIp) {
        // Brute-force guard on refresh token guessing, keyed per source IP
        if (!loginRateLimiter.isAllowed(
                java.util.List.of("ratelimit:refresh:ip:" + (clientIp == null || clientIp.isBlank() ? "unknown" : clientIp)),
                refreshMaxAttempts, refreshWindowSeconds)) {
            throw new BusinessException("Too many refresh attempts. Please try again later.", "RATE_LIMIT_EXCEEDED", 429);
        }

        String hash = hashToken(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException("Invalid refresh token", "INVALID_TOKEN", 401));

        if (token.isRevoked() || token.getExpiresAt().isBefore(Instant.now())) {
            // Token Reuse Attack Detected: Revoke entire token family
            refreshTokenRepository.revokeFamily(token.getFamilyId());
            throw new BusinessException("Token expired or revoked. Session terminated.", "TOKEN_REUSE_DETECTED", 401);
        }

        // Revoke current token upon rotation
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        User user = userRepository.findByIdOptional(token.getUserId())
                .orElseThrow(() -> new BusinessException("User not found", "USER_NOT_FOUND", 404));

        return createAuthSession(user, token.getFamilyId());
    }

    private AuthResponse createAuthSession(User user, UUID familyId) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = UUID.randomUUID() + "-" + UUID.randomUUID();
        String tokenHash = hashToken(rawRefreshToken);

        RefreshToken refreshToken = new RefreshToken(
                user.getId(),
                tokenHash,
                familyId,
                Instant.now().plus(refreshTokenExpirationDays, ChronoUnit.DAYS)
        );
        refreshTokenRepository.save(refreshToken);

        return new AuthResponse(
                accessToken,
                rawRefreshToken,
                user.getId(),
                user.getEmail(),
                user.getFullName(),
                user.getRole().name()
        );
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 not supported", e);
        }
    }
}
