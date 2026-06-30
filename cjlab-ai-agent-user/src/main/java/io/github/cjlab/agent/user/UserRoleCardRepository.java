package io.github.cjlab.agent.user;

import java.util.List;
import java.util.Optional;

public interface UserRoleCardRepository {

    UserRoleCard save(UserRoleCard roleCard);

    List<UserRoleCard> listByUserId(String userId);

    Optional<UserRoleCard> findByUserIdAndRoleId(String userId, String roleId);
}
