package io.github.cjlab.agent.memory;

import java.time.Instant;

public record ConversationSummary(
        String conversationId,
        String content,
        int messageCount,
        Instant updatedAt
) {
}
