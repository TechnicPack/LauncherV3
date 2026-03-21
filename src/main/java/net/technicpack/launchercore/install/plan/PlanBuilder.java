package net.technicpack.launchercore.install.plan;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Queue;
import java.util.function.Consumer;

public class PlanBuilder<TContext> {
    private final Map<String, ExecutionPhase> phases = new LinkedHashMap<>();
    private final Map<String, PlanNode<TContext>> nodes = new LinkedHashMap<>();

    public PlanBuilder<TContext> addPhase(String id, String label) {
        Objects.requireNonNull(id, "Phase id must not be null");
        Objects.requireNonNull(label, "Phase label must not be null");

        if (phases.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate phase id: " + id);
        }

        phases.put(id, new ExecutionPhase(id, label, phases.size()));
        return this;
    }

    public PlanBuilder<TContext> addNode(String id, String phaseId, String description, float weight, Consumer<TContext> action) {
        return addNode(id, phaseId, description, weight, Collections.<String>emptyList(), wrap(action));
    }

    public PlanBuilder<TContext> addNode(String id, String phaseId, String description, float weight,
                                         Collection<String> dependencies, Consumer<TContext> action) {
        return addNode(id, phaseId, description, weight, dependencies, wrap(action));
    }

    public PlanBuilder<TContext> addNode(String id, String phaseId, String description, float weight, PlanNodeAction<TContext> action) {
        return addNode(id, phaseId, description, weight, Collections.<String>emptyList(), action);
    }

    public PlanBuilder<TContext> addNode(String id, String phaseId, String description, float weight,
                                         Collection<String> dependencies, PlanNodeAction<TContext> action) {
        Objects.requireNonNull(id, "Node id must not be null");

        if (nodes.containsKey(id)) {
            throw new IllegalArgumentException("Duplicate node id: " + id);
        }

        ExecutionPhase phase = phases.get(phaseId);
        if (phase == null) {
            throw new IllegalArgumentException("Unknown phase id: " + phaseId);
        }

        nodes.put(id, new PlanNode<>(id, phase, description, weight, dependencies, action));
        return this;
    }

    public ExecutionPlan<TContext> build() {
        validateDependenciesExist();
        validateAcyclic();

        float totalWeight = 0.0f;
        for (PlanNode<TContext> node : nodes.values()) {
            totalWeight += node.getWeight();
        }

        return new ExecutionPlan<>(phases, nodes, totalWeight);
    }

    private void validateDependenciesExist() {
        for (PlanNode<TContext> node : nodes.values()) {
            for (String dependency : node.getDependencies()) {
                if (!nodes.containsKey(dependency)) {
                    throw new IllegalArgumentException(String.format("Node %s depends on unknown node %s", node.getId(), dependency));
                }
            }
        }
    }

    private void validateAcyclic() {
        Map<String, Integer> indegree = new HashMap<>();
        Map<String, List<String>> dependents = new HashMap<>();

        for (PlanNode<TContext> node : nodes.values()) {
            indegree.put(node.getId(), node.getDependencies().size());
            for (String dependency : node.getDependencies()) {
                dependents.computeIfAbsent(dependency, ignored -> new ArrayList<>()).add(node.getId());
            }
        }

        Queue<String> ready = new ArrayDeque<>();
        for (Map.Entry<String, Integer> entry : indegree.entrySet()) {
            if (entry.getValue() == 0) {
                ready.add(entry.getKey());
            }
        }

        int visited = 0;
        while (!ready.isEmpty()) {
            String nodeId = ready.remove();
            visited++;

            for (String dependent : dependents.getOrDefault(nodeId, Collections.<String>emptyList())) {
                int updated = indegree.get(dependent) - 1;
                indegree.put(dependent, updated);
                if (updated == 0) {
                    ready.add(dependent);
                }
            }
        }

        if (visited != nodes.size()) {
            throw new IllegalArgumentException("Execution plan contains a dependency cycle");
        }
    }

    private static <TContext> PlanNodeAction<TContext> wrap(Consumer<TContext> action) {
        return new PlanNodeAction<TContext>() {
            @Override
            public void execute(TContext context, NodeProgressReporter reporter) throws IOException {
                action.accept(context);
            }
        };
    }
}
