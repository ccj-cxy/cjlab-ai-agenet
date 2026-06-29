package io.github.cjlab.agent.rag;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

public class SimpleKeywordKnowledgeRetriever implements KnowledgeRetriever {

    private final KnowledgeRepository knowledgeRepository;

    public SimpleKeywordKnowledgeRetriever(KnowledgeRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
    }

    @Override
    public List<RetrievedDocument> retrieve(String query, int limit) {
        Set<String> terms = tokenize(query);
        if (terms.isEmpty()) {
            return List.of();
        }
        return knowledgeRepository.list().stream()
                .map(document -> new RetrievedDocument(document, score(document, terms)))
                .filter(document -> document.score() > 0)
                .sorted(Comparator.comparingDouble(RetrievedDocument::score).reversed())
                .limit(limit)
                .toList();
    }

    private double score(KnowledgeDocument document, Set<String> terms) {
        String text = (document.title() + " " + document.content()).toLowerCase(Locale.ROOT);
        long hits = terms.stream().filter(text::contains).count();
        return (double) hits / terms.size();
    }

    private Set<String> tokenize(String query) {
        if (query == null || query.isBlank()) {
            return Set.of();
        }
        return List.of(query.toLowerCase(Locale.ROOT).split("\\s+")).stream()
                .filter(term -> !term.isBlank())
                .collect(Collectors.toSet());
    }
}
