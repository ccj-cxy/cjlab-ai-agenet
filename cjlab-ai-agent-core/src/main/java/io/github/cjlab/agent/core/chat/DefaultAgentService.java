package io.github.cjlab.agent.core.chat;

import io.github.cjlab.agent.common.AgentException;
import io.github.cjlab.agent.core.runtime.AgentRunRequest;
import io.github.cjlab.agent.core.runtime.AgentRunResult;
import io.github.cjlab.agent.core.runtime.AgentRuntime;
import io.github.cjlab.agent.memory.ConversationMemory;
import io.github.cjlab.agent.memory.ConversationMessage;
import io.github.cjlab.agent.memory.MessageRole;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

public class DefaultAgentService implements AgentService {

    private final ConversationMemory conversationMemory;
    private final AgentRuntime agentRuntime;

    public DefaultAgentService(ConversationMemory conversationMemory, AgentRuntime agentRuntime) {
        this.conversationMemory = conversationMemory;
        this.agentRuntime = agentRuntime;
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        return doChat(request, null);
    }

    @Override
    public ChatResponse streamChat(ChatRequest request, Consumer<String> chunkConsumer) {
        return doChat(request, chunkConsumer);
    }

    private ChatResponse doChat(ChatRequest request, Consumer<String> chunkConsumer) {
        if (request == null || request.message() == null || request.message().isBlank()) {
            throw new AgentException("Message must not be blank.");
        }
        String conversationId = resolveConversationId(request.conversationId());
        conversationMemory.append(new ConversationMessage(
                conversationId,
                MessageRole.USER,
                request.message(),
                Instant.now()
        ));

        AgentRunResult result = chunkConsumer == null
                ? agentRuntime.run(new AgentRunRequest(conversationId, request.message()))
                : agentRuntime.stream(new AgentRunRequest(conversationId, request.message()), chunkConsumer);

        conversationMemory.append(new ConversationMessage(
                conversationId,
                MessageRole.ASSISTANT,
                result.content(),
                Instant.now()
        ));
        return new ChatResponse(conversationId, result.content());
    }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return conversationId;
    }
}
