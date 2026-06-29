package io.github.cjlab.agent.rag;

import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public class Bm25KnowledgeRetriever implements KnowledgeRetriever {

    private static final double K1 = 1.5;
    private static final double B = 0.75;

    private final KnowledgeRepository knowledgeRepository;

    public Bm25KnowledgeRetriever(KnowledgeRepository knowledgeRepository) {
        this.knowledgeRepository = knowledgeRepository;
    }

    @Override
    public List<RetrievedDocument> retrieve(String query, int limit) {
        List<String> queryTerms = tokenize(query);
        List<KnowledgeDocument> documents = knowledgeRepository.list();
        if (queryTerms.isEmpty() || documents.isEmpty()) {
            return List.of();
        }

        Map<String, Integer> documentFrequency = documentFrequency(documents);
        double averageLength = documents.stream()
                .mapToInt(document -> tokenize(text(document)).size())
                .average()
                .orElse(1.0);

        return documents.stream()
                .map(document -> new RetrievedDocument(
                        document,
                        score(document, queryTerms, documentFrequency, documents.size(), averageLength)
                ))
                .filter(document -> document.score() > 0)
                .sorted(Comparator.comparingDouble(RetrievedDocument::score).reversed())
                .limit(limit)
                .toList();
    }

    private double score(
            KnowledgeDocument document,
            List<String> queryTerms,
            Map<String, Integer> documentFrequency,
            int documentCount,
            double averageLength
    ) {
        List<String> documentTerms = tokenize(text(document));
        Map<String, Long> termFrequency = documentTerms.stream()
                .collect(Collectors.groupingBy(term -> term, Collectors.counting()));
        double documentLength = Math.max(1, documentTerms.size());
        double score = 0;
        for (String term : new HashSet<>(queryTerms)) {
            long frequency = termFrequency.getOrDefault(term, 0L);
            if (frequency == 0) {
                continue;
            }
            int df = documentFrequency.getOrDefault(term, 0);
            double idf = Math.log(1 + (documentCount - df + 0.5) / (df + 0.5));
            double numerator = frequency * (K1 + 1);
            double denominator = frequency + K1 * (1 - B + B * documentLength / averageLength);
            score += idf * numerator / denominator;
        }
        return score;
    }

    private Map<String, Integer> documentFrequency(List<KnowledgeDocument> documents) {
        Map<String, Integer> frequency = new HashMap<>();
        for (KnowledgeDocument document : documents) {
            Set<String> terms = new HashSet<>(tokenize(text(document)));
            for (String term : terms) {
                frequency.merge(term, 1, Integer::sum);
            }
        }
        return frequency;
    }

    private String text(KnowledgeDocument document) {
        return document.title() + " " + document.content();
    }

    private List<String> tokenize(String text) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        return List.of(text.toLowerCase(Locale.ROOT).split("[^\\p{IsAlphabetic}\\p{IsDigit}\\p{IsIdeographic}]+"))
                .stream()
                .filter(term -> !term.isBlank())
                .toList();
    }
}
