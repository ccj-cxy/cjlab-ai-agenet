package io.github.cjlab.agent.rag;

import java.util.List;

public interface KnowledgeRetriever {

    List<RetrievedDocument> retrieve(String query, int limit);
}
