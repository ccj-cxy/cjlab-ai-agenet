package io.github.cjlab.agent.memory.repository.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.cjlab.agent.memory.ConversationMemory;
import io.github.cjlab.agent.memory.ConversationMessage;
import io.github.cjlab.agent.memory.MessageRole;
import io.github.cjlab.agent.memory.persistence.entity.ConversationMessageEntity;
import io.github.cjlab.agent.memory.persistence.mapper.ConversationMessageMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class MyBatisPlusConversationMemory implements ConversationMemory {

    private final ConversationMessageMapper conversationMessageMapper;
    private final ZoneId zoneId;

    public MyBatisPlusConversationMemory(ConversationMessageMapper conversationMessageMapper) {
        this.conversationMessageMapper = conversationMessageMapper;
        this.zoneId = ZoneId.systemDefault();
    }

    @Override
    public void append(ConversationMessage message) {
        conversationMessageMapper.insert(toEntity(message));
    }

    @Override
    public List<ConversationMessage> recent(String conversationId, int limit) {
        int safeLimit = Math.max(1, limit);
        return conversationMessageMapper.selectList(new LambdaQueryWrapper<ConversationMessageEntity>()
                        .eq(ConversationMessageEntity::getConversationId, conversationId)
                        .orderByDesc(ConversationMessageEntity::getCreatedAt)
                        .last("LIMIT " + safeLimit))
                .stream()
                .map(this::toDomain)
                .sorted(Comparator.comparing(ConversationMessage::createdAt))
                .toList();
    }

    private ConversationMessageEntity toEntity(ConversationMessage message) {
        ConversationMessageEntity entity = new ConversationMessageEntity();
        entity.setId(UUID.randomUUID().toString());
        entity.setConversationId(message.conversationId());
        entity.setRole(message.role().name());
        entity.setContent(message.content());
        entity.setCreatedAt(toLocalDateTime(message.createdAt()));
        return entity;
    }

    private ConversationMessage toDomain(ConversationMessageEntity entity) {
        return new ConversationMessage(
                entity.getConversationId(),
                MessageRole.valueOf(entity.getRole()),
                entity.getContent(),
                toInstant(entity.getCreatedAt())
        );
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, zoneId);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(zoneId).toInstant();
    }
}
