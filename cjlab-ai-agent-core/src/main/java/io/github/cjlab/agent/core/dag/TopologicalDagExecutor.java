package io.github.cjlab.agent.core.dag;

import io.github.cjlab.agent.common.AgentException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TopologicalDagExecutor implements DagExecutor {

    @Override
    public DagExecutionContext execute(Collection<DagNode> nodes, DagExecutionContext context) {
        Map<String, DagNode> remaining = new LinkedHashMap<>();
        for (DagNode node : nodes) {
            if (remaining.put(node.id(), node) != null) {
                throw new AgentException("Duplicate DAG node id: " + node.id());
            }
        }

        Set<String> completed = new HashSet<>();
        while (!remaining.isEmpty()) {
            List<DagNode> readyNodes = readyNodes(remaining, completed);
            if (readyNodes.isEmpty()) {
                throw new AgentException("DAG contains a cycle or unresolved dependency.");
            }
            for (DagNode node : readyNodes) {
                Object result = node.handler().handle(context);
                context.putResult(node.id(), result);
                completed.add(node.id());
                remaining.remove(node.id());
            }
        }
        return context;
    }

    private List<DagNode> readyNodes(Map<String, DagNode> remaining, Set<String> completed) {
        List<DagNode> ready = new ArrayList<>();
        for (DagNode node : remaining.values()) {
            if (completed.containsAll(node.dependsOn())) {
                ready.add(node);
            }
        }
        return ready;
    }
}
