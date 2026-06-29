package io.github.cjlab.agent.user;

import io.github.cjlab.agent.common.AgentException;

import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import java.util.regex.Pattern;

public class UserService {

    private static final Pattern EMAIL_PATTERN = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");

    private final UserRepository userRepository;
    private final PasswordHasher passwordHasher;
    private final UserSessionService userSessionService;

    public UserService(
            UserRepository userRepository,
            PasswordHasher passwordHasher,
            UserSessionService userSessionService
    ) {
        this.userRepository = userRepository;
        this.passwordHasher = passwordHasher;
        this.userSessionService = userSessionService;
    }

    public UserProfileResponse register(RegisterUserRequest request) {
        String email = normalizeEmail(request.email());
        validateEmail(email);
        validatePassword(request.password());
        if (userRepository.existsByEmail(email)) {
            throw new AgentException("Email already registered.");
        }

        Instant now = Instant.now();
        UserAccount user = new UserAccount(
                UUID.randomUUID().toString(),
                email,
                resolveDisplayName(request.displayName(), email),
                passwordHasher.hash(request.password()),
                UserStatus.ACTIVE,
                now,
                now
        );
        return UserProfileResponse.from(userRepository.save(user));
    }

    public LoginResponse login(LoginRequest request) {
        String email = normalizeEmail(request.email());
        UserAccount user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AgentException("Invalid email or password."));
        if (user.status() != UserStatus.ACTIVE) {
            throw new AgentException("User account is disabled.");
        }
        if (!passwordHasher.matches(request.password(), user.passwordHash())) {
            throw new AgentException("Invalid email or password.");
        }
        UserSession session = userSessionService.create(user.id());
        return new LoginResponse(session.token(), session.expiresAt(), UserProfileResponse.from(user));
    }

    public UserProfileResponse currentUser(String token) {
        UserSession session = userSessionService.findByToken(token)
                .orElseThrow(() -> new AgentException("Invalid or expired access token."));
        UserAccount user = userRepository.findById(session.userId())
                .orElseThrow(() -> new AgentException("User not found."));
        return UserProfileResponse.from(user);
    }

    private String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }

    private void validateEmail(String email) {
        if (!EMAIL_PATTERN.matcher(email).matches()) {
            throw new AgentException("Email format is invalid.");
        }
    }

    private void validatePassword(String password) {
        if (password == null || password.length() < 8) {
            throw new AgentException("Password must be at least 8 characters.");
        }
    }

    private String resolveDisplayName(String displayName, String email) {
        if (displayName != null && !displayName.isBlank()) {
            return displayName.trim();
        }
        return email.substring(0, email.indexOf('@'));
    }
}
