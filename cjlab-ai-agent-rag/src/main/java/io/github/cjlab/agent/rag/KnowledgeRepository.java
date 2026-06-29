package io.github.cjlab.agent.rag;

import java.util.List;
import java.util.Optional;

public interface KnowledgeRepository {

    KnowledgeDocument save(KnowledgeDocument document);

    List<KnowledgeDocument> list();

    Optional<KnowledgeDocument> findById(String id);
}
