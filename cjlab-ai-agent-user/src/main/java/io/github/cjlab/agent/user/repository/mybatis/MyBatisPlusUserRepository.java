package io.github.cjlab.agent.user.repository.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.cjlab.agent.user.UserAccount;
import io.github.cjlab.agent.user.UserRepository;
import io.github.cjlab.agent.user.UserStatus;
import io.github.cjlab.agent.user.persistence.entity.UserAccountEntity;
import io.github.cjlab.agent.user.persistence.mapper.UserAccountMapper;

import java.time.Instant;
import java.util.Date;
import java.util.Optional;

public class MyBatisPlusUserRepository implements UserRepository {

    private final UserAccountMapper userAccountMapper;

    public MyBatisPlusUserRepository(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
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
        entity.setCreateTime(toDate(user.createdAt()));
        entity.setUpdateTime(toDate(user.updatedAt()));
        entity.setDeleted(false);
        return entity;
    }

    private UserAccount toDomain(UserAccountEntity entity) {
        return new UserAccount(
                entity.getId(),
                entity.getEmail(),
                entity.getDisplayName(),
                entity.getPasswordHash(),
                UserStatus.valueOf(entity.getStatus()),
                toInstant(entity.getCreateTime()),
                toInstant(entity.getUpdateTime())
        );
    }

    private Date toDate(Instant instant) {
        return instant == null ? null : Date.from(instant);
    }

    private Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }
}
