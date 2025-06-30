package net.technicpack.launchercore.mirror.download;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.ReadableByteChannel;

public class CountingReadableByteChannel implements ReadableByteChannel {
    private final ReadableByteChannel wrapped;
    private long bytesRead = 0;

    public CountingReadableByteChannel(ReadableByteChannel wrapped) {
        this.wrapped = wrapped;
    }

    @Override
    public int read(ByteBuffer dst) throws IOException {
        int n = wrapped.read(dst);
        if (n > 0) {
            bytesRead += n;
        }
        return n;
    }

    @Override
    public boolean isOpen() {
        return wrapped.isOpen();
    }

    @Override
    public void close() throws IOException {
        wrapped.close();
    }

    public long getBytesRead() {
        return bytesRead;
    }
}
