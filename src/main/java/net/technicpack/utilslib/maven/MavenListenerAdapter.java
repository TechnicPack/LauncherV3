package net.technicpack.utilslib.maven;

import net.technicpack.launchercore.util.DownloadListener;
import net.technicpack.rest.io.Resource;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;
import org.eclipse.aether.transfer.TransferResource;

import java.util.HashMap;
import java.util.Map;

public class MavenListenerAdapter implements TransferListener {
    private DownloadListener listener;
    private long total = 0;
    private Map<String, Long> completedAmounts = new HashMap<String, Long>();

    public MavenListenerAdapter(DownloadListener listener) {
        this.listener = listener;
    }

    @Override
    public void transferInitiated(TransferEvent transferEvent) throws TransferCancelledException {
    }

    @Override
    public void transferStarted(TransferEvent transferEvent) throws TransferCancelledException {
        completedAmounts.put(transferEvent.getResource().getResourceName(), 0L);
        total += transferEvent.getResource().getContentLength();
        updateValue(transferEvent.getResource(), transferEvent.getTransferredBytes());
    }

    @Override
    public void transferProgressed(TransferEvent transferEvent) throws TransferCancelledException {
        updateValue(transferEvent.getResource(), transferEvent.getTransferredBytes());
    }

    @Override
    public void transferCorrupted(TransferEvent transferEvent) throws TransferCancelledException {
    }

    @Override
    public void transferSucceeded(TransferEvent transferEvent) {
        updateValue(transferEvent.getResource(), transferEvent.getResource().getContentLength());
    }

    @Override
    public void transferFailed(TransferEvent transferEvent) {

    }

    private void updateValue(TransferResource resource, long bytes) {
        completedAmounts.put(resource.getResourceName(), bytes);

        long completed = 0;
        for (Long value : completedAmounts.values()) {
            completed += value;
        }

        float total = this.total;
        float progress = completed;
        float percent = (100.0f*progress)/total;
        listener.stateChanged("", percent);
    }
}
