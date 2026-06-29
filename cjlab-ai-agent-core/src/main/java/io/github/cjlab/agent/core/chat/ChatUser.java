package io.github.cjlab.agent.core.chat;

public record ChatUser(
        String id,
        String email,
        String displayName,
        String status
) {
}
