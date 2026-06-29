package io.github.cjlab.agent.core.dag;

import java.util.Collection;
import java.util.Map;

public interface DagExecutor {

    DagExecutionContext execute(Collection<DagNode> nodes, DagExecutionContext context);
}
