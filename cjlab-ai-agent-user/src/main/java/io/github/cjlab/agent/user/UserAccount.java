package io.github.cjlab.agent.user;

import java.time.Instant;

public record UserAccount(
        String id,
        String email,
        String displayName,
        String passwordHash,
        UserStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
