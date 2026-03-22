package net.technicpack.launchercore.install.plan;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.technicpack.launchercore.progress.CurrentItemMode;
import net.technicpack.launchercore.progress.ExecutionProgressListener;

public class PlanExecutor<TContext> {
  private final ExecutionProgressListener listener;
  private final Comparator<PlanNode<TContext>> readyNodeComparator;

  public PlanExecutor(ExecutionProgressListener listener) {
    this(
        listener,
        Comparator.comparingInt((PlanNode<TContext> node) -> node.getPhase().getOrder())
            .thenComparing(PlanNode::getId));
  }

  public PlanExecutor(
      ExecutionProgressListener listener, Comparator<PlanNode<TContext>> readyNodeComparator) {
    this.listener = listener;
    this.readyNodeComparator = readyNodeComparator;
  }

  public void execute(ExecutionPlan<TContext> plan, TContext context)
      throws IOException, InterruptedException {
    Map<String, Integer> indegree = new HashMap<>();
    Map<String, List<String>> dependents = new HashMap<>();
    Map<String, PlanNode<TContext>> nodesById = new HashMap<>();
    List<PlanNode<TContext>> ready = new ArrayList<>();

    for (PlanNode<TContext> node : plan.getNodes()) {
      nodesById.put(node.getId(), node);
      indegree.put(node.getId(), node.getDependencies().size());
      if (node.getDependencies().isEmpty()) {
        ready.add(node);
      }
      for (String dependency : node.getDependencies()) {
        dependents.computeIfAbsent(dependency, ignored -> new ArrayList<>()).add(node.getId());
      }
    }

    float completedWeight = 0.0f;
    int processed = 0;

    while (!ready.isEmpty()) {
      if (Thread.currentThread().isInterrupted()) {
        throw new InterruptedException();
      }

      ready.sort(readyNodeComparator);
      PlanNode<TContext> node = ready.remove(0);
      processed++;

      NodeState state = new NodeState(node, completedWeight, plan.getTotalWeight());
      if (listener != null) {
        listener.overallChanged(node.getPhase().getLabel(), state.getOverallPercent());
        listener.currentItemChanged(node.getDescription(), CurrentItemMode.INDETERMINATE, null);
      }

      node.getAction().execute(context, state);

      if (Thread.currentThread().isInterrupted()) {
        throw new InterruptedException();
      }

      completedWeight += node.getWeight();
      state.complete(completedWeight, processed == nodesById.size());

      for (String dependentId :
          dependents.getOrDefault(node.getId(), Collections.<String>emptyList())) {
        int updated = indegree.get(dependentId) - 1;
        indegree.put(dependentId, updated);
        if (updated == 0) {
          ready.add(nodesById.get(dependentId));
        }
      }
    }

    if (processed != nodesById.size()) {
      throw new IllegalStateException("Execution plan stalled before all nodes ran");
    }
  }

  private class NodeState implements NodeProgressReporter {
    private final PlanNode<TContext> node;
    private final float completedWeightAtStart;
    private final float totalWeight;
    private float nodeProgress = 0.0f;

    private NodeState(PlanNode<TContext> node, float completedWeightAtStart, float totalWeight) {
      this.node = node;
      this.completedWeightAtStart = completedWeightAtStart;
      this.totalWeight = totalWeight;
    }

    @Override
    public void updateNodeProgress(float percent) {
      nodeProgress = clamp(percent);
      if (listener != null) {
        listener.overallChanged(node.getPhase().getLabel(), getOverallPercent());
      }
    }

    @Override
    public void updateCurrentItem(String label, CurrentItemMode mode, Float percent) {
      if (listener != null) {
        listener.currentItemChanged(label, mode, percent);
      }
    }

    private float getOverallPercent() {
      if (totalWeight == 0.0f) {
        return 100.0f;
      }

      float progress = completedWeightAtStart + (node.getWeight() * (nodeProgress / 100.0f));
      return (progress / totalWeight) * 100.0f;
    }

    private void complete(float completedWeight, boolean isLastNode) {
      nodeProgress = 100.0f;
      if (listener != null) {
        listener.overallChanged(
            node.getPhase().getLabel(),
            totalWeight == 0.0f ? 100.0f : (completedWeight / totalWeight) * 100.0f);
        listener.currentItemChanged("", CurrentItemMode.IDLE, null);
      }
    }

    private float clamp(float percent) {
      if (percent < 0.0f) {
        return 0.0f;
      }
      if (percent > 100.0f) {
        return 100.0f;
      }
      return percent;
    }
  }
}
