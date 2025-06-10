package net.technicpack.ui;

import net.technicpack.launcher.settings.TechnicSettings;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.ui.listitems.LanguageItem;

import javax.swing.JComboBox;
import java.util.List;
import java.util.Locale;

public class UIUtils {
    private UIUtils() {
        // Prevent instantiation of utility class
    }

    /**
     * Populates a JComboBox with language options, including the system default locale.
     *
     * @param defaultLocaleText The text to display for the system default locale.
     * @param languages         The JComboBox to populate with LanguageItem objects.
     * @param resources         The ResourceLoader instance to fetch language resources.
     * @param settings          The TechnicSettings instance to retrieve the user's preferred language code.
     */
    public static void populateLanguageSelector(String defaultLocaleText, JComboBox<LanguageItem> languages, ResourceLoader resources, TechnicSettings settings) {
        // Add the system default locale as the first item
        languages.addItem(new LanguageItem(ResourceLoader.DEFAULT_LOCALE, defaultLocaleText, resources));

        // Add all supported languages from the ResourceLoader
        List<Locale> supportedLanguages = resources.getSupportedLanguages();
        for (Locale locale : supportedLanguages) {
            languages.addItem(new LanguageItem(resources.getCodeFromLocale(locale), locale.getDisplayName(locale), resources.getVariant(locale)));
        }

        // Set the user's desired locale if it is not the default
        String settingsLanguageCode = settings.getLanguageCode();
        if (!settingsLanguageCode.equalsIgnoreCase(ResourceLoader.DEFAULT_LOCALE)) {
            Locale wantedLocale = resources.getLocaleFromCode(settingsLanguageCode);

            languages.setSelectedItem(wantedLocale);
        }
    }
}
