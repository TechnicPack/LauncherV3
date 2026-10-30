package net.technicpack.minecraftcore.microsoft.auth;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.google.api.client.auth.oauth2.StoredCredential;
import com.google.api.client.util.store.DataStore;
import com.google.api.client.util.store.FileDataStoreFactory;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class WindowsCredentialStoreTest {
  @TempDir Path temporary;

  @ParameterizedTest
  @ValueSource(
      strings = {
        "C:\\",
        "C:oauth",
        "\\\\server\\share\\oauth",
        "\\\\?\\C:\\oauth",
        "C:\\launcher\\..\\oauth",
        "C:\\launcher\\.\\oauth",
        "C:\\launcher\\oauth.",
        "C:\\launcher\\oauth ",
        "C:\\launcher\\oauth:stream",
        "C:\\launcher\\\\oauth",
        "C:\\NUL\\oauth",
        "C:\\con.txt\\oauth",
        "C:\\COM1\\oauth",
        "C:\\LPT²\\oauth",
        "C:/launcher/oauth",
        "C:\\launcher\\*\\oauth",
        "C:\\launcher\\oauth\\",
        "C:\\Windows",
        "C:\\launcher\\not-oauth"
      })
  void rejectsAmbiguousOrOutOfScopeRepairPaths(String path) {
    assertThrows(IOException.class, () -> WindowsCredentialStore.validatePath(path, true));
  }

  @ParameterizedTest
  @ValueSource(
      strings = {"C:\\launcher\\oauth", "D:\\O'Brien [test]; $(not-code)\\oauth", "C:\\日本語\\OAUTH"})
  void acceptsLiteralLocalOAuthPaths(String path) throws Exception {
    assertEquals(path, WindowsCredentialStore.validatePath(path, true));
  }

  @Test
  void networkStoresRemainAvailableForOrdinaryPreparationButNotElevation() throws Exception {
    String network = "\\\\server\\share\\launcher\\oauth";
    assertEquals(network, WindowsCredentialStore.validatePath(network, false));
    assertThrows(IOException.class, () -> WindowsCredentialStore.validatePath(network, true));
    assertThrows(
        IOException.class, () -> WindowsCredentialStore.validatePath("\\\\?\\C:\\oauth", false));
    assertThrows(
        IOException.class, () -> WindowsCredentialStore.validatePath("\\\\server\\share", false));
  }

  @Test
  void quotesPowerShellLiteralWithoutInterpretingSyntax() {
    assertEquals(
        "'a''b; $(anything) ` $value'",
        WindowsCredentialStore.literal("a'b; $(anything) ` $value"));
  }

  @Test
  void boundsTheWholeUtf16WindowsCommand() throws Exception {
    String executable = "C:\\Windows\\System32\\WindowsPowerShell\\v1.0\\powershell.exe";
    int available =
        32767
            - executable.length()
            - 4
            - "-NoLogo -NoProfile -NonInteractive -EncodedCommand ".length();
    String encoded = new String(new char[available]).replace('\0', 'A');
    WindowsCredentialStore.checkCommandLength(executable, encoded);
    assertThrows(
        IOException.class,
        () -> WindowsCredentialStore.checkCommandLength(executable, encoded + "A"));
  }

  @Test
  void distinguishesSuccessfulHelperExitFromCancellationAndFailure() throws Exception {
    assertTrue(WindowsCredentialStore.checkedExit(0));
    assertFalse(WindowsCredentialStore.checkedElevationError(1223));
    assertThrows(IOException.class, () -> WindowsCredentialStore.checkedExit(1223));
    IOException error =
        assertThrows(IOException.class, () -> WindowsCredentialStore.checkedElevationError(5));
    assertTrue(error.getMessage().contains("5"));
  }

  @Test
  void openingAuthenticatorPreservesSerializedCredentialsAcrossReopens() throws Exception {
    Path directory = temporary.resolve("oauth");
    if (WindowsCredentialStore.isSupported()) WindowsCredentialStore.prepare(directory);
    DataStore<StoredCredential> store =
        new FileDataStoreFactory(directory.toFile())
            .getDataStore(StoredCredential.DEFAULT_DATA_STORE_ID);
    store.set("sentinel-user", new StoredCredential().setRefreshToken("preserved-refresh-token"));
    Path credential = directory.resolve(StoredCredential.DEFAULT_DATA_STORE_ID);
    byte[] original = Files.readAllBytes(credential);

    assertNull(new MicrosoftAuthenticator(directory.toFile()).getUnavailableCredentialStorePath());
    assertNull(new MicrosoftAuthenticator(directory.toFile()).getUnavailableCredentialStorePath());

    assertArrayEquals(original, Files.readAllBytes(credential));
    DataStore<StoredCredential> reopened =
        new FileDataStoreFactory(directory.toFile())
            .getDataStore(StoredCredential.DEFAULT_DATA_STORE_ID);
    assertEquals("preserved-refresh-token", reopened.get("sentinel-user").getRefreshToken());
  }

  @Test
  void nonDirectoryStoreFallsBackWithoutDeletingOrRenamingAnything() throws Exception {
    Path directory = temporary.resolve("oauth");
    byte[] original = "existing data must survive".getBytes(StandardCharsets.UTF_8);
    Files.write(directory, original);
    assertEquals(
        directory.toString(),
        new MicrosoftAuthenticator(directory.toFile()).getUnavailableCredentialStorePath());
    assertArrayEquals(original, Files.readAllBytes(directory));
    try (java.util.stream.Stream<Path> children = Files.list(temporary)) {
      assertEquals(1, children.count());
    }
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void newlyCreatedCredentialFileUsesTheSecuredDirectoryOwner() throws Exception {
    Path directory = temporary.resolve("oauth");
    assertNull(new MicrosoftAuthenticator(directory.toFile()).getUnavailableCredentialStorePath());
    assertEquals(
        Files.getOwner(directory),
        Files.getOwner(directory.resolve(StoredCredential.DEFAULT_DATA_STORE_ID)));
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void preparationRejectsHardLinkedCredentialsWithoutTouchingEitherName() throws Exception {
    Path directory = Files.createDirectory(temporary.resolve("oauth"));
    Path outside = temporary.resolve("outside");
    byte[] original = "untouched linked credential".getBytes(StandardCharsets.UTF_8);
    Files.write(outside, original);
    Path credential = Files.createLink(directory.resolve("StoredCredential"), outside);
    assertThrows(IOException.class, () -> WindowsCredentialStore.prepare(directory));
    assertArrayEquals(original, Files.readAllBytes(outside));
    assertArrayEquals(original, Files.readAllBytes(credential));
    assertTrue(Files.isSameFile(outside, credential));
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void preparationRejectsCredentialDirectoryAndPreservesItsContents() throws Exception {
    Path directory = Files.createDirectory(temporary.resolve("oauth"));
    Path credential = Files.createDirectory(directory.resolve("StoredCredential"));
    Path sentinel = Files.write(credential.resolve("sentinel"), new byte[] {4, 2});
    assertThrows(IOException.class, () -> WindowsCredentialStore.prepare(directory));
    assertArrayEquals(new byte[] {4, 2}, Files.readAllBytes(sentinel));
  }

  @Test
  @EnabledOnOs(OS.WINDOWS)
  void preparationRejectsReparseAncestorsWithoutTouchingTheirTargets() throws Exception {
    Path target = Files.createDirectory(temporary.resolve("target"));
    Path oauth = Files.createDirectory(target.resolve("oauth"));
    Path sentinel = Files.write(oauth.resolve("sentinel"), new byte[] {1, 2, 3});
    Path link = temporary.resolve("link");
    try {
      Files.createSymbolicLink(link, target);
    } catch (IOException | UnsupportedOperationException e) {
      Assumptions.abort(
          "Windows symlink creation requires Developer Mode or the symlink privilege");
    }
    assertThrows(IOException.class, () -> WindowsCredentialStore.prepare(link.resolve("oauth")));
    assertArrayEquals(new byte[] {1, 2, 3}, Files.readAllBytes(sentinel));
  }
}
