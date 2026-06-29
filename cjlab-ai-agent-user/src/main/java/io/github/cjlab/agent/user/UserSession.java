package io.github.cjlab.agent.user;

import java.time.Instant;

public record UserSession(
        String token,
        String userId,
        Instant createdAt,
        Instant expiresAt
) {

    public boolean expired(Instant now) {
        return !expiresAt.isAfter(now);
    }
}
