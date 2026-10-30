package net.technicpack.minecraftcore.microsoft.auth;

import com.sun.jna.Function;
import com.sun.jna.Memory;
import com.sun.jna.Native;
import com.sun.jna.NativeLibrary;
import com.sun.jna.Pointer;
import com.sun.jna.Structure;
import com.sun.jna.WString;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Locale;

/** Windows-only, non-destructive ownership preparation and explicitly requested ACL repair. */
public final class WindowsCredentialStore {
  private static final int CANCELLED = 1223;

  private WindowsCredentialStore() {}

  public static boolean isSupported() {
    return System.getProperty("os.name", "").startsWith("Windows");
  }

  /** Never elevates. An inaccessible or differently owned store is left for explicit recovery. */
  static void prepare(Path directory) throws IOException {
    try {
      execute(directory, false);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      throw new IOException("Interrupted while preparing the Windows credential store", e);
    }
  }

  /**
   * Prompts for elevation only when invoked. Repairs only this oauth directory and its
   * StoredCredential file; neither is deleted, renamed, or rewritten. Returns false only when
   * Windows reports that the user cancelled elevation. Call off the Swing event thread.
   */
  public static boolean repair(Path directory) throws IOException, InterruptedException {
    return execute(directory, true);
  }

  private static boolean execute(Path directory, boolean repair)
      throws IOException, InterruptedException {
    if (!isSupported()) throw new IOException("Windows credential repair is not supported here");
    String path = validatePath(directory.toAbsolutePath().toString(), repair);
    try {
      Windows windows = new Windows();
      String sid = windows.currentUserSid();
      if (!repair && windows.alreadyOwned(path, sid)) return true;
      String powershell = windows.systemPowerShell();
      String source;
      try (InputStream input =
          WindowsCredentialStore.class.getResourceAsStream(
              "/net/technicpack/launcher/resources/WindowsCredentialStore.cs")) {
        if (input == null) throw new IOException("Windows credential helper is missing");
        source = new String(readAll(input), StandardCharsets.UTF_8);
      }
      String systemDirectory = Paths.get(powershell).getParent().getParent().getParent().toString();
      String script =
          "$ErrorActionPreference='Stop';$result=1;$temp=$null;try {"
              + "$env:PSModulePath=$PSHOME+'\\Modules';"
              + "Import-Module ($PSHOME+'\\Modules\\Microsoft.PowerShell.Utility\\Microsoft.PowerShell.Utility.psd1');"
              + (repair ? secureCompilerDirectory(systemDirectory) : "")
              + "Add-Type -TypeDefinition "
              + literal(source)
              + ";[TechnicCredentialStore]::Run("
              + literal(path)
              + ","
              + literal(sid)
              + ","
              + (repair ? "$true" : "$false")
              + ");$result=0} catch {$e=$_.Exception;"
              + "while($null -ne $e.InnerException){$e=$e.InnerException};"
              + "[Console]::Error.WriteLine($e.Message);"
              + "if($e -is [System.ComponentModel.Win32Exception]){$result=$e.NativeErrorCode}}"
              + "finally {if($null -ne $temp){try {[IO.Directory]::Delete($temp,$true)}"
              + "catch {$result=1}}};exit $result";
      String encoded = encode(script);
      checkCommandLength(powershell, encoded);
      // Embed the entire trusted helper in the elevated command, never a mutable script path.
      if (repair) return windows.elevate(powershell, systemDirectory, encoded);
      Process process =
          new ProcessBuilder(
                  powershell,
                  "-NoLogo",
                  "-NoProfile",
                  "-NonInteractive",
                  "-EncodedCommand",
                  encoded)
              .directory(Paths.get(systemDirectory).toFile())
              .redirectErrorStream(true)
              .start();
      try {
        byte[] diagnostics;
        try (InputStream input = process.getInputStream()) {
          diagnostics = readAll(input);
        }
        int exit = process.waitFor();
        if (exit != 0) {
          throw new IOException(
              "Windows credential helper failed (exit "
                  + exit
                  + "): "
                  + new String(diagnostics, StandardCharsets.UTF_8).trim());
        }
        return true;
      } catch (InterruptedException e) {
        process.destroy();
        throw e;
      }
    } catch (LinkageError | RuntimeException e) {
      throw new IOException("Could not run the Windows credential helper", e);
    }
  }

  private static String secureCompilerDirectory(String systemDirectory) {
    // CodeDOM may compile through temporary DLLs. Keep them under a protected system parent,
    // explicitly Administrators-owned, not the initiating user's writable TEMP or current
    // directory.
    return "$system="
        + literal(systemDirectory)
        + ";"
        + "$admins=[Security.Principal.SecurityIdentifier]::new('S-1-5-32-544');"
        + "$acl=[Security.AccessControl.DirectorySecurity]::new();"
        + "$acl.SetOwner($admins);$acl.SetAccessRuleProtection($true,$false);"
        + "$acl.AddAccessRule([Security.AccessControl.FileSystemAccessRule]::new("
        + "$admins,'FullControl','ContainerInherit,ObjectInherit','None','Allow'));"
        + "$candidate=[IO.Path]::Combine($system,'TechnicCredentialRepair-'+[Guid]::NewGuid().ToString('N'));"
        + "[IO.Directory]::CreateDirectory($candidate,$acl)|Out-Null;$temp=$candidate;"
        + "$env:TEMP=$temp;$env:TMP=$temp;$env:PATH=$system;";
  }

  static void checkCommandLength(String executable, String encoded) throws IOException {
    // Include executable quotes, separating space, fixed arguments and the terminating NUL.
    if (executable.length()
            + encoded.length()
            + 4
            + "-NoLogo -NoProfile -NonInteractive -EncodedCommand ".length()
        > 32767) {
      throw new IOException(
          "Windows credential helper command exceeds the Windows command-line limit");
    }
  }

  static boolean checkedExit(int exit) throws IOException {
    if (exit == 0) return true;
    throw new IOException("Windows credential helper failed (exit " + exit + ")");
  }

  static boolean checkedElevationError(int error) throws IOException {
    if (error == CANCELLED) return false;
    throw new IOException("Windows elevation failed (error " + error + ")");
  }

  // Reject alternate streams, device paths and Win32 aliases before invoking any native code.
  // Ordinary preparation also supports UNC stores; elevated repair remains local-only.
  static String validatePath(String path, boolean repair) throws IOException {
    boolean local = path.matches("[A-Za-z]:\\\\.+");
    boolean network = !repair && path.startsWith("\\\\");
    if ((!local && !network) || path.indexOf('/') >= 0) {
      throw new IOException("Unsupported Windows credential path");
    }
    String[] components = path.substring(local ? 3 : 2).split("\\\\", -1);
    if (network && components.length < 3) {
      throw new IOException("Credential store must be below a network share root");
    }
    for (String component : components) {
      String base = component.split("\\.", 2)[0].toUpperCase(Locale.ROOT);
      if (component.isEmpty()
          || component.equals(".")
          || component.equals("..")
          || component.endsWith(".")
          || component.endsWith(" ")
          || component.matches(".*[<>:\"/|?*\\x00-\\x1f].*")
          || base.matches("CON|PRN|AUX|NUL|COM[0-9¹²³]|LPT[0-9¹²³]")) {
        throw new IOException("Unsafe Windows credential path");
      }
    }
    if (repair && !components[components.length - 1].equalsIgnoreCase("oauth")) {
      throw new IOException("Only the launcher's oauth directory can be repaired");
    }
    return path;
  }

  static String literal(String value) {
    return "'" + value.replace("'", "''") + "'";
  }

  private static String encode(String script) {
    return Base64.getEncoder().encodeToString(script.getBytes(StandardCharsets.UTF_16LE));
  }

  private static byte[] readAll(InputStream input) throws IOException {
    ByteArrayOutputStream output = new ByteArrayOutputStream();
    byte[] buffer = new byte[4096];
    int size;
    while ((size = input.read(buffer)) != -1) output.write(buffer, 0, size);
    return output.toByteArray();
  }

  private static final class Windows {
    private final NativeLibrary kernel = NativeLibrary.getInstance("kernel32");
    private final NativeLibrary security = NativeLibrary.getInstance("advapi32");

    private Function kernel(String name) {
      return kernel.getFunction(name, Function.ALT_CONVENTION);
    }

    private Function security(String name) {
      return security.getFunction(name, Function.ALT_CONVENTION);
    }

    // Healthy stores need no subprocess or compiler. Inspect handles rather than resolving names
    // through user names, environment variables or filesystem links.
    boolean alreadyOwned(String path, String sid) throws IOException {
      String root = Paths.get(path).getRoot().toString();
      int drive = kernel("GetDriveTypeW").invokeInt(new Object[] {new WString(root)});
      if (drive != 2 && drive != 3 && drive != 4) {
        throw new IOException("Unsupported Windows credential drive");
      }
      List<Pointer> handles = new ArrayList<>();
      try {
        Pointer rootHandle = open(root, true, false);
        if (rootHandle == null) return false;
        handles.add(rootHandle);
        String[] parts = path.substring(root.length()).split("\\\\");
        String current = root;
        Pointer directory = null;
        for (int i = 0; i < parts.length; i++) {
          current = current.endsWith("\\") ? current + parts[i] : current + "\\" + parts[i];
          directory = open(current, true, i == parts.length - 1);
          if (directory == null) return false;
          handles.add(directory);
        }
        if (!ownedByUser(directory, sid)) return false;
        Pointer credential = open(path + "\\StoredCredential", false, true);
        if (credential == null) return true;
        handles.add(credential);
        return ownedByUser(credential, sid);
      } finally {
        for (int i = handles.size() - 1; i >= 0; i--) {
          kernel("CloseHandle").invokeInt(new Object[] {handles.get(i)});
        }
      }
    }

    private Pointer open(String path, boolean directory, boolean readOwner) throws IOException {
      Pointer handle =
          kernel("CreateFileW")
              .invokePointer(
                  new Object[] {
                    new WString(path), readOwner ? 0x20000 : 0, 1, null, 3, 0x02200000, null
                  });
      long address = Pointer.nativeValue(handle);
      if (address == -1 || (Native.POINTER_SIZE == 4 && address == 0xffffffffL)) {
        int error = Native.getLastError();
        if (error == 2 || error == 3) return null;
        throw new IOException("Cannot inspect Windows credential store (error " + error + ")");
      }
      boolean valid = false;
      try (Memory info = new Memory(52)) {
        if (kernel("GetFileInformationByHandle").invokeInt(new Object[] {handle, info}) == 0) {
          throw new IOException(
              "Cannot inspect Windows credential object (error " + Native.getLastError() + ")");
        }
        int attributes = info.getInt(0);
        if ((attributes & 0x400) != 0
            || ((attributes & 0x10) != 0) != directory
            || (!directory && info.getInt(40) != 1)) {
          throw new IOException("Unsafe Windows credential store object");
        }
        valid = true;
        return handle;
      } finally {
        if (!valid) kernel("CloseHandle").invokeInt(new Object[] {handle});
      }
    }

    private boolean ownedByUser(Pointer handle, String sid) throws IOException {
      int width = Native.POINTER_SIZE;
      try (Memory result = new Memory(width * 5L);
          Memory ownerString = new Memory(width)) {
        int error =
            security("GetSecurityInfo")
                .invokeInt(
                    new Object[] {
                      handle,
                      1,
                      1,
                      result,
                      result.share(width),
                      result.share(width * 2L),
                      result.share(width * 3L),
                      result.share(width * 4L)
                    });
        if (error != 0) throw new IOException("Cannot read credential owner (error " + error + ")");
        Pointer descriptor = result.getPointer(width * 4L);
        try {
          if (security("ConvertSidToStringSidW")
                  .invokeInt(new Object[] {result.getPointer(0), ownerString})
              == 0) {
            throw new IOException(
                "Cannot identify credential owner (error " + Native.getLastError() + ")");
          }
          Pointer owner = ownerString.getPointer(0);
          try {
            String value = owner.getWideString(0);
            if (sid.equals(value)) return true;
            if ("S-1-5-32-544".equals(value)) return false;
            throw new IOException("Credential store belongs to another Windows user");
          } finally {
            kernel("LocalFree").invokePointer(new Object[] {owner});
          }
        } finally {
          kernel("LocalFree").invokePointer(new Object[] {descriptor});
        }
      }
    }

    boolean elevate(String executable, String systemDirectory, String encodedScript)
        throws IOException, InterruptedException {
      ShellExecuteInfo info = new ShellExecuteInfo();
      info.cbSize = info.size();
      info.fMask = 0x40 | 0x100 | 0x400; // NOCLOSEPROCESS, NOASYNC, FLAG_NO_UI
      info.lpVerb = new WString("runas");
      info.lpFile = new WString(executable);
      info.lpDirectory = new WString(systemDirectory);
      info.lpParameters =
          new WString("-NoLogo -NoProfile -NonInteractive -EncodedCommand " + encodedScript);
      info.nShow = 0;
      Function execute =
          NativeLibrary.getInstance("shell32")
              .getFunction("ShellExecuteExW", Function.ALT_CONVENTION);
      if (execute.invokeInt(new Object[] {info}) == 0) {
        return checkedElevationError(Native.getLastError());
      }
      Pointer process = info.hProcess;
      if (process == null) throw new IOException("Windows elevation returned no process");
      try (Memory exitCode = new Memory(4)) {
        while (true) {
          if (Thread.interrupted()) throw new InterruptedException();
          int wait = kernel("WaitForSingleObject").invokeInt(new Object[] {process, 200});
          if (wait == 0) break;
          if (wait != 258) throw new IOException("Could not wait for Windows credential repair");
        }
        if (kernel("GetExitCodeProcess").invokeInt(new Object[] {process, exitCode}) == 0) {
          throw new IOException("Could not read Windows credential repair result");
        }
        // Only ShellExecuteEx's ERROR_CANCELLED means UAC cancellation, not a helper exit code.
        return checkedExit(exitCode.getInt(0));
      } finally {
        kernel("CloseHandle").invokeInt(new Object[] {process});
      }
    }

    String systemPowerShell() throws IOException {
      try (Memory buffer = new Memory(32768 * 2L)) {
        int count = kernel("GetSystemDirectoryW").invokeInt(new Object[] {buffer, 32768});
        if (count == 0 || count >= 32768)
          throw new IOException("Cannot locate Windows system directory");
        return Paths.get(buffer.getWideString(0), "WindowsPowerShell", "v1.0", "powershell.exe")
            .toString();
      }
    }

    String currentUserSid() throws IOException {
      Pointer process = kernel("GetCurrentProcess").invokePointer(new Object[0]);
      try (Memory tokenResult = new Memory(Native.POINTER_SIZE);
          Memory size = new Memory(4)) {
        if (security("OpenProcessToken").invokeInt(new Object[] {process, 8, tokenResult}) == 0) {
          throw new IOException("Cannot read Windows process token");
        }
        Pointer token = tokenResult.getPointer(0);
        try {
          size.clear();
          security("GetTokenInformation").invokeInt(new Object[] {token, 1, null, 0, size});
          int length = size.getInt(0);
          if (length <= 0) throw new IOException("Cannot size Windows token user");
          try (Memory user = new Memory(length);
              Memory sidResult = new Memory(Native.POINTER_SIZE)) {
            if (security("GetTokenInformation")
                        .invokeInt(new Object[] {token, 1, user, length, size})
                    == 0
                || security("ConvertSidToStringSidW")
                        .invokeInt(new Object[] {user.getPointer(0), sidResult})
                    == 0) {
              throw new IOException("Cannot identify Windows process user");
            }
            Pointer sid = sidResult.getPointer(0);
            try {
              return sid.getWideString(0);
            } finally {
              kernel("LocalFree").invokePointer(new Object[] {sid});
            }
          }
        } finally {
          kernel("CloseHandle").invokeInt(new Object[] {token});
        }
      }
    }
  }

  public static final class ShellExecuteInfo extends Structure {
    public int cbSize;
    public int fMask;
    public Pointer hwnd;
    public WString lpVerb;
    public WString lpFile;
    public WString lpParameters;
    public WString lpDirectory;
    public int nShow;
    public Pointer hInstApp;
    public Pointer lpIDList;
    public Pointer lpClass;
    public Pointer hkeyClass;
    public int dwHotKey;
    public Pointer hIcon;
    public Pointer hProcess;

    @Override
    protected List<String> getFieldOrder() {
      return Arrays.asList(
          "cbSize",
          "fMask",
          "hwnd",
          "lpVerb",
          "lpFile",
          "lpParameters",
          "lpDirectory",
          "nShow",
          "hInstApp",
          "lpIDList",
          "lpClass",
          "hkeyClass",
          "dwHotKey",
          "hIcon",
          "hProcess");
    }
  }
}
