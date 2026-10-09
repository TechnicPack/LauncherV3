package net.technicpack.ui;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import java.util.Locale;
import javax.swing.JComboBox;
import javax.swing.SwingUtilities;
import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.ui.listitems.LanguageItem;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class UIUtilsTest {
  @TempDir Path tempDir;

  @Test
  void explicitEnglishSurvivesSelectorRebuild() throws Exception {
    assertSelectionAfterRebuild("en", "en");
  }

  @Test
  void regionalLanguageSelectsItsOwnEntry() throws Exception {
    assertSelectionAfterRebuild("pt,BR", "pt,BR");
  }

  @Test
  void systemDefaultRemainsDistinctFromResolvedLanguage() throws Exception {
    assertSelectionAfterRebuild(ResourceLoader.DEFAULT_LOCALE, ResourceLoader.DEFAULT_LOCALE);
  }

  private void assertSelectionAfterRebuild(String savedCode, String expectedCode) throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          TechnicSettings settings = new TechnicSettings();
          settings.setFilePath(tempDir.resolve("settings.json").toFile());
          settings.setLanguageCode(savedCode);
          ResourceLoader resources =
              new ResourceLoader(null, "net", "technicpack", "launcher", "resources");
          resources.setSupportedLanguages(
              new Locale[] {Locale.ENGLISH, new Locale("pt", "PT"), new Locale("pt", "BR")});
          resources.setLocale(savedCode);
          JComboBox<LanguageItem> languages = new JComboBox<>();
          UIUtils.populateLanguageSelector("OS Default", languages, resources, settings);
          languages.removeAllItems();
          UIUtils.populateLanguageSelector("OS Default", languages, resources, settings);
          assertEquals(expectedCode, ((LanguageItem) languages.getSelectedItem()).getLangCode());
          assertEquals(savedCode, settings.getLanguageCode());
        });
  }
}
