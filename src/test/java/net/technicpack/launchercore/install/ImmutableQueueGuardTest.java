package net.technicpack.launchercore.install;

import net.technicpack.launchercore.install.tasks.IInstallTask;
import net.technicpack.launchercore.install.tasks.ParallelTaskGroup;
import net.technicpack.launchercore.install.tasks.TaskGroup;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ImmutableQueueGuardTest {
    @Test
    void installTasksQueueRejectsMutationOnceExecutionStarts() {
        InstallTasksQueue<Void> queue = new InstallTasksQueue<>(null);

        queue.beginTaskExecution(new NoOpTask("running"));

        assertThrows(IllegalStateException.class, () -> queue.addTask(new NoOpTask("later")));
        assertThrows(IllegalStateException.class, () -> queue.addNextTask(new NoOpTask("sooner")));
    }

    @Test
    void taskGroupRejectsMutationOnceExecutionStarts() throws Exception {
        TaskGroup<Void> group = new TaskGroup<>("group");
        group.addTask(new NoOpTask("first"));

        group.runTask(new InstallTasksQueue<Void>(null));

        assertThrows(IllegalStateException.class, () -> group.addTask(new NoOpTask("later")));
        assertThrows(IllegalStateException.class, () -> group.addNextTask(new NoOpTask("sooner")));
    }

    @Test
    void parallelTaskGroupRejectsMutationOnceExecutionStarts() throws Exception {
        ParallelTaskGroup<Void> group = new ParallelTaskGroup<>("group");
        group.addTask(new NoOpTask("first"));

        group.runTask(new InstallTasksQueue<Void>(null));

        assertThrows(IllegalStateException.class, () -> group.addTask(new NoOpTask("later")));
        assertThrows(IllegalStateException.class, () -> group.addNextTask(new NoOpTask("sooner")));
    }

    private static class NoOpTask implements IInstallTask<Void> {
        private final String description;

        private NoOpTask(String description) {
            this.description = description;
        }

        @Override
        public String getTaskDescription() {
            return description;
        }

        @Override
        public float getTaskProgress() {
            return 0;
        }

        @Override
        public void runTask(InstallTasksQueue<Void> queue) throws IOException {
        }
    }
}
