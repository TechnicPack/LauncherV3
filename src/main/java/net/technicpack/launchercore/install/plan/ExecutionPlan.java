package net.technicpack.launchercore.install.plan;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public class ExecutionPlan<TContext> {
    private final Map<String, ExecutionPhase> phases;
    private final Map<String, PlanNode<TContext>> nodes;
    private final float totalWeight;

    ExecutionPlan(Map<String, ExecutionPhase> phases, Map<String, PlanNode<TContext>> nodes, float totalWeight) {
        this.phases = Collections.unmodifiableMap(new LinkedHashMap<>(phases));
        this.nodes = Collections.unmodifiableMap(new LinkedHashMap<>(nodes));
        this.totalWeight = totalWeight;
    }

    public Collection<ExecutionPhase> getPhases() {
        return phases.values();
    }

    public Collection<PlanNode<TContext>> getNodes() {
        return nodes.values();
    }

    public float getTotalWeight() {
        return totalWeight;
    }
}
