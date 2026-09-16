package com.reloop.auth.repository;

import com.reloop.auth.domain.User;
import com.reloop.common.jpa.ReloopRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;

@ApplicationScoped
public class UserRepository implements ReloopRepository<User, Long> {

    public Optional<User> findByEmail(String email) {
        return find("email", email).firstResultOptional();
    }

    public Optional<User> findByFirebaseUid(String firebaseUid) {
        return find("firebaseUid", firebaseUid).firstResultOptional();
    }

    public boolean existsByEmail(String email) {
        return count("email", email) > 0;
    }
}
