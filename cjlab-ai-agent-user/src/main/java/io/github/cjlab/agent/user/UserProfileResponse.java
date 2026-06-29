package io.github.cjlab.agent.user;

import java.time.Instant;

public record UserProfileResponse(
        String id,
        String email,
        String displayName,
        UserStatus status,
        Instant createdAt
) {

    public static UserProfileResponse from(UserAccount user) {
        return new UserProfileResponse(
                user.id(),
                user.email(),
                user.displayName(),
                user.status(),
                user.createdAt()
        );
    }
}
