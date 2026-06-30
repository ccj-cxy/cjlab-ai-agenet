package io.github.cjlab.agent.user;

import java.time.Instant;

public record UserRoleCard(
        String userId,
        String roleId,
        String name,
        String description,
        String instruction,
        Instant updatedAt
) {
}
