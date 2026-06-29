package io.github.cjlab.agent.user.repository.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import io.github.cjlab.agent.user.UserSession;
import io.github.cjlab.agent.user.UserSessionService;
import io.github.cjlab.agent.user.persistence.entity.UserSessionEntity;
import io.github.cjlab.agent.user.persistence.mapper.UserSessionMapper;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Date;
import java.util.Optional;

public class MyBatisPlusUserSessionService implements UserSessionService {

    private static final int TOKEN_BYTES = 32;

    private final UserSessionMapper userSessionMapper;
    private final SecureRandom secureRandom = new SecureRandom();
    private final Duration ttl;
    private final ZoneId zoneId;

    public MyBatisPlusUserSessionService(UserSessionMapper userSessionMapper, Duration ttl) {
        this.userSessionMapper = userSessionMapper;
        this.ttl = ttl;
        this.zoneId = ZoneId.systemDefault();
    }

    @Override
    public UserSession create(String userId) {
        Instant now = Instant.now();
        UserSession session = new UserSession(newToken(), userId, now, now.plus(ttl));
        userSessionMapper.insert(toEntity(session));
        return session;
    }

    @Override
    public Optional<UserSession> findByToken(String token) {
        if (token == null || token.isBlank()) {
            return Optional.empty();
        }
        UserSession session = Optional.ofNullable(userSessionMapper.selectById(token))
                .map(this::toDomain)
                .orElse(null);
        if (session == null) {
            return Optional.empty();
        }
        if (session.expired(Instant.now())) {
            userSessionMapper.deleteById(token);
            return Optional.empty();
        }
        return Optional.of(session);
    }

    @Override
    public Duration ttl() {
        return ttl;
    }

    public void deleteExpired() {
        userSessionMapper.delete(new LambdaQueryWrapper<UserSessionEntity>()
                .le(UserSessionEntity::getExpiresAt, LocalDateTime.now()));
    }

    public void deleteByUserId(String userId) {
        userSessionMapper.delete(new LambdaUpdateWrapper<UserSessionEntity>()
                .eq(UserSessionEntity::getUserId, userId));
    }

    private UserSessionEntity toEntity(UserSession session) {
        UserSessionEntity entity = new UserSessionEntity();
        entity.setToken(session.token());
        entity.setUserId(session.userId());
        entity.setCreateTime(toDate(session.createdAt()));
        entity.setExpiresAt(toLocalDateTime(session.expiresAt()));
        entity.setDeleted(false);
        return entity;
    }

    private UserSession toDomain(UserSessionEntity entity) {
        return new UserSession(
                entity.getToken(),
                entity.getUserId(),
                toInstant(entity.getCreateTime()),
                toInstant(entity.getExpiresAt())
        );
    }

    private Date toDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }

    private Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, zoneId);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(zoneId).toInstant();
    }

    private String newToken() {
        byte[] bytes = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
