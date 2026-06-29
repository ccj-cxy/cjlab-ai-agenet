package io.github.cjlab.agent.memory;

import java.util.List;

public interface ConversationMemory {

    void append(ConversationMessage message);

    List<ConversationMessage> recent(String conversationId, int limit);
}
