package net.technicpack.ui.lang;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Locale;
import org.junit.jupiter.api.Test;

class ResourceLoaderUtf8BundleTest {

  @Test
  void loadsUtf8PropertiesOnJava8WithoutNative2Ascii() {
    ResourceLoader loader =
        new ResourceLoader(null, "net", "technicpack", "ui", "lang", "testbundles");
    loader.setSupportedLanguages(new Locale[] {Locale.ENGLISH, Locale.SIMPLIFIED_CHINESE});
    loader.setLocale(Locale.SIMPLIFIED_CHINESE);

    assertEquals("新闻", loader.getString("utf8.sample"));
  }
}
