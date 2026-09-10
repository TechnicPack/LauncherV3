package net.technicpack.launchercore.auth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.swing.SwingUtilities;
import net.technicpack.launcher.io.UserStore;
import net.technicpack.minecraftcore.microsoft.auth.MicrosoftUser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UserModelTest {
  @TempDir Path tempDir;

  @Test
  void backgroundLoginAndLogoutNotifyOnEdtWithoutLosingTransitions() throws Exception {
    UserModel model = new UserModel(UserStore.load(tempDir.resolve("users.json")), null);
    IUserType user = new MicrosoftUser("00000000000000000000000000000000", "OfflinePlayer");
    List<IUserType> notifications = new ArrayList<>();
    List<Boolean> notificationThreads = new ArrayList<>();
    model.addAuthListener(
        changedUser -> {
          notifications.add(changedUser);
          notificationThreads.add(SwingUtilities.isEventDispatchThread());
        });

    ExecutorService authentication = Executors.newSingleThreadExecutor();
    try {
      SwingUtilities.invokeAndWait(
          () -> {
            try {
              // Hold the EDT while both transitions occur: listeners must not mutate its UI yet.
              authentication
                  .submit(
                      () -> {
                        model.setCurrentUser(user);
                        model.setCurrentUser(null);
                      })
                  .get(5, TimeUnit.SECONDS);
            } catch (Exception e) {
              throw new AssertionError(e);
            }
            assertTrue(notifications.isEmpty(), "Authentication callbacks must wait for the EDT");
          });
      SwingUtilities.invokeAndWait(
          () -> {
            assertEquals(Arrays.asList(user, null), notifications);
            assertEquals(Arrays.asList(true, true), notificationThreads);
          });
    } finally {
      authentication.shutdownNow();
      SwingUtilities.invokeAndWait(() -> {});
    }
  }

  @Test
  void logoutOnEdtNotifiesListenersBeforeReturning() throws Exception {
    UserModel model = new UserModel(null, null);
    List<IUserType> notifications = new ArrayList<>();
    model.addAuthListener(notifications::add);

    SwingUtilities.invokeAndWait(
        () -> {
          model.setCurrentUser(null);
          assertEquals(Arrays.asList((IUserType) null), notifications);
        });
  }
}
