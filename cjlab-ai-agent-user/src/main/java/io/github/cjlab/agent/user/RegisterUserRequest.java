package io.github.cjlab.agent.user;

public record RegisterUserRequest(
        String email,
        String password,
        String displayName
) {
}
