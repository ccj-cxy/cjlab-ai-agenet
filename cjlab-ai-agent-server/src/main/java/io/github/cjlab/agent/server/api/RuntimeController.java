package io.github.cjlab.agent.server.api;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;
import java.util.List;

@RestController
@RequestMapping("/api/runtime")
public class RuntimeController {

    private final String runtime;
    private final Environment environment;

    public RuntimeController(
            @Value("${cjlab.agent.runtime:local-dag}") String runtime,
            Environment environment
    ) {
        this.runtime = runtime;
        this.environment = environment;
    }

    @GetMapping
    public RuntimeInfo runtime() {
        return new RuntimeInfo(
                runtime,
                Arrays.asList(environment.getActiveProfiles()),
                environment.getProperty("spring.ai.model.chat", "none")
        );
    }

    public record RuntimeInfo(
            String runtime,
            List<String> activeProfiles,
            String chatModel
    ) {
    }
}
