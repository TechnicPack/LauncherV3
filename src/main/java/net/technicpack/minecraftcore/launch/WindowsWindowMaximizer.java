package net.technicpack.minecraftcore.launch;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.technicpack.utilslib.Utils;

/**
 * Maximizes the game's window once it appears. Minecraft has no maximized launch argument, so the
 * launcher watches for a top-level game window that the user could maximize and maximizes it once;
 * later size changes are left to the player.
 */
final class WindowsWindowMaximizer {
  private static final long POLL_INTERVAL_MS = 250;
  private static final int GWL_STYLE = -16;
  private static final int GW_OWNER = 4;
  private static final int SW_MAXIMIZE = 3;
  private static final int WS_THICKFRAME = 0x00040000;
  private static final int WS_MAXIMIZEBOX = 0x00010000;
  private static final int PROCESS_ACCESS =
      0x00100000 | 0x1000; // SYNCHRONIZE | QUERY_LIMITED_INFORMATION
  private static final int TH32CS_SNAPPROCESS = 2;
  private static final int ERROR_NO_MORE_FILES = 18;

  private WindowsWindowMaximizer() {}

  static Target open(Process process) throws ReflectiveOperationException {
    NativeCalls calls = new NativeCalls();
    long pid = getPid(process, calls);
    requirePid(pid);
    try (ProcessTimes times = new ProcessTimes()) {
      Target target = open(calls, pid, times, true);
      // Windows ProcessImpl retains its native handle until finalization (Java 8) or cleaning
      // (Java 9+), not process exit. Keep the Process strongly reachable through OpenProcess and
      // handoff: that original handle prevents PID reuse even for an already-exited wrapper.
      target.sourceProcess = process;
      return target;
    }
  }

  static Target open(long pid, long creationTime) {
    requirePid(pid);
    if (creationTime <= 0) throw new IllegalArgumentException("Invalid process creation time");
    NativeCalls calls = new NativeCalls();
    try (ProcessTimes times = new ProcessTimes()) {
      Target target = open(calls, pid, times, true);
      if (target.creationTime != creationTime) {
        target.close();
        throw new IllegalStateException("The Minecraft process identity has changed");
      }
      return target;
    }
  }

  /** The caller retains the root handle; every descendant handle belongs to this invocation. */
  static void run(Target root, boolean followDescendants) throws InterruptedException {
    if (root.handle == null)
      throw new IllegalStateException("The Minecraft process handle is closed");
    try (ProcessTree tree = new ProcessTree(root, followDescendants);
        WindowPoller windows = new WindowPoller(root.calls)) {
      while (true) {
        if (Thread.currentThread().isInterrupted()) throw new InterruptedException();
        // Snapshot before deciding the tree has exited: a wrapper may already have exited while
        // its game child, whose parent PID still identifies the retained wrapper, remains alive.
        if (!tree.refresh()) return;
        if (windows.poll(tree, System.nanoTime())) return;
        Thread.sleep(POLL_INTERVAL_MS);
      }
    }
  }

  static final class Target implements AutoCloseable {
    private final NativeCalls calls;
    private final long pid;
    private final long creationTime;
    private Pointer handle;
    private Process sourceProcess;
    private long exitTime;
    private boolean alive;

    private Target(NativeCalls calls, long pid, Pointer handle, ProcessTimes times) {
      this.calls = calls;
      this.pid = pid;
      this.handle = handle;
      this.creationTime = times.memory.getLong(0);
      this.alive = true;
    }

    long getPid() {
      return pid;
    }

    long getCreationTime() {
      return creationTime;
    }

    private void refresh(ProcessTimes times) {
      calls.waitArguments[0] = handle;
      int result = calls.waitForSingleObject.invokeInt(calls.waitArguments);
      if (result == 258) { // WAIT_TIMEOUT: the retained process object is still running.
        alive = true;
      } else if (result == 0) {
        alive = false;
        if (exitTime == 0) {
          times.read(calls, handle);
          exitTime = times.memory.getLong(8);
          if (exitTime == 0) throw new IllegalStateException("Missing process exit time");
        }
      } else {
        throw nativeFailure("WaitForSingleObject");
      }
    }

    @Override
    public void close() {
      if (handle != null) {
        calls.close(handle);
        handle = null;
        sourceProcess = null;
      }
    }
  }

  private static Target open(NativeCalls calls, long pid, ProcessTimes times, boolean required) {
    calls.openArguments[2] = (int) pid; // DWORD is 32 bits, including on 64-bit Windows.
    Pointer handle = calls.openProcess.invokePointer(calls.openArguments);
    if (handle == null) {
      int error = Native.getLastError();
      // A snapshot can race an exiting child, or include a protected child we cannot inspect.
      if (!required && (error == 5 || error == 87)) {
        if (error == 5 && !calls.reportedAccessDenied) {
          calls.reportedAccessDenied = true;
          Utils.getLogger().warning("Could not inspect a wrapper descendant (Windows error 5)");
        }
        return null;
      }
      throw new IllegalStateException("OpenProcess failed (Windows error " + error + ")");
    }
    try {
      times.read(calls, handle);
      if (times.memory.getLong(0) <= 0)
        throw new IllegalStateException("Invalid process creation time");
      return new Target(calls, pid, handle, times);
    } catch (RuntimeException | Error e) {
      calls.close(handle);
      throw e;
    }
  }

  private static void requirePid(long pid) {
    if (pid <= 0 || pid > 0xffffffffL)
      throw new IllegalArgumentException("Invalid Windows process ID");
  }

  private static long getPid(Process process, NativeCalls calls)
      throws ReflectiveOperationException {
    try {
      // Java 9+, without linking a Java 9 API into Java 8 bytecode.
      return (Long) Process.class.getMethod("pid").invoke(process);
    } catch (NoSuchMethodException java8) {
      Field handleField = process.getClass().getDeclaredField("handle");
      handleField.setAccessible(true);
      calls.processIdArguments[0] = Pointer.createConstant(handleField.getLong(process));
      return Integer.toUnsignedLong(calls.getProcessId.invokeInt(calls.processIdArguments));
    } catch (InvocationTargetException e) {
      throw new IllegalStateException("Could not read the Minecraft process ID", e.getCause());
    }
  }

  private static IllegalStateException nativeFailure(String operation) {
    return new IllegalStateException(
        operation + " failed (Windows error " + Native.getLastError() + ")");
  }

  /** One native binding context per root, shared by all of its descendants. */
  private static final class NativeCalls {
    final Function findWindowEx;
    final Function getWindowThreadProcessId;
    final Function isWindowVisible;
    final Function isIconic;
    final Function getWindow;
    final Function getWindowLong;
    final Function getClassName;
    final Function isZoomed;
    final Function showWindowAsync;
    final Function openProcess;
    final Function getProcessId;
    final Function getProcessTimes;
    final Function waitForSingleObject;
    final Function closeHandle;
    final Function createSnapshot;
    final Function processFirst;
    final Function processNext;
    final Object[] openArguments = {PROCESS_ACCESS, false, 0};
    final Object[] waitArguments = {null, 0};
    final Object[] closeArguments = {null};
    final Object[] processIdArguments = {null};
    final Object[] snapshotArguments = {TH32CS_SNAPPROCESS, 0};
    boolean reportedAccessDenied;

    NativeCalls() {
      NativeLibrary user32 = NativeLibrary.getInstance("user32");
      findWindowEx = function(user32, "FindWindowExW");
      getWindowThreadProcessId = function(user32, "GetWindowThreadProcessId");
      isWindowVisible = function(user32, "IsWindowVisible");
      isIconic = function(user32, "IsIconic");
      getWindow = function(user32, "GetWindow");
      getWindowLong = function(user32, "GetWindowLongW");
      getClassName = function(user32, "GetClassNameW");
      isZoomed = function(user32, "IsZoomed");
      showWindowAsync = function(user32, "ShowWindowAsync");
      NativeLibrary kernel32 = NativeLibrary.getInstance("kernel32");
      openProcess = function(kernel32, "OpenProcess");
      getProcessId = function(kernel32, "GetProcessId");
      getProcessTimes = function(kernel32, "GetProcessTimes");
      waitForSingleObject = function(kernel32, "WaitForSingleObject");
      closeHandle = function(kernel32, "CloseHandle");
      createSnapshot = function(kernel32, "CreateToolhelp32Snapshot");
      processFirst = function(kernel32, "Process32FirstW");
      processNext = function(kernel32, "Process32NextW");
    }

    private static Function function(NativeLibrary library, String name) {
      return library.getFunction(name, Function.ALT_CONVENTION);
    }

    void close(Pointer handle) {
      closeArguments[0] = handle;
      if (closeHandle.invokeInt(closeArguments) == 0) {
        Utils.getLogger().warning("Could not close maximizer handle: " + Native.getLastError());
      }
    }
  }

  private static final class ProcessTimes implements AutoCloseable {
    final Memory memory = new Memory(32);
    final Object[] arguments = {null, memory, memory.share(8), memory.share(16), memory.share(24)};

    void read(NativeCalls calls, Pointer handle) {
      arguments[0] = handle;
      if (calls.getProcessTimes.invokeInt(arguments) == 0) throw nativeFailure("GetProcessTimes");
    }

    @Override
    public void close() {
      memory.close();
    }
  }

  static boolean createdDuringParentLifetime(long parentCreation, long parentExit, long creation) {
    return creation >= parentCreation && (parentExit == 0 || creation <= parentExit);
  }

  private static final class ProcessTree implements AutoCloseable {
    private final NativeCalls calls;
    private final List<Target> targets = new ArrayList<>();
    private final ProcessTimes times = new ProcessTimes();
    private final Memory entry;
    private final Object[] entryArguments;
    private long[] pids;
    private long[] parentPids;

    ProcessTree(Target root, boolean followDescendants) {
      calls = root.calls;
      targets.add(root);
      // PROCESSENTRY32W contains a pointer-sized ULONG_PTR, but all PIDs remain DWORDs.
      entry = followDescendants ? new Memory(Native.POINTER_SIZE == 8 ? 568 : 556) : null;
      entryArguments = followDescendants ? new Object[] {null, entry} : null;
      pids = followDescendants ? new long[128] : null;
      parentPids = followDescendants ? new long[128] : null;
    }

    boolean refresh() {
      for (int i = 0; i < targets.size(); i++) targets.get(i).refresh(times);
      if (entry != null) discoverDescendants();
      for (int i = 0; i < targets.size(); i++) {
        if (targets.get(i).alive) return true;
      }
      return false;
    }

    private void discoverDescendants() {
      Pointer snapshot = calls.createSnapshot.invokePointer(calls.snapshotArguments);
      if (snapshot == null
          || Pointer.nativeValue(snapshot) == -1L
          || (Native.POINTER_SIZE == 4 && Pointer.nativeValue(snapshot) == 0xffffffffL)) {
        throw nativeFailure("CreateToolhelp32Snapshot");
      }
      int count = 0;
      try {
        entry.setInt(0, (int) entry.size());
        entryArguments[0] = snapshot;
        int found = calls.processFirst.invokeInt(entryArguments);
        while (found != 0) {
          if (count == pids.length) {
            pids = Arrays.copyOf(pids, count * 2);
            parentPids = Arrays.copyOf(parentPids, count * 2);
          }
          pids[count] = Integer.toUnsignedLong(entry.getInt(8));
          parentPids[count] =
              Integer.toUnsignedLong(entry.getInt(Native.POINTER_SIZE == 8 ? 32 : 24));
          count++;
          found = calls.processNext.invokeInt(entryArguments);
        }
        if (Native.getLastError() != ERROR_NO_MORE_FILES) throw nativeFailure("Process32NextW");
      } finally {
        calls.close(snapshot);
      }

      // Snapshot order is arbitrary: resolve children before their grandchildren in repeated
      // passes.
      boolean added;
      do {
        added = false;
        for (int i = 0; i < count; i++) {
          long pid = pids[i];
          if (pid == 0 || find(pid) != null) continue;
          Target parent = find(parentPids[i]);
          if (parent == null) continue;
          pids[i] = 0;
          Target child = open(calls, pid, times, false);
          if (child == null) continue;
          boolean retained = false;
          try {
            parent.refresh(times);
            if (createdDuringParentLifetime(
                parent.creationTime, parent.exitTime, child.creationTime)) {
              // Retain even exited processes until this run ends, both to protect PID identities
              // and
              // to recognize a surviving grandchild after an intermediate wrapper has exited.
              targets.add(child);
              retained = true;
              child.refresh(times);
              added = true;
            }
          } finally {
            if (!retained) child.close();
          }
        }
      } while (added);
    }

    private Target find(long pid) {
      for (int i = 0; i < targets.size(); i++) {
        Target target = targets.get(i);
        if (target.pid == pid) return target;
      }
      return null;
    }

    boolean ownsLiveProcess(long pid) {
      Target target = find(pid);
      return target != null && target.alive;
    }

    @Override
    public void close() {
      for (int i = 1; i < targets.size(); i++) targets.get(i).close();
      if (entry != null) entry.close();
      times.close();
    }
  }

  static boolean isMaximizableStyle(int style) {
    return (style & WS_THICKFRAME) != 0 && (style & WS_MAXIMIZEBOX) != 0;
  }

  static boolean isGameClass(Memory name, int length) {
    return matchesClass(name, length, "LWJGL") || matchesClass(name, length, "GLFW30");
  }

  private static boolean matchesClass(Memory name, int length, String expected) {
    if (length != expected.length()) return false;
    for (int i = 0; i < length; i++) {
      if ((char) name.getShort(i * 2L) != expected.charAt(i)) return false;
    }
    return true;
  }

  private static final class WindowPoller implements AutoCloseable {
    private final NativeCalls calls;
    private final WindowPolicy policy = new WindowPolicy();
    private final Memory windowPid = new Memory(4);
    private final Memory className = new Memory(256 * 2);
    private final Object[] findArguments = {null, null, null, null};
    private final Object[] windowArguments = {null};
    private final Object[] pidArguments = {null, windowPid};
    private final Object[] ownerArguments = {null, GW_OWNER};
    private final Object[] styleArguments = {null, GWL_STYLE};
    private final Object[] classArguments = {null, className, 256};
    private final Object[] showArguments = {null, SW_MAXIMIZE};
    private long currentPid;

    WindowPoller(NativeCalls calls) {
      this.calls = calls;
    }

    boolean poll(ProcessTree tree, long now) {
      policy.beginSweep();
      findArguments[1] = null;
      Pointer window;
      while ((window = calls.findWindowEx.invokePointer(findArguments)) != null) {
        findArguments[1] = window;
        boolean eligible = isMaximizableGameWindow(window, tree);
        long pid = currentPid;
        boolean zoomed = eligible && calls.isZoomed.invokeInt(windowArguments) != 0;
        long id = Pointer.nativeValue(window);
        int action = policy.observe(id, pid, eligible, zoomed, now);
        if (action == WindowPolicy.CONFIRMED) {
          Utils.getLogger().info("Maximized the Minecraft window");
          return true;
        }
        if (action == WindowPolicy.ALREADY_MAXIMIZED) return true;
        if (action == WindowPolicy.REQUEST) {
          // Recheck immediately before posting: never restore a window the player just minimized.
          if (!isMaximizableGameWindow(window, tree) || currentPid != pid) {
            policy.requestFailed(id);
            continue;
          }
          if (calls.isZoomed.invokeInt(windowArguments) != 0) return true;
          showArguments[0] = window;
          if (calls.showWindowAsync.invokeInt(showArguments) == 0) {
            policy.requestFailed(id);
            Utils.getLogger().warning("Could not request Minecraft window maximization");
          }
        }
      }
      policy.endSweep();
      return false;
    }

    private boolean isMaximizableGameWindow(Pointer window, ProcessTree tree) {
      pidArguments[0] = window;
      windowPid.setInt(0, 0);
      calls.getWindowThreadProcessId.invokeInt(pidArguments);
      currentPid = Integer.toUnsignedLong(windowPid.getInt(0));
      if (!tree.ownsLiveProcess(currentPid)) return false;
      windowArguments[0] = window;
      ownerArguments[0] = window;
      if (calls.isWindowVisible.invokeInt(windowArguments) == 0
          || calls.isIconic.invokeInt(windowArguments) != 0
          || calls.getWindow.invokePointer(ownerArguments) != null) {
        return false;
      }
      // GWL_STYLE returns a 32-bit LONG, not a pointer-sized LONG_PTR.
      styleArguments[0] = window;
      if (!isMaximizableStyle(calls.getWindowLong.invokeInt(styleArguments))) return false;
      classArguments[0] = window;
      return isGameClass(className, calls.getClassName.invokeInt(classArguments));
    }

    @Override
    public void close() {
      className.close();
      windowPid.close();
    }
  }

  /**
   * Deterministic window lifecycle policy. A brief eligible period filters transient splash
   * windows; a surviving HWND receives at most one request, including when posting or confirmation
   * fails.
   */
  static final class WindowPolicy {
    static final int WAIT = 0;
    static final int REQUEST = 1;
    static final int CONFIRMED = 2;
    static final int ALREADY_MAXIMIZED = 3;
    static final long STABLE_NANOS = 1_500_000_000L;
    static final long CONFIRM_NANOS = 2_000_000_000L;
    private final List<Candidate> candidates = new ArrayList<>();

    void beginSweep() {
      for (int i = 0; i < candidates.size(); i++) candidates.get(i).seen = false;
    }

    int observe(long window, long pid, boolean eligible, boolean zoomed, long now) {
      Candidate candidate = null;
      for (int i = 0; i < candidates.size(); i++) {
        Candidate existing = candidates.get(i);
        if (existing.window == window) {
          if (existing.pid == pid || pid == 0) candidate = existing;
          else candidates.remove(i);
          break;
        }
      }
      if (candidate == null) {
        if (!eligible) return WAIT;
        candidate = new Candidate(window, pid);
        candidates.add(candidate);
      }
      candidate.seen = true;
      if (!eligible) {
        candidate.eligible = false;
        return WAIT;
      }
      if (!candidate.eligible) {
        candidate.eligible = true;
        candidate.stableSince = now;
      }
      if (candidate.attempted) {
        if (candidate.pending && now - candidate.confirmUntil < 0) {
          if (zoomed) return CONFIRMED;
        } else {
          candidate.pending = false;
        }
        return WAIT;
      }
      if (now - candidate.stableSince < STABLE_NANOS) return WAIT;
      if (zoomed) return ALREADY_MAXIMIZED;
      candidate.attempted = true;
      candidate.pending = true;
      candidate.confirmUntil = now + CONFIRM_NANOS;
      return REQUEST;
    }

    void requestFailed(long window) {
      for (int i = 0; i < candidates.size(); i++) {
        Candidate candidate = candidates.get(i);
        if (candidate.window == window) {
          candidate.pending = false;
          return;
        }
      }
    }

    void endSweep() {
      for (int i = candidates.size() - 1; i >= 0; i--) {
        if (!candidates.get(i).seen) candidates.remove(i);
      }
    }

    private static final class Candidate {
      final long window;
      final long pid;
      boolean seen;
      boolean eligible;
      boolean attempted;
      boolean pending;
      long stableSince;
      long confirmUntil;

      Candidate(long window, long pid) {
        this.window = window;
        this.pid = pid;
      }
    }
  }
}
