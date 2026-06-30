package io.github.cjlab.agent.user.persistence.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import io.github.cjlab.agent.user.persistence.entity.UserRoleCardEntity;
import org.apache.ibatis.annotations.Insert;

public interface UserRoleCardMapper extends BaseMapper<UserRoleCardEntity> {

    @Insert("""
            INSERT INTO cjlab_user_role_card (
                id,
                user_id,
                role_id,
                name,
                description,
                instruction,
                avatar,
                create_time,
                update_time,
                creator,
                updater,
                deleted
            )
            VALUES (
                #{id},
                #{userId},
                #{roleId},
                #{name},
                #{description},
                #{instruction},
                #{avatar},
                #{createTime},
                #{updateTime},
                #{creator},
                #{updater},
                #{deleted}
            )
            ON DUPLICATE KEY UPDATE
                user_id = VALUES(user_id),
                role_id = VALUES(role_id),
                name = VALUES(name),
                description = VALUES(description),
                instruction = VALUES(instruction),
                avatar = VALUES(avatar),
                update_time = VALUES(update_time),
                updater = VALUES(updater),
                deleted = 0
            """)
    int upsert(UserRoleCardEntity entity);
}
