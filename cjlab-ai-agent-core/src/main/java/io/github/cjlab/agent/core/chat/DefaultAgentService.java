package io.github.cjlab.agent.core.chat;

import io.github.cjlab.agent.common.AgentException;
import io.github.cjlab.agent.core.llm.ChatModelGateway;
import io.github.cjlab.agent.core.runtime.AgentRunRequest;
import io.github.cjlab.agent.core.runtime.AgentRunResult;
import io.github.cjlab.agent.core.runtime.AgentRuntime;
import io.github.cjlab.agent.memory.ConversationMemory;
import io.github.cjlab.agent.memory.ConversationMessage;
import io.github.cjlab.agent.memory.ConversationSummary;
import io.github.cjlab.agent.memory.ConversationSummaryRepository;
import io.github.cjlab.agent.memory.MessageRole;

import java.time.Instant;
import java.util.UUID;
import java.util.function.Consumer;

public class DefaultAgentService implements AgentService {

    private final ConversationMemory conversationMemory;
    private final AgentRuntime agentRuntime;
    private final ConversationSummaryRepository conversationSummaryRepository;
    private final ChatModelGateway chatModelGateway;

    public DefaultAgentService(
            ConversationMemory conversationMemory,
            AgentRuntime agentRuntime,
            ConversationSummaryRepository conversationSummaryRepository,
            ChatModelGateway chatModelGateway
    ) {
        this.conversationMemory = conversationMemory;
        this.agentRuntime = agentRuntime;
        this.conversationSummaryRepository = conversationSummaryRepository;
        this.chatModelGateway = chatModelGateway;
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

        ConversationSummary currentSummary = conversationSummaryRepository.findByConversationId(conversationId)
                .orElse(null);
        String summaryContent = currentSummary == null ? null : currentSummary.content();

        AgentRunResult result = chunkConsumer == null
                ? agentRuntime.run(new AgentRunRequest(
                        conversationId,
                        request.message(),
                        request.user(),
                        request.roleCard(),
                        summaryContent
                ))
                : agentRuntime.stream(new AgentRunRequest(
                        conversationId,
                        request.message(),
                        request.user(),
                        request.roleCard(),
                        summaryContent
                ), chunkConsumer);

        conversationMemory.append(new ConversationMessage(
                conversationId,
                MessageRole.ASSISTANT,
                result.content(),
                Instant.now()
        ));
        updateSummary(conversationId, currentSummary, request.message(), result.content());
        return new ChatResponse(conversationId, result.content());
    }

    private void updateSummary(
            String conversationId,
            ConversationSummary currentSummary,
            String userMessage,
            String assistantMessage
    ) {
        String previous = currentSummary == null || currentSummary.content() == null || currentSummary.content().isBlank()
                ? "None"
                : currentSummary.content();
        int messageCount = currentSummary == null ? 2 : currentSummary.messageCount() + 2;
        String prompt = """
                Summarize the conversation memory for future turns.
                Keep durable facts, user preferences, unresolved tasks, important decisions, and useful context.
                Do not include transient wording. Keep it concise.

                Previous summary:
                %s

                Latest exchange:
                USER: %s
                ASSISTANT: %s

                Updated summary:
                """.formatted(previous, userMessage, assistantMessage);
        try {
            String summary = chatModelGateway.chat(prompt);
            if (summary != null && !summary.isBlank()) {
                conversationSummaryRepository.save(new ConversationSummary(
                        conversationId,
                        trimSummary(summary),
                        messageCount,
                        Instant.now()
                ));
            }
        } catch (RuntimeException ignored) {
            // Summary is helpful context, not part of the user-visible response path.
        }
    }

    private String trimSummary(String summary) {
        String normalized = summary.trim();
        int maxLength = 2400;
        if (normalized.length() <= maxLength) {
            return normalized;
        }
        return normalized.substring(0, maxLength);
    }

    private String resolveConversationId(String conversationId) {
        if (conversationId == null || conversationId.isBlank()) {
            return UUID.randomUUID().toString();
        }
        return conversationId;
    }
}
