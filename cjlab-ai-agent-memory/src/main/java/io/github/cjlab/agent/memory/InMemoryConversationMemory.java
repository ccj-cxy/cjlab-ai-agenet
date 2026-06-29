package io.github.cjlab.agent.memory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryConversationMemory implements ConversationMemory {

    private final Map<String, List<ConversationMessage>> messages = new ConcurrentHashMap<>();

    @Override
    public void append(ConversationMessage message) {
        messages.computeIfAbsent(message.conversationId(), ignored -> new ArrayList<>()).add(message);
    }

    @Override
    public List<ConversationMessage> recent(String conversationId, int limit) {
        List<ConversationMessage> conversationMessages = messages.getOrDefault(conversationId, List.of());
        int fromIndex = Math.max(0, conversationMessages.size() - limit);
        return List.copyOf(conversationMessages.subList(fromIndex, conversationMessages.size()));
    }
}
