package io.github.cjlab.agent.server.security;

public record CurrentUser(
        String id,
        String email,
        String displayName
) {
}
