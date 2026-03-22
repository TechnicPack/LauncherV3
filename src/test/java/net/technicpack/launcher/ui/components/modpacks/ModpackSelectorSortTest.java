package net.technicpack.launcher.ui.components.modpacks;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.technicpack.launcher.ui.controls.modpacks.ModpackWidget;
import org.junit.jupiter.api.Test;
import sun.misc.Unsafe;

class ModpackSelectorSortTest {
  @Test
  void sortPacksIgnoresNullWidgetsInsteadOfThrowing() throws Exception {
    ModpackSelector selector = allocateWithoutConstructor();
    setAllModpacks(selector, mapOfNullWidgets());

    List<ModpackWidget> sorted = invokeSortPacks(selector);

    assertTrue(sorted.isEmpty());
  }

  private static Map<String, ModpackWidget> mapOfNullWidgets() {
    Map<String, ModpackWidget> map = new HashMap<>();
    map.put("first", null);
    map.put("second", null);
    return map;
  }

  @SuppressWarnings("unchecked")
  private static List<ModpackWidget> invokeSortPacks(ModpackSelector selector) throws Exception {
    Method sortPacks = ModpackSelector.class.getDeclaredMethod("sortPacks");
    sortPacks.setAccessible(true);

    return assertDoesNotThrow(
        () -> {
          try {
            return (List<ModpackWidget>) sortPacks.invoke(selector);
          } catch (InvocationTargetException e) {
            throw e.getCause();
          }
        });
  }

  private static ModpackSelector allocateWithoutConstructor() throws Exception {
    Field field = Unsafe.class.getDeclaredField("theUnsafe");
    field.setAccessible(true);
    Unsafe unsafe = (Unsafe) field.get(null);
    return (ModpackSelector) unsafe.allocateInstance(ModpackSelector.class);
  }

  private static void setAllModpacks(
      ModpackSelector selector, Map<String, ModpackWidget> allModpacks) throws Exception {
    Field field = ModpackSelector.class.getDeclaredField("allModpacks");
    field.setAccessible(true);
    field.set(selector, allModpacks);
  }
}
