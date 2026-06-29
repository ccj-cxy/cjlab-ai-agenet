package io.github.cjlab.agent.server.security;

import java.util.UUID;

public final class UserConversationIds {

    private UserConversationIds() {
    }

    public static String internal(String userId, String conversationId) {
        return userId + ":" + external(conversationId);
    }

    public static String external(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        int separatorIndex = conversationId.indexOf(':');
        if (separatorIndex >= 0 && separatorIndex + 1 < conversationId.length()) {
            return conversationId.substring(separatorIndex + 1);
        }
        return conversationId;
    }
}
