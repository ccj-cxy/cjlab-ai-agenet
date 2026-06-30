package io.github.cjlab.agent.memory.repository.mybatis;

import io.github.cjlab.agent.memory.ConversationSummary;
import io.github.cjlab.agent.memory.ConversationSummaryRepository;
import io.github.cjlab.agent.memory.persistence.entity.ConversationSummaryEntity;
import io.github.cjlab.agent.memory.persistence.mapper.ConversationSummaryMapper;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

public class MyBatisPlusConversationSummaryRepository implements ConversationSummaryRepository {

    private final ConversationSummaryMapper conversationSummaryMapper;

    public MyBatisPlusConversationSummaryRepository(ConversationSummaryMapper conversationSummaryMapper) {
        this.conversationSummaryMapper = conversationSummaryMapper;
    }

    @Override
    public ConversationSummary save(ConversationSummary summary) {
        ConversationSummaryEntity existing = conversationSummaryMapper.selectById(summary.conversationId());
        ConversationSummaryEntity entity = toEntity(summary, existing);
        if (existing == null) {
            conversationSummaryMapper.insert(entity);
        } else {
            conversationSummaryMapper.updateById(entity);
        }
        return summary;
    }

    @Override
    public Optional<ConversationSummary> findByConversationId(String conversationId) {
        return Optional.ofNullable(conversationSummaryMapper.selectById(conversationId))
                .map(this::toDomain);
    }

    private ConversationSummaryEntity toEntity(ConversationSummary summary, ConversationSummaryEntity existing) {
        ConversationSummaryEntity entity = new ConversationSummaryEntity();
        entity.setConversationId(summary.conversationId());
        entity.setContent(summary.content());
        entity.setMessageCount(summary.messageCount());
        entity.setCreateTime(existing == null ? new Date() : existing.getCreateTime());
        entity.setUpdateTime(toDate(summary.updatedAt()));
        entity.setDeleted(false);
        return entity;
    }

    private ConversationSummary toDomain(ConversationSummaryEntity entity) {
        return new ConversationSummary(
                entity.getConversationId(),
                entity.getContent(),
                entity.getMessageCount() == null ? 0 : entity.getMessageCount(),
                toInstant(entity.getUpdateTime())
        );
    }

    private Date toDate(Instant instant) {
        return instant == null ? new Date() : Date.from(instant);
    }

    private Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
