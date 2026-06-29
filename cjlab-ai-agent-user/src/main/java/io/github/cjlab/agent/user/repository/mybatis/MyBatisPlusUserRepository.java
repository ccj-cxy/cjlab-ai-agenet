package io.github.cjlab.agent.user.repository.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.cjlab.agent.user.UserAccount;
import io.github.cjlab.agent.user.UserRepository;
import io.github.cjlab.agent.user.UserStatus;
import io.github.cjlab.agent.user.persistence.entity.UserAccountEntity;
import io.github.cjlab.agent.user.persistence.mapper.UserAccountMapper;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Optional;

public class MyBatisPlusUserRepository implements UserRepository {

    private final UserAccountMapper userAccountMapper;
    private final ZoneId zoneId;

    public MyBatisPlusUserRepository(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
        this.zoneId = ZoneId.systemDefault();
    }

    @Override
    public UserAccount save(UserAccount user) {
        userAccountMapper.insert(toEntity(user));
        return user;
    }

    @Override
    public Optional<UserAccount> findById(String id) {
        return Optional.ofNullable(userAccountMapper.selectById(id))
                .map(this::toDomain);
    }

    @Override
    public Optional<UserAccount> findByEmail(String email) {
        return Optional.ofNullable(userAccountMapper.selectOne(new LambdaQueryWrapper<UserAccountEntity>()
                        .eq(UserAccountEntity::getEmail, normalizeEmail(email))
                        .last("LIMIT 1")))
                .map(this::toDomain);
    }

    @Override
    public boolean existsByEmail(String email) {
        Long count = userAccountMapper.selectCount(new LambdaQueryWrapper<UserAccountEntity>()
                .eq(UserAccountEntity::getEmail, normalizeEmail(email)));
        return count != null && count > 0;
    }

    private UserAccountEntity toEntity(UserAccount user) {
        UserAccountEntity entity = new UserAccountEntity();
        entity.setId(user.id());
        entity.setEmail(user.email());
        entity.setDisplayName(user.displayName());
        entity.setPasswordHash(user.passwordHash());
        entity.setStatus(user.status().name());
        entity.setCreatedAt(toLocalDateTime(user.createdAt()));
        entity.setUpdatedAt(toLocalDateTime(user.updatedAt()));
        return entity;
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        return new UserAccount(
                entity.getId(),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.getPasswordHash(),
                UserStatus.valueOf(entity.getStatus()),
                toInstant(entity.getCreatedAt()),
                toInstant(entity.getUpdatedAt())
        );
    }

    private LocalDateTime toLocalDateTime(Instant instant) {
        return instant == null ? null : LocalDateTime.ofInstant(instant, zoneId);
    }

    private Instant toInstant(LocalDateTime localDateTime) {
        return localDateTime == null ? null : localDateTime.atZone(zoneId).toInstant();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
