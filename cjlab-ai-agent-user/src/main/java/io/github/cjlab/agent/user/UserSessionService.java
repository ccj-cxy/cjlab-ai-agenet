package io.github.cjlab.agent.user;

import java.time.Duration;
import java.util.Optional;

public interface UserSessionService {

    UserSession create(String userId);

    Optional<UserSession> findByToken(String token);

    Duration ttl();
}
