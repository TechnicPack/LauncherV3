package net.technicpack.launchercore.install.plan;

import java.util.Objects;

public class ExecutionPhase {
  private final String id;
  private final String label;
  private final int order;

  ExecutionPhase(String id, String label, int order) {
    this.id = Objects.requireNonNull(id, "Phase id must not be null");
    this.label = Objects.requireNonNull(label, "Phase label must not be null");
    this.order = order;
  }

  public String getId() {
    return id;
  }

  public String getLabel() {
    return label;
  }

  public int getOrder() {
    return order;
  }
}
