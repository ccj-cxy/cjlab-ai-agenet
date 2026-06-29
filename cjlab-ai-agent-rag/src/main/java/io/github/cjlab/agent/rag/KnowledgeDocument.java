package io.github.cjlab.agent.rag;

import java.util.Map;

public record KnowledgeDocument(
        String id,
        String title,
        String content,
        Map<String, Object> metadata
) {
}
