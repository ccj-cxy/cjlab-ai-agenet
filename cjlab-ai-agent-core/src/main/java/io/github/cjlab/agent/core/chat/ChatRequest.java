package io.github.cjlab.agent.core.chat;

public record ChatRequest(
        String conversationId,
        String message,
        ChatUser user
) {
    public ChatRequest(String conversationId, String message) {
        this(conversationId, message, null);
    }
}
