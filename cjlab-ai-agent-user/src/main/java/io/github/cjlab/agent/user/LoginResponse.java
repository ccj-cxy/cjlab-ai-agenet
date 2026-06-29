package io.github.cjlab.agent.user;

import java.time.Instant;

public record LoginResponse(
        String accessToken,
        Instant expiresAt,
        UserProfileResponse user
) {
}
