package net.technicpack.launchercore.install.plan;

import java.io.IOException;
import java.util.function.Function;
import net.technicpack.launchercore.install.InstallTasksQueue;
import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.launchercore.install.tasks.ListenerTask;
import net.technicpack.launchercore.progress.CurrentItemMode;

public class LegacyTaskPlanAction<TContext, TMetadata> implements PlanNodeAction<TContext> {
  private final IInstallTask<TMetadata> task;
  private final Function<TContext, TMetadata> metadataExtractor;

  public LegacyTaskPlanAction(
      IInstallTask<TMetadata> task, Function<TContext, TMetadata> metadataExtractor) {
    this.task = task;
    this.metadataExtractor = metadataExtractor;
  }

  @Override
  @SuppressWarnings("unchecked")
  public void execute(TContext context, NodeProgressReporter reporter)
      throws IOException, InterruptedException {
    InstallTasksQueue<TMetadata> queue = new InstallTasksQueue<>(null);
    queue.setMetadata(metadataExtractor.apply(context));
    queue.beginTaskExecution(task);

    if (task instanceof ListenerTask) {
      ((ListenerTask<TMetadata>) task).setProgressReporter(reporter);
    }

    reporter.updateCurrentItem(task.getTaskDescription(), CurrentItemMode.INDETERMINATE, null);
    task.runTask(queue);
    reporter.updateNodeProgress(100.0f);
  }
}
