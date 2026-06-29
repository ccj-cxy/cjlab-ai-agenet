package io.github.cjlab.agent.rag;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

public class InMemoryKnowledgeRepository implements KnowledgeRepository {

    private final Map<String, KnowledgeDocument> documents = new ConcurrentHashMap<>();

    @Override
    public KnowledgeDocument save(KnowledgeDocument document) {
        documents.put(document.id(), document);
        return document;
    }

    @Override
    public List<KnowledgeDocument> list() {
        return List.copyOf(documents.values());
    }

    @Override
    public Optional<KnowledgeDocument> findById(String id) {
        return Optional.ofNullable(documents.get(id));
    }
}
