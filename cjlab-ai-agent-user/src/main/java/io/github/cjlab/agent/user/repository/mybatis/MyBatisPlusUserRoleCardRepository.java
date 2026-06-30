package io.github.cjlab.agent.user.repository.mybatis;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import io.github.cjlab.agent.user.UserRoleCard;
import io.github.cjlab.agent.user.UserRoleCardRepository;
import io.github.cjlab.agent.user.persistence.entity.UserRoleCardEntity;
import io.github.cjlab.agent.user.persistence.mapper.UserRoleCardMapper;

import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Optional;

public class MyBatisPlusUserRoleCardRepository implements UserRoleCardRepository {

    private final UserRoleCardMapper userRoleCardMapper;

    public MyBatisPlusUserRoleCardRepository(UserRoleCardMapper userRoleCardMapper) {
        this.userRoleCardMapper = userRoleCardMapper;
    }

    @Override
    public UserRoleCard save(UserRoleCard roleCard) {
        String id = id(roleCard.userId(), roleCard.roleId());
        UserRoleCardEntity existing = userRoleCardMapper.selectById(id);
        UserRoleCardEntity entity = toEntity(id, roleCard, existing);
        if (existing == null) {
            userRoleCardMapper.insert(entity);
        } else {
            userRoleCardMapper.updateById(entity);
        }
        return roleCard;
    }

    @Override
    public List<UserRoleCard> listByUserId(String userId) {
        return userRoleCardMapper.selectList(new LambdaQueryWrapper<UserRoleCardEntity>()
                        .eq(UserRoleCardEntity::getUserId, userId)
                        .orderByDesc(UserRoleCardEntity::getUpdateTime))
                .stream()
                .map(this::toDomain)
                .toList();
    }

    @Override
    public Optional<UserRoleCard> findByUserIdAndRoleId(String userId, String roleId) {
        return Optional.ofNullable(userRoleCardMapper.selectById(id(userId, roleId)))
                .map(this::toDomain);
    }

    private UserRoleCardEntity toEntity(String id, UserRoleCard roleCard, UserRoleCardEntity existing) {
        Date now = new Date();
        UserRoleCardEntity entity = new UserRoleCardEntity();
        entity.setId(id);
        entity.setUserId(roleCard.userId());
        entity.setRoleId(roleCard.roleId());
        entity.setName(roleCard.name());
        entity.setDescription(roleCard.description());
        entity.setInstruction(roleCard.instruction());
        entity.setCreateTime(existing == null ? now : existing.getCreateTime());
        entity.setUpdateTime(toDate(roleCard.updatedAt(), now));
        entity.setDeleted(false);
        return entity;
    }

    private UserRoleCard toDomain(UserRoleCardEntity entity) {
        return new UserRoleCard(
                entity.getUserId(),
                entity.getRoleId(),
                entity.getName(),
                entity.getDescription(),
                entity.getInstruction(),
                toInstant(entity.getUpdateTime())
        );
    }

    private String id(String userId, String roleId) {
        return userId + ":" + roleId;
    }

    private Date toDate(Instant instant, Date defaultValue) {
        return instant == null ? defaultValue : Date.from(instant);
    }

    private Instant toInstant(Date date) {
        return date == null ? null : date.toInstant();
    }
}
