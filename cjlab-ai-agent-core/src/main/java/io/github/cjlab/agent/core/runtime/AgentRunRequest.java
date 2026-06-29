package io.github.cjlab.agent.core.runtime;

import io.github.cjlab.agent.core.chat.ChatUser;

public record AgentRunRequest(
        String conversationId,
        String message,
        ChatUser user
) {
    public AgentRunRequest(String conversationId, String message) {
        this(conversationId, message, null);
    }
}
