package io.github.cjlab.agent.memory;

import java.time.Instant;

public record ConversationMessage(
        String conversationId,
        MessageRole role,
        String content,
        Instant createdAt
) {
}
