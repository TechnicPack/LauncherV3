package net.technicpack.launcher.ui.components.modpacks;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.technicpack.launcher.ui.controls.modpacks.ModpackWidget;
import org.junit.jupiter.api.Test;

class ModpackSelectorSortTest {
  @Test
  void sortPacksIgnoresNullWidgetsInsteadOfThrowing() {
    List<ModpackWidget> sorted = ModpackSelector.sortPacks(mapOfNullWidgets().values());

    assertTrue(sorted.isEmpty());
  }

  private static Map<String, ModpackWidget> mapOfNullWidgets() {
    Map<String, ModpackWidget> map = new HashMap<>();
    map.put("first", null);
    map.put("second", null);
    return map;
  }
}
