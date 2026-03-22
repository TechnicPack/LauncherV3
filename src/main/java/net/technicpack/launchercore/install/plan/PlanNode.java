package net.technicpack.launchercore.install.plan;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public class PlanNode<TContext> {
  private final String id;
  private final ExecutionPhase phase;
  private final String description;
  private final float weight;
  private final List<String> dependencies;
  private final PlanNodeAction<TContext> action;

  PlanNode(
      String id,
      ExecutionPhase phase,
      String description,
      float weight,
      Collection<String> dependencies,
      PlanNodeAction<TContext> action) {
    this.id = Objects.requireNonNull(id, "Node id must not be null");
    this.phase = Objects.requireNonNull(phase, "Node phase must not be null");
    this.description = Objects.requireNonNull(description, "Node description must not be null");
    this.weight = weight;
    this.dependencies = Collections.unmodifiableList(new ArrayList<>(dependencies));
    this.action = Objects.requireNonNull(action, "Node action must not be null");
  }

  public String getId() {
    return id;
  }

  public ExecutionPhase getPhase() {
    return phase;
  }

  public String getDescription() {
    return description;
  }

  public float getWeight() {
    return weight;
  }

  public List<String> getDependencies() {
    return dependencies;
  }

  PlanNodeAction<TContext> getAction() {
    return action;
  }
}
