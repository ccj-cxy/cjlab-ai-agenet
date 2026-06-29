package io.github.cjlab.agent.core.chat;

public record ChatRequest(
        String conversationId,
        String message
) {
}
