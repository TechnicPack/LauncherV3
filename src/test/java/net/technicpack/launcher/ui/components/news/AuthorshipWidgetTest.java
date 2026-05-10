package net.technicpack.launcher.ui.components.news;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.Locale;
import net.technicpack.ui.lang.ResourceLoader;
import org.junit.jupiter.api.Test;

class AuthorshipWidgetTest {

  @Test
  void dateTextUsesLargestElapsedCalendarUnit() throws Exception {
    AuthorshipWidget widget = new AuthorshipWidget(resources());

    assertEquals(
        "2 Hours Ago", getDateText(widget, Date.from(Instant.now().minus(2, ChronoUnit.HOURS))));
    assertEquals(
        "2 Days Ago", getDateText(widget, Date.from(Instant.now().minus(2, ChronoUnit.DAYS))));
  }

  @Test
  void dateTextReportsYearsAgoForOldPosts() throws Exception {
    AuthorshipWidget widget = new AuthorshipWidget(resources());

    Date twoYearsAgo = Date.from(ZonedDateTime.now().minusYears(2).toInstant());
    assertEquals("2 Years Ago", getDateText(widget, twoYearsAgo));
  }

  private static ResourceLoader resources() {
    ResourceLoader loader = new ResourceLoader(null, "net", "technicpack", "launcher", "resources");
    loader.setSupportedLanguages(new Locale[] {Locale.ENGLISH});
    loader.setLocale(Locale.ENGLISH);
    return loader;
  }

  private static String getDateText(AuthorshipWidget widget, Date date) throws Exception {
    Method method = AuthorshipWidget.class.getDeclaredMethod("getDateText", Date.class);
    method.setAccessible(true);
    try {
      return (String) method.invoke(widget, date);
    } catch (InvocationTargetException e) {
      Throwable cause = e.getCause();
      if (cause instanceof Exception) {
        throw (Exception) cause;
      }
      throw e;
    }
  }
}
