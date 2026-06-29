package io.github.cjlab.agent.server.api;

import io.github.cjlab.agent.user.LoginRequest;
import io.github.cjlab.agent.user.LoginResponse;
import io.github.cjlab.agent.user.RegisterUserRequest;
import io.github.cjlab.agent.user.UserProfileResponse;
import io.github.cjlab.agent.user.UserService;
import org.springframework.http.HttpHeaders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public UserProfileResponse register(@RequestBody RegisterUserRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public LoginResponse login(@RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @GetMapping("/me")
    public UserProfileResponse me(@RequestHeader(HttpHeaders.AUTHORIZATION) String authorization) {
        return userService.currentUser(bearerToken(authorization));
    }

    private String bearerToken(String authorization) {
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            return "";
        }
        return authorization.substring("Bearer ".length()).trim();
    }
}
