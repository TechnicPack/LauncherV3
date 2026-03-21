package net.technicpack.launchercore.progress;

import net.technicpack.launchercore.util.DownloadListener;

public final class ExecutionProgressListeners {
    private ExecutionProgressListeners() {
    }

    public static ExecutionProgressListener adapt(DownloadListener listener) {
        if (listener instanceof ExecutionProgressListener) {
            return (ExecutionProgressListener) listener;
        }

        return new ExecutionProgressListener() {
            @Override
            public void overallChanged(String label, float percent) {
                if (listener != null) {
                    listener.stateChanged(label, percent);
                }
            }

            @Override
            public void currentItemChanged(String label, CurrentItemMode mode, Float percent) {
            }
        };
    }
}
