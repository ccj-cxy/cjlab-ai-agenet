package io.github.cjlab.agent.rag;

public record RetrievedDocument(
        KnowledgeDocument document,
        double score
) {
}
