package io.github.cjlab.agent.core.dag;

import java.util.Map;
import java.util.Set;

public record DagNode(
        String id,
        Set<String> dependsOn,
        DagNodeHandler handler,
        Map<String, Object> parameters
) {
}
