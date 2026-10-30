package net.technicpack.minecraftcore.launch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import org.junit.jupiter.api.Test;

class WindowsWindowMaximizerLauncherTest {
  private static final String MODE = "--technic-window-maximizer";

  @Test
  void ordinaryLauncherArgumentsAreNotConsumed() {
    assertFalse(WindowsWindowMaximizerLauncher.runIfRequested(new String[0]));
    assertFalse(WindowsWindowMaximizerLauncher.runIfRequested(new String[] {"--help"}));
  }

  @Test
  void malformedHelperArgumentsReturnBeforeNormalStartup() throws Exception {
    String[][] invalid = {
      {MODE},
      {MODE, "1", "2"},
      {MODE, "1", "2", "true", "extra"},
      {MODE, "0", "2", "true"},
      {MODE, "-1", "2", "true"},
      {MODE, "4294967296", "2", "true"},
      {MODE, "not-a-pid", "2", "true"},
      {MODE, "1", "0", "true"},
      {MODE, "1", "-1", "true"},
      {MODE, "1", "9223372036854775808", "true"},
      {MODE, "1", "2", "yes"}
    };
    PrintStream originalError = System.err;
    ByteArrayOutputStream errors = new ByteArrayOutputStream();
    try (PrintStream error = new PrintStream(errors, true, "UTF-8")) {
      System.setErr(error);
      for (String[] args : invalid) {
        assertTrue(WindowsWindowMaximizerLauncher.runIfRequested(args));
      }
    } finally {
      System.setErr(originalError);
    }
  }

  @Test
  void successfulHandoffClosesStreamsWithoutDestroyingHelper() throws Exception {
    HelperProcess helper = new HelperProcess("READY\n", true);
    WindowsWindowMaximizerLauncher.attach(helper, 1000);
    assertFalse(helper.destroyed);
    assertTrue(helper.isAlive());
    helper.assertStreamsClosed();
  }

  @Test
  void bufferedHandshakeIsAcceptedEvenWhenHelperAlreadyFinished() throws Exception {
    HelperProcess helper = new HelperProcess("READY\n", false);
    WindowsWindowMaximizerLauncher.attach(helper, 1000);
    assertFalse(helper.destroyed);
    helper.assertStreamsClosed();
  }

  @Test
  void malformedHandshakeDestroysHelperAndClosesStreams() {
    HelperProcess helper = new HelperProcess("WRONG\n", true);
    assertThrows(IOException.class, () -> WindowsWindowMaximizerLauncher.attach(helper, 1000));
    assertTrue(helper.destroyed);
    helper.assertStreamsClosed();
  }

  @Test
  void earlyHelperExitClosesStreams() {
    HelperProcess helper = new HelperProcess("REA", false);
    assertThrows(IOException.class, () -> WindowsWindowMaximizerLauncher.attach(helper, 1000));
    assertTrue(helper.destroyed);
    helper.assertStreamsClosed();
  }

  @Test
  void handshakeDeadlineDestroysHelperWithoutAReaderThread() {
    HelperProcess helper = new HelperProcess("", true);
    assertThrows(IOException.class, () -> WindowsWindowMaximizerLauncher.attach(helper, 0));
    assertTrue(helper.destroyed);
    helper.assertStreamsClosed();
  }

  @Test
  void interruptionDestroysHelperAndClosesStreams() {
    HelperProcess helper = new HelperProcess("", true);
    Thread.currentThread().interrupt();
    try {
      assertThrows(
          InterruptedException.class, () -> WindowsWindowMaximizerLauncher.attach(helper, 1000));
    } finally {
      Thread.interrupted();
    }
    assertTrue(helper.destroyed);
    helper.assertStreamsClosed();
  }

  private static final class HelperProcess extends Process {
    private final TrackedInputStream stdout;
    private final TrackedInputStream stderr = new TrackedInputStream("");
    private final TrackedOutputStream stdin = new TrackedOutputStream();
    private boolean alive;
    private boolean destroyed;

    private HelperProcess(String output, boolean alive) {
      stdout = new TrackedInputStream(output);
      this.alive = alive;
    }

    @Override
    public OutputStream getOutputStream() {
      return stdin;
    }

    @Override
    public InputStream getInputStream() {
      return stdout;
    }

    @Override
    public InputStream getErrorStream() {
      return stderr;
    }

    @Override
    public int waitFor() {
      throw new AssertionError("Handshake must not wait for the detached helper to finish");
    }

    @Override
    public int exitValue() {
      if (alive) {
        throw new IllegalThreadStateException();
      }
      return 0;
    }

    @Override
    public boolean isAlive() {
      return alive;
    }

    @Override
    public void destroy() {
      destroyed = true;
      alive = false;
    }

    @Override
    public Process destroyForcibly() {
      destroy();
      return this;
    }

    private void assertStreamsClosed() {
      assertTrue(stdin.closed);
      assertTrue(stdout.closed);
      assertTrue(stderr.closed);
    }
  }

  private static final class TrackedInputStream extends ByteArrayInputStream {
    private boolean closed;

    private TrackedInputStream(String contents) {
      super(contents.getBytes(StandardCharsets.US_ASCII));
    }

    @Override
    public void close() {
      closed = true;
    }
  }

  private static final class TrackedOutputStream extends ByteArrayOutputStream {
    private boolean closed;

    @Override
    public void close() {
      closed = true;
    }
  }
}
