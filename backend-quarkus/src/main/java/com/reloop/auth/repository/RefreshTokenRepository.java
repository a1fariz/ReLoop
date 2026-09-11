package com.reloop.auth.repository;

import com.reloop.auth.domain.RefreshToken;
import com.reloop.common.jpa.ReloopRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class RefreshTokenRepository implements ReloopRepository<RefreshToken, UUID> {

    public Optional<RefreshToken> findByTokenHash(String tokenHash) {
        return find("tokenHash", tokenHash).firstResultOptional();
    }

    public void revokeFamily(UUID familyId) {
        update("isRevoked = true WHERE familyId = ?1", familyId);
    }
}
