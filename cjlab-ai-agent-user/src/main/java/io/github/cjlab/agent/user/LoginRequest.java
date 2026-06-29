package io.github.cjlab.agent.user;

public record LoginRequest(
        String email,
        String password
) {
}
