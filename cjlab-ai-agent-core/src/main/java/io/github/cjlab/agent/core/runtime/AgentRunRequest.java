package io.github.cjlab.agent.core.runtime;

import io.github.cjlab.agent.core.chat.ChatUser;
import io.github.cjlab.agent.core.chat.ChatRoleCard;

public record AgentRunRequest(
        String conversationId,
        String message,
        ChatUser user,
        ChatRoleCard roleCard,
        String summary
) {
    public AgentRunRequest(String conversationId, String message) {
        this(conversationId, message, null, null, null);
    }

    public AgentRunRequest(String conversationId, String message, ChatUser user) {
        this(conversationId, message, user, null, null);
    }
}
