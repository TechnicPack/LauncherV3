package net.technicpack.launchercore.install.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.technicpack.launchercore.progress.CurrentItemMode;
import net.technicpack.launchercore.progress.ExecutionProgressListener;
import org.junit.jupiter.api.Test;

class ExecutionPlanTest {
  @Test
  void buildRejectsDuplicateNodeIds() {
    PlanBuilder<List<String>> builder = new PlanBuilder<>();

    builder.addPhase("phase", "Phase");
    builder.addNode("one", "phase", "First", 1.0f, context -> context.add("first"));

    IllegalArgumentException exception =
        assertThrows(
            IllegalArgumentException.class,
            () ->
                builder.addNode("one", "phase", "Second", 1.0f, context -> context.add("second")));

    assertEquals("Duplicate node id: one", exception.getMessage());
  }

  @Test
  void buildRejectsMissingDependencies() {
    PlanBuilder<List<String>> builder = new PlanBuilder<>();

    builder.addPhase("phase", "Phase");
    builder.addNode(
        "one",
        "phase",
        "First",
        1.0f,
        Collections.singletonList("missing"),
        context -> context.add("first"));

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, builder::build);

    assertEquals("Node one depends on unknown node missing", exception.getMessage());
  }

  @Test
  void buildRejectsCycles() {
    PlanBuilder<List<String>> builder = new PlanBuilder<>();

    builder.addPhase("phase", "Phase");
    builder.addNode(
        "one",
        "phase",
        "First",
        1.0f,
        Collections.singletonList("two"),
        context -> context.add("first"));
    builder.addNode(
        "two",
        "phase",
        "Second",
        1.0f,
        Collections.singletonList("one"),
        context -> context.add("second"));

    IllegalArgumentException exception =
        assertThrows(IllegalArgumentException.class, builder::build);

    assertEquals("Execution plan contains a dependency cycle", exception.getMessage());
  }

  @Test
  void executesNodesInDependencyOrderAndReportsOverallAndCurrentItemProgress() throws Exception {
    PlanBuilder<List<String>> builder = new PlanBuilder<>();
    RecordingListener listener = new RecordingListener();

    builder.addPhase("discovery", "Discovery");
    builder.addPhase("install", "Install");
    builder.addNode("prepare", "discovery", "Prepare", 1.0f, context -> context.add("prepare"));
    builder.addNode(
        "download",
        "install",
        "Download",
        3.0f,
        Collections.singletonList("prepare"),
        (context, reporter) -> {
          context.add("download");
          reporter.updateCurrentItem("example.zip", CurrentItemMode.DETERMINATE, 25.0f);
          reporter.updateNodeProgress(25.0f);
          reporter.updateCurrentItem("example.zip", CurrentItemMode.DETERMINATE, 100.0f);
        });
    builder.addNode(
        "extract",
        "install",
        "Extract",
        1.0f,
        Collections.singletonList("download"),
        (context, reporter) -> {
          context.add("extract");
          reporter.updateCurrentItem("Extracting metadata", CurrentItemMode.INDETERMINATE, null);
        });

    ExecutionPlan<List<String>> plan = builder.build();
    new PlanExecutor<List<String>>(listener).execute(plan, new ArrayList<String>());

    assertEquals(Arrays.asList("prepare", "download", "extract"), listener.executedSteps);
    assertEquals(
        Arrays.asList(
            "overall:Discovery:0.00",
            "item:Prepare:INDETERMINATE:null",
            "overall:Discovery:20.00",
            "item::IDLE:null",
            "overall:Install:20.00",
            "item:Download:INDETERMINATE:null",
            "item:example.zip:DETERMINATE:25.00",
            "overall:Install:35.00",
            "item:example.zip:DETERMINATE:100.00",
            "overall:Install:80.00",
            "item::IDLE:null",
            "overall:Install:80.00",
            "item:Extract:INDETERMINATE:null",
            "item:Extracting metadata:INDETERMINATE:null",
            "overall:Install:100.00",
            "item::IDLE:null"),
        listener.events);
  }

  @Test
  void stopsExecutingWhenThreadIsInterruptedBetweenNodes() {
    PlanBuilder<List<String>> builder = new PlanBuilder<>();
    builder.addPhase("install", "Install");
    builder.addNode(
        "first",
        "install",
        "First",
        1.0f,
        context -> {
          context.add("first");
          Thread.currentThread().interrupt();
        });
    builder.addNode("second", "install", "Second", 1.0f, context -> context.add("second"));

    ExecutionPlan<List<String>> plan = builder.build();
    List<String> executed = new ArrayList<>();

    try {
      assertThrows(
          InterruptedException.class,
          () -> new PlanExecutor<List<String>>(null).execute(plan, executed));
      assertEquals(Collections.singletonList("first"), executed);
    } finally {
      Thread.interrupted();
    }
  }

  private static class RecordingListener implements ExecutionProgressListener {
    private final List<String> events = new ArrayList<>();
    private final List<String> executedSteps = new ArrayList<>();

    @Override
    public void overallChanged(String label, float percent) {
      events.add(String.format("overall:%s:%.2f", label, percent));

      if ("Discovery".equals(label) && percent == 0.0f) {
        executedSteps.add("prepare");
      } else if ("Install".equals(label) && percent == 20.0f) {
        executedSteps.add("download");
      } else if ("Install".equals(label) && percent == 80.0f && executedSteps.size() == 2) {
        executedSteps.add("extract");
      }
    }

    @Override
    public void currentItemChanged(String label, CurrentItemMode mode, Float percent) {
      events.add(
          String.format(
              "item:%s:%s:%s",
              label, mode, percent == null ? "null" : String.format("%.2f", percent)));
    }
  }
}
