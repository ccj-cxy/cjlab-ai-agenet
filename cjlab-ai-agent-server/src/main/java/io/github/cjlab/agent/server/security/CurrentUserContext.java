package io.github.cjlab.agent.server.security;

import io.github.cjlab.agent.common.AgentException;

public final class CurrentUserContext {

    private static final ThreadLocal<CurrentUser> CURRENT_USER = new ThreadLocal<>();

    private CurrentUserContext() {
    }

    public static void set(CurrentUser user) {
        CURRENT_USER.set(user);
    }

    public static CurrentUser required() {
        CurrentUser user = CURRENT_USER.get();
        if (user == null) {
            throw new AgentException("Current user is not available.");
        }
        return user;
    }

    public static void clear() {
        CURRENT_USER.remove();
    }
}
