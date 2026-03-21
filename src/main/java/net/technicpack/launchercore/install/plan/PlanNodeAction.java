package net.technicpack.launchercore.install.plan;

import java.io.IOException;

@FunctionalInterface
public interface PlanNodeAction<TContext> {
    void execute(TContext context, NodeProgressReporter reporter) throws IOException, InterruptedException;
}
