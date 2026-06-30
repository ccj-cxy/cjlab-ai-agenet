package io.github.cjlab.agent.core.chat;

public record ChatRequest(
        String conversationId,
        String message,
        ChatUser user,
        ChatRoleCard roleCard,
        String summary
) {
    public ChatRequest(String conversationId, String message) {
        this(conversationId, message, null, null, null);
    }

    public ChatRequest(String conversationId, String message, ChatUser user) {
        this(conversationId, message, user, null, null);
    }
}
