package com.reloop.auth.service;

import com.reloop.auth.domain.RefreshToken;
import com.reloop.auth.domain.User;
import com.reloop.auth.dto.AuthResponse;
import com.reloop.auth.dto.LoginRequest;
import com.reloop.auth.dto.RegisterRequest;
import com.reloop.auth.repository.RefreshTokenRepository;
import com.reloop.auth.repository.UserRepository;
import com.reloop.common.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.UUID;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final long refreshTokenExpirationDays;

    public AuthService(
            UserRepository userRepository,
            RefreshTokenRepository refreshTokenRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            @Value("${jwt.refresh-token-expiration-days:7}") long refreshTokenExpirationDays
    ) {
        this.userRepository = userRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.refreshTokenExpirationDays = refreshTokenExpirationDays;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        if (userRepository.existsByEmail(normalizedEmail)) {
            throw new BusinessException("Email already registered", "EMAIL_ALREADY_EXISTS", HttpStatus.CONFLICT);
        }

        User.Role role = User.Role.CUSTOMER;
        if (request.role() != null) {
            try {
                role = User.Role.valueOf(request.role().toUpperCase());
            } catch (IllegalArgumentException ignored) {}
        }

        User user = new User(
                normalizedEmail,
                passwordEncoder.encode(request.password()),
                request.fullName().trim(),
                request.phoneNumber(),
                role
        );
        user = userRepository.save(user);

        return createAuthSession(user, UUID.randomUUID());
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        String normalizedEmail = request.email().trim().toLowerCase();
        User user = userRepository.findByEmail(normalizedEmail)
                .orElseThrow(() -> new BusinessException("Invalid email or password", "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED));

        if (user.isLocked()) {
            throw new BusinessException("Account is locked", "ACCOUNT_LOCKED", HttpStatus.FORBIDDEN);
        }

        if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
            throw new BusinessException("Invalid email or password", "INVALID_CREDENTIALS", HttpStatus.UNAUTHORIZED);
        }

        return createAuthSession(user, UUID.randomUUID());
    }

    @Transactional
    public AuthResponse refreshToken(String rawRefreshToken) {
        String hash = hashToken(rawRefreshToken);
        RefreshToken token = refreshTokenRepository.findByTokenHash(hash)
                .orElseThrow(() -> new BusinessException("Invalid refresh token", "INVALID_TOKEN", HttpStatus.UNAUTHORIZED));

        if (token.isRevoked() || token.getExpiresAt().isBefore(Instant.now())) {
            // Token Reuse Attack Detected: Revoke entire token family
            refreshTokenRepository.revokeFamily(token.getFamilyId());
            throw new BusinessException("Token expired or revoked. Session terminated.", "TOKEN_REUSE_DETECTED", HttpStatus.UNAUTHORIZED);
        }

        // Revoke current token upon rotation
        token.setRevoked(true);
        refreshTokenRepository.save(token);

        User user = userRepository.findById(token.getUserId())
                .orElseThrow(() -> new BusinessException("User not found", "USER_NOT_FOUND", HttpStatus.NOT_FOUND));

        return createAuthSession(user, token.getFamilyId());
    }

    private AuthResponse createAuthSession(User user, UUID familyId) {
        String accessToken = jwtService.generateAccessToken(user);
        String rawRefreshToken = UUID.randomUUID().toString() + "-" + UUID.randomUUID().toString();
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
