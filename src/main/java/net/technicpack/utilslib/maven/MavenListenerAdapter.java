package net.technicpack.utilslib.maven;

import net.technicpack.launchercore.util.DownloadListener;
import org.eclipse.aether.transfer.TransferCancelledException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferListener;

public class MavenListenerAdapter implements TransferListener {
    private DownloadListener listener;

    public MavenListenerAdapter(DownloadListener listener) {
        this.listener = listener;
    }

    @Override
    public void transferInitiated(TransferEvent transferEvent) throws TransferCancelledException {

    }

    @Override
    public void transferStarted(TransferEvent transferEvent) throws TransferCancelledException {
        listener.stateChanged("", 0);
    }

    @Override
    public void transferProgressed(TransferEvent transferEvent) throws TransferCancelledException {
        float total = transferEvent.getResource().getContentLength();
        float progress = transferEvent.getTransferredBytes();
        float percent = (100.0f*progress)/total;
        listener.stateChanged("", percent);
    }

    @Override
    public void transferCorrupted(TransferEvent transferEvent) throws TransferCancelledException {
    }

    @Override
    public void transferSucceeded(TransferEvent transferEvent) {
        listener.stateChanged("", 100.0f);
    }

    @Override
    public void transferFailed(TransferEvent transferEvent) {

    }
}
