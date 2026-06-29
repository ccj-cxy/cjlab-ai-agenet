package io.github.cjlab.agent.server.config;

import io.github.cjlab.agent.server.security.AuthInterceptor;
import io.github.cjlab.agent.user.UserService;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebSecurityConfiguration implements WebMvcConfigurer {

    private final UserService userService;

    public WebSecurityConfiguration(UserService userService) {
        this.userService = userService;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new AuthInterceptor(userService))
                .addPathPatterns(
                        "/api/chat/**",
                        "/api/memory/**",
                        "/api/knowledge/**",
                        "/api/tools/**",
                        "/api/users/me"
                );
    }
}
