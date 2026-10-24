package net.technicpack.minecraftcore.launch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.technicpack.minecraftcore.MojangUtils;
import net.technicpack.minecraftcore.mojang.version.io.Rule;
import org.junit.jupiter.api.Test;

class LaunchOptionsTest {
  @Test
  void customWindowPassesConfiguredDimensions() {
    List<String> commands = commandsFor(WindowType.CUSTOM);

    assertEquals(Arrays.asList("--width", "1280", "--height", "720"), commands);
  }

  @Test
  void maximizedWindowPassesPositiveDimensions() {
    List<String> commands = commandsFor(WindowType.MAXIMIZED);

    assertEquals("--width", commands.get(0));
    assertTrue(Integer.parseInt(commands.get(1)) > 0);
    assertEquals("--height", commands.get(2));
    assertTrue(Integer.parseInt(commands.get(3)) > 0);
  }

  @Test
  void defaultAndFullscreenWindowsPassNoDimensions() {
    assertEquals(Collections.emptyList(), commandsFor(WindowType.DEFAULT));
    assertEquals(Collections.singletonList("--fullscreen"), commandsFor(WindowType.FULLSCREEN));
  }

  @Test
  void maximizedWindowEnablesCustomResolutionFeature() {
    List<Rule> rules =
        Collections.singletonList(
            MojangUtils.getGson()
                .fromJson(
                    "{\"action\":\"allow\",\"features\":{\"has_custom_resolution\":true}}",
                    Rule.class));

    assertTrue(Rule.isAllowable(rules, new Options(WindowType.MAXIMIZED), null));
    assertTrue(Rule.isAllowable(rules, new Options(WindowType.CUSTOM), null));
    assertFalse(Rule.isAllowable(rules, new Options(WindowType.DEFAULT), null));
    assertFalse(Rule.isAllowable(rules, new Options(WindowType.FULLSCREEN), null));
  }

  private static List<String> commandsFor(WindowType windowType) {
    LaunchCommandCollector commands = new LaunchCommandCollector();
    new LaunchOptions(null, null, new Options(windowType)).appendToCommands(commands);
    return commands.collect();
  }

  private static final class Options implements ILaunchOptions {
    private final WindowType windowType;

    private Options(WindowType windowType) {
      this.windowType = windowType;
    }

    @Override
    public String getClientId() {
      return "client-id";
    }

    @Override
    public WindowType getLaunchWindowType() {
      return windowType;
    }

    @Override
    public int getCustomWidth() {
      return 1280;
    }

    @Override
    public int getCustomHeight() {
      return 720;
    }

    @Override
    public boolean shouldUseStencilBuffer() {
      return true;
    }

    @Override
    public String getWrapperCommand() {
      return null;
    }

    @Override
    public String getJavaArgs() {
      return null;
    }

    @Override
    public boolean isUsingDefaultJavaArgs() {
      return true;
    }

    @Override
    public boolean shouldUseMojangJava() {
      return true;
    }
  }
}
