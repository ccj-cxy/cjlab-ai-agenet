package io.github.cjlab.agent.memory;

import java.util.Optional;

public interface ConversationSummaryRepository {

    ConversationSummary save(ConversationSummary summary);

    Optional<ConversationSummary> findByConversationId(String conversationId);
}
