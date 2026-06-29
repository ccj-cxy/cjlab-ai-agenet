package io.github.cjlab.agent.server.api;

import io.github.cjlab.agent.rag.KnowledgeDocument;
import io.github.cjlab.agent.rag.KnowledgeRepository;
import io.github.cjlab.agent.rag.KnowledgeRetriever;
import io.github.cjlab.agent.rag.RetrievedDocument;
import io.github.cjlab.agent.server.security.CurrentUser;
import io.github.cjlab.agent.server.security.CurrentUserContext;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/knowledge")
public class KnowledgeController {

    private final KnowledgeRepository knowledgeRepository;
    private final KnowledgeRetriever knowledgeRetriever;

    public KnowledgeController(KnowledgeRepository knowledgeRepository, KnowledgeRetriever knowledgeRetriever) {
        this.knowledgeRepository = knowledgeRepository;
        this.knowledgeRetriever = knowledgeRetriever;
    }

    @PostMapping
    public KnowledgeDocument save(@RequestBody KnowledgeDocument document) {
        CurrentUser user = CurrentUserContext.required();
        String id = document.id() == null || document.id().isBlank()
                ? UUID.randomUUID().toString()
                : document.id();
        Map<String, Object> metadata = document.metadata() == null
                ? new java.util.HashMap<>()
                : new java.util.HashMap<>(document.metadata());
        metadata.put("userId", user.id());
        metadata.putIfAbsent("userEmail", user.email());
        return knowledgeRepository.save(new KnowledgeDocument(
                id,
                document.title(),
                document.content(),
                Map.copyOf(metadata)
        ));
    }

    @GetMapping
    public List<KnowledgeDocument> list() {
        CurrentUser user = CurrentUserContext.required();
        return knowledgeRepository.list().stream()
                .filter(document -> document.metadata() != null)
                .filter(document -> user.id().equals(document.metadata().get("userId")))
                .toList();
    }

    @GetMapping("/search")
    public List<RetrievedDocument> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit
    ) {
        return knowledgeRetriever.retrieve(query, limit);
    }
}
