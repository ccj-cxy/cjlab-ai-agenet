package io.github.cjlab.agent.rag.repository.mybatis;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.cjlab.agent.common.AgentException;
import io.github.cjlab.agent.rag.KnowledgeDocument;
import io.github.cjlab.agent.rag.KnowledgeRepository;
import io.github.cjlab.agent.rag.persistence.entity.KnowledgeDocumentEntity;
import io.github.cjlab.agent.rag.persistence.mapper.KnowledgeDocumentMapper;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class MyBatisPlusKnowledgeRepository implements KnowledgeRepository {

    private static final TypeReference<Map<String, Object>> METADATA_TYPE = new TypeReference<>() {
    };

    private final KnowledgeDocumentMapper knowledgeDocumentMapper;
    private final ObjectMapper objectMapper;

    public MyBatisPlusKnowledgeRepository(KnowledgeDocumentMapper knowledgeDocumentMapper) {
        this.knowledgeDocumentMapper = knowledgeDocumentMapper;
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public KnowledgeDocument save(KnowledgeDocument document) {
        KnowledgeDocumentEntity existing = knowledgeDocumentMapper.selectById(document.id());
        KnowledgeDocumentEntity entity = toEntity(document, existing);
        if (existing == null) {
            knowledgeDocumentMapper.insert(entity);
        } else {
            knowledgeDocumentMapper.updateById(entity);
        }
        return document;
    }

    @Override
    public List<KnowledgeDocument> list() {
        return knowledgeDocumentMapper.selectList(null).stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<KnowledgeDocument> findById(String id) {
        return Optional.ofNullable(knowledgeDocumentMapper.selectById(id))
                .map(this::toDomain);
    }

    private KnowledgeDocumentEntity toEntity(KnowledgeDocument document, KnowledgeDocumentEntity existing) {
        LocalDateTime now = LocalDateTime.now();
        KnowledgeDocumentEntity entity = new KnowledgeDocumentEntity();
        entity.setId(document.id());
        entity.setTitle(document.title());
        entity.setContent(document.content());
        entity.setMetadataJson(writeMetadata(document.metadata()));
        entity.setCreatedAt(existing == null ? now : existing.getCreatedAt());
        entity.setUpdatedAt(now);
        return entity;
    }

    private KnowledgeDocument toDomain(KnowledgeDocumentEntity entity) {
        return new KnowledgeDocument(
                entity.getId(),
                entity.getTitle(),
                entity.getContent(),
                readMetadata(entity.getMetadataJson())
        );
    }

    private String writeMetadata(Map<String, Object> metadata) {
        try {
            return objectMapper.writeValueAsString(metadata == null ? Map.of() : metadata);
        } catch (Exception exception) {
            throw new AgentException("Failed to serialize knowledge metadata.", exception);
        }
    }

    private Map<String, Object> readMetadata(String metadataJson) {
        if (metadataJson == null || metadataJson.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(metadataJson, METADATA_TYPE);
        } catch (Exception exception) {
            throw new AgentException("Failed to deserialize knowledge metadata.", exception);
        }
    }
}
