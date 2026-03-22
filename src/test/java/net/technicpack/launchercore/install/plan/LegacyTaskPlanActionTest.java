package net.technicpack.launchercore.install.plan;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class LegacyTaskPlanActionTest {
  @Test
  void recordingQueueRespectsFrontAndBackInsertionOrder() {
    RecordingTasksQueue<Void> queue = new RecordingTasksQueue<>();

    queue.addTask(new NamedTask("back"));
    queue.addNextTask(new NamedTask("front"));

    assertEquals(
        Arrays.asList("front", "back"),
        queue.getTasks().stream()
            .map(task -> ((NamedTask) task).getName())
            .collect(Collectors.toList()));
  }

  @Test
  void legacyTaskActionBridgesListenerTaskProgressIntoNodeReporter() throws Exception {
    TestListenerTask task = new TestListenerTask();
    RecordingReporter reporter = new RecordingReporter();

    new LegacyTaskPlanAction<Void, Void>(task, context -> null).execute(null, reporter);

    assertEquals(
        Arrays.asList(
            "item:test-step:INDETERMINATE:null",
            "item:download.bin:DETERMINATE:50.00",
            "node:50.00",
            "node:100.00"),
        reporter.events);
  }

  @Test
  void legacyTaskActionRejectsRuntimeQueueMutation() {
    assertThrows(
        IllegalStateException.class,
        () ->
            new LegacyTaskPlanAction<Void, Void>(new MutatingTask(), context -> null)
                .execute(null, new RecordingReporter()));
  }

  private static class NamedTask
      extends net.technicpack.launchercore.install.tasks.CopyFileTask<Void> {
    private final String name;

    private NamedTask(String name) {
      super(new java.io.File(name), new java.io.File(name));
      this.name = name;
    }

    private String getName() {
      return name;
    }
  }

  private static class TestListenerTask
      extends net.technicpack.launchercore.install.tasks.ListenerTask<Void> {
    @Override
    public String getTaskDescription() {
      return "test-step";
    }

    @Override
    public void runTask(net.technicpack.launchercore.install.InstallTasksQueue<Void> queue) {
      super.setQueue(queue);
      stateChanged("download.bin", 50.0f);
    }
  }

  private static class MutatingTask
      implements net.technicpack.launchercore.install.tasks.IInstallTask<Void> {
    @Override
    public String getTaskDescription() {
      return "mutating";
    }

    @Override
    public float getTaskProgress() {
      return 0;
    }

    @Override
    public void runTask(net.technicpack.launchercore.install.InstallTasksQueue<Void> queue) {
      queue.addTask(new NamedTask("illegal"));
    }
  }

  private static class RecordingReporter implements NodeProgressReporter {
    private final List<String> events = new ArrayList<>();

    @Override
    public void updateNodeProgress(float percent) {
      events.add(String.format("node:%.2f", percent));
    }

    @Override
    public void updateCurrentItem(
        String label, net.technicpack.launchercore.progress.CurrentItemMode mode, Float percent) {
      events.add(
          String.format(
              "item:%s:%s:%s",
              label, mode, percent == null ? "null" : String.format("%.2f", percent)));
    }
  }
}
