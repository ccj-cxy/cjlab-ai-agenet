package io.github.cjlab.agent.infrastructure.agentscope;

import io.github.cjlab.agent.memory.ConversationMemory;
import io.github.cjlab.agent.memory.ConversationMessage;

import java.util.List;

public class AgentScopeMemoryAdapter {

    private final ConversationMemory conversationMemory;

    public AgentScopeMemoryAdapter(ConversationMemory conversationMemory) {
        this.conversationMemory = conversationMemory;
    }

    public List<ConversationMessage> recent(String conversationId, int limit) {
        return conversationMemory.recent(conversationId, limit);
    }
}
