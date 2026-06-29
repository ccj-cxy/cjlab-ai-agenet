package io.github.cjlab.agent.server.security;

import io.github.cjlab.agent.rag.KnowledgeRetriever;
import io.github.cjlab.agent.rag.RetrievedDocument;

import java.util.List;
import java.util.Objects;

public class UserScopedKnowledgeRetriever implements KnowledgeRetriever {

    private static final int OVERSAMPLE_MULTIPLIER = 6;

    private final KnowledgeRetriever delegate;

    public UserScopedKnowledgeRetriever(KnowledgeRetriever delegate) {
        this.delegate = delegate;
    }

    @Override
    public List<RetrievedDocument> retrieve(String query, int limit) {
        CurrentUser user = CurrentUserContext.required();
        int delegateLimit = Math.max(limit * OVERSAMPLE_MULTIPLIER, limit);
        return delegate.retrieve(query, delegateLimit).stream()
                .filter(document -> document.document().metadata() != null)
                .filter(document -> Objects.equals(user.id(), document.document().metadata().get("userId")))
                .limit(limit)
                .toList();
    }
}
