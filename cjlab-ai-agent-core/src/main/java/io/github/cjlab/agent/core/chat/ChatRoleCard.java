package io.github.cjlab.agent.core.chat;

public record ChatRoleCard(
        String id,
        String name,
        String description,
        String instruction,
        String avatar
) {
}
