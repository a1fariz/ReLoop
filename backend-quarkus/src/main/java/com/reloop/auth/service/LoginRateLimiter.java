package com.reloop.auth.service;

import io.quarkus.redis.datasource.RedisDataSource;
import io.quarkus.redis.datasource.string.SetArgs;
import io.quarkus.redis.datasource.string.StringCommands;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.Collection;

/**
 * Fixed-window throttle backed by Redis. The window key is created with
 * SET NX EX (atomic, always carries a TTL) then INCR'd — a crash can never
 * leave an expiry-less key, which would lock the key's owner out forever.
 * Degrades open: if Redis is unavailable, requests proceed rather than
 * turning a cache outage into an outage of the protected flow.
 */
@ApplicationScoped
public class LoginRateLimiter {
    private static final Logger log = Logger.getLogger(LoginRateLimiter.class);

    private final StringCommands<String, String> commands;

    public LoginRateLimiter(RedisDataSource redisDataSource) {
        this.commands = redisDataSource.string(String.class, String.class);
    }

    /**
     * @param keys throttle keys (one per identity dimension, e.g. email and client
     *             IP); the attempt counts against every key and any exhausted key denies.
     */
    public boolean isAllowed(Collection<String> keys, int maxAttempts, int windowSeconds) {
        try {
            for (String key : keys) {
                // Atomic window bootstrap: key always has a TTL, even under concurrency
                commands.set(key, "0", new SetArgs().nx().ex(windowSeconds));
                long attempts = commands.incr(key);
                if (attempts > maxAttempts) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            log.warnf("Rate limiter unavailable, allowing attempt: %s", e.getMessage());
            return true;
        }
    }

    /** Increments the failure counter for a key; returns the new count. */
    public long recordFailure(String key, int windowSeconds) {
        try {
            commands.set(key, "0", new SetArgs().nx().ex(windowSeconds));
            return commands.incr(key);
        } catch (Exception e) {
            log.warnf("Lockout counter unavailable: %s", e.getMessage());
            return 0;
        }
    }

    /** Clears the failure counter after a successful authentication. */
    public void resetFailures(String key) {
        try {
            // 3.15 StringCommands has no DEL; a 1-second empty window resets the counter
            commands.set(key, "0", new SetArgs().ex(1));
        } catch (Exception e) {
            log.warnf("Lockout reset unavailable: %s", e.getMessage());
        }
    }
}
