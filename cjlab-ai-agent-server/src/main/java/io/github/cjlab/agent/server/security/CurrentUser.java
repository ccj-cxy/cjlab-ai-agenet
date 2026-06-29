package io.github.cjlab.agent.server.security;

import io.github.cjlab.agent.user.UserStatus;

import java.time.Instant;

public record CurrentUser(
        String id,
        String email,
        String displayName,
        UserStatus status,
        Instant createdAt
) {
}
