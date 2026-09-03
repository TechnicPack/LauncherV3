package net.technicpack.ui.components;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.Document;
import org.junit.jupiter.api.Test;

class ConsoleFrameTest {
  @Test
  void longUnbrokenLineWrapsWithinViewportWithoutChangingText() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          try {
            JTextPane textPane = ConsoleFrame.createConsoleTextPane();
            textPane.setFont(ConsoleFrame.getMonospaceFont());
            JScrollPane scrollPane = new JScrollPane(textPane);
            scrollPane.setSize(new Dimension(640, 200));

            String line = createLongLine();
            Document document = textPane.getDocument();
            document.insertString(0, line, null);
            scrollPane.doLayout();
            scrollPane.getViewport().doLayout();

            int viewportWidth = scrollPane.getViewport().getExtentSize().width;
            assertTrue(textPane.getScrollableTracksViewportWidth());
            assertEquals(viewportWidth, textPane.getWidth());
            assertFalse(scrollPane.getHorizontalScrollBar().isVisible());
            assertTrue(
                textPane.getPreferredSize().height
                    > textPane.getFontMetrics(textPane.getFont()).getHeight());
            assertEquals(line, textPane.getText());
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        });
  }

  private static String createLongLine() {
    StringBuilder line = new StringBuilder(10_000);
    for (int i = 0; i < 10_000; i++) {
      line.append('x');
    }
    return line.toString();
  }
}
