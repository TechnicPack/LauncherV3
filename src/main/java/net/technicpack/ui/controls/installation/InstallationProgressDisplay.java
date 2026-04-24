package net.technicpack.ui.controls.installation;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.GridLayout;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import net.technicpack.launchercore.progress.CurrentItemMode;
import net.technicpack.launchercore.progress.ExecutionProgressListener;
import net.technicpack.launchercore.util.DownloadListener;

public class InstallationProgressDisplay extends JPanel
    implements ExecutionProgressListener, DownloadListener {
  private final ProgressBar overallProgressBar;
  private final JPanel currentItemRow;
  private final JPanel currentItemCenter;
  private final JLabel currentItemCaptionLabel;
  private final InlineProgressStrip currentItemProgressBar;
  private final EllipsizedLabel currentItemNameLabel;
  private volatile long uiUpdateGeneration;

  public InstallationProgressDisplay() {
    setOpaque(false);
    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));

    overallProgressBar = new ProgressBar();
    overallProgressBar.setAlignmentX(LEFT_ALIGNMENT);
    overallProgressBar.setMinimumSize(new Dimension(0, 31));
    overallProgressBar.setPreferredSize(new Dimension(360, 31));
    overallProgressBar.setMaximumSize(new Dimension(Integer.MAX_VALUE, 31));

    currentItemRow = new JPanel();
    currentItemRow.setOpaque(false);
    currentItemRow.setLayout(new BorderLayout(10, 0));
    currentItemRow.setBorder(BorderFactory.createEmptyBorder(4, 20, 0, 20));
    currentItemRow.setAlignmentX(LEFT_ALIGNMENT);
    currentItemRow.setMinimumSize(new Dimension(0, 18));
    currentItemRow.setPreferredSize(new Dimension(360, 18));
    currentItemRow.setMaximumSize(new Dimension(Integer.MAX_VALUE, 18));

    currentItemCaptionLabel = new JLabel("Current item");
    currentItemCaptionLabel.setOpaque(false);
    currentItemCaptionLabel.setPreferredSize(new Dimension(78, 14));

    currentItemProgressBar = new InlineProgressStrip();
    currentItemProgressBar.setPreferredSize(new Dimension(0, 10));

    currentItemNameLabel = new EllipsizedLabel("");
    currentItemNameLabel.setOpaque(false);
    currentItemNameLabel.setMinimumSize(new Dimension(0, 14));
    currentItemNameLabel.setPreferredSize(new Dimension(0, 14));

    currentItemCenter = new JPanel();
    currentItemCenter.setOpaque(false);
    currentItemCenter.setLayout(new GridLayout(1, 2, 12, 0));
    currentItemCenter.add(currentItemProgressBar);
    currentItemCenter.add(currentItemNameLabel);

    currentItemRow.add(currentItemCaptionLabel, BorderLayout.WEST);
    currentItemRow.add(currentItemCenter, BorderLayout.CENTER);
    currentItemRow.setVisible(false);

    add(overallProgressBar);
    add(currentItemRow);
  }

  public ProgressBar getOverallProgressBar() {
    return overallProgressBar;
  }

  public JPanel getCurrentItemRow() {
    return currentItemRow;
  }

  public JLabel getCurrentItemCaptionLabel() {
    return currentItemCaptionLabel;
  }

  public InlineProgressStrip getCurrentItemProgressBar() {
    return currentItemProgressBar;
  }

  public JLabel getCurrentItemNameLabel() {
    return currentItemNameLabel;
  }

  public JLabel getCurrentItemLabel() {
    return currentItemNameLabel;
  }

  public int getExpandedPreferredHeight() {
    return overallProgressBar.getPreferredSize().height + currentItemRow.getPreferredSize().height;
  }

  public void configureForSplash() {
    configureForSplash(220);
  }

  public void configureForSplash(int preferredWidth) {
    setComponentHeight(overallProgressBar, preferredWidth, 24);
    currentItemRow.setBorder(BorderFactory.createEmptyBorder(3, 0, 0, 0));
    // Row = 3 (top border) + 15 (label content) = 18, enough to fit descenders on the 11pt
    // label font (getFont().getSize() = 11, measured height ~15).
    setComponentHeight(currentItemRow, preferredWidth, 18);
    // Keep the row visible from the start so pack() reserves its 18px. If the row were hidden
    // during pack and later revealed during the update (e.g., when asset download starts), the
    // splash frame would stay its packed size and the BorderLayout would compensate by
    // shrinking CENTER — which clips the splash image top/bottom. Showing it now means the
    // mini progress bar sits empty (0%, blank label) until something drives it.
    currentItemRow.setVisible(true);
    currentItemCaptionLabel.setVisible(false);
    currentItemCaptionLabel.setPreferredSize(new Dimension(0, 0));
    currentItemCenter.removeAll();
    currentItemCenter.setLayout(new BorderLayout(8, 0));
    currentItemProgressBar.setMinimumSize(new Dimension(84, 8));
    currentItemProgressBar.setPreferredSize(new Dimension(84, 8));
    currentItemProgressBar.setMaximumSize(new Dimension(84, 8));
    currentItemNameLabel.setMinimumSize(new Dimension(0, 15));
    currentItemNameLabel.setPreferredSize(new Dimension(0, 15));
    currentItemCenter.add(currentItemProgressBar, BorderLayout.WEST);
    currentItemCenter.add(currentItemNameLabel, BorderLayout.CENTER);
    revalidate();
  }

  @Override
  public void overallChanged(String label, float percent) {
    final long generation = uiUpdateGeneration;
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> applyOverallChanged(label, percent, generation));
      return;
    }

    applyOverallChanged(label, percent, generation);
  }

  @Override
  public void currentItemChanged(String label, CurrentItemMode mode, Float percent) {
    final long generation = uiUpdateGeneration;
    if (!SwingUtilities.isEventDispatchThread()) {
      SwingUtilities.invokeLater(() -> applyCurrentItemChanged(label, mode, percent, generation));
      return;
    }

    applyCurrentItemChanged(label, mode, percent, generation);
  }

  private void applyOverallChanged(String label, float percent, long generation) {
    if (generation != uiUpdateGeneration) {
      return;
    }
    overallProgressBar.setProgress(label, percent);
  }

  private void applyCurrentItemChanged(
      String label, CurrentItemMode mode, Float percent, long generation) {
    if (generation != uiUpdateGeneration) {
      return;
    }

    switch (mode) {
      case DETERMINATE:
        currentItemRow.setVisible(true);
        currentItemNameLabel.setText(label);
        currentItemProgressBar.setProgress(percent == null ? 0.0f : percent);
        break;
      case INDETERMINATE:
        currentItemRow.setVisible(true);
        currentItemNameLabel.setText(label);
        currentItemProgressBar.setIndeterminate(true);
        break;
      case IDLE:
      default:
        resetCurrentItem(false);
        break;
    }
  }

  @Override
  public void stateChanged(String fileName, float progress) {
    overallChanged(fileName, progress);
  }

  @Override
  public void setVisible(boolean aFlag) {
    uiUpdateGeneration++;
    if (!aFlag) {
      resetCurrentItem(true);
    }
    super.setVisible(aFlag);
  }

  private void resetCurrentItem(boolean clearRowVisibility) {
    currentItemNameLabel.setText("");
    currentItemProgressBar.reset();
    if (clearRowVisibility) {
      currentItemRow.setVisible(false);
    } else {
      currentItemRow.setVisible(true);
    }
  }

  private static void setComponentHeight(JPanel component, int preferredWidth, int height) {
    component.setMinimumSize(new Dimension(0, height));
    component.setPreferredSize(new Dimension(preferredWidth, height));
    component.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
  }

  private static void setComponentHeight(ProgressBar component, int preferredWidth, int height) {
    component.setMinimumSize(new Dimension(0, height));
    component.setPreferredSize(new Dimension(preferredWidth, height));
    component.setMaximumSize(new Dimension(Integer.MAX_VALUE, height));
  }

  private static class EllipsizedLabel extends JLabel {
    private static final int MAX_PREFERRED_WIDTH = 240;
    private String fullText;

    private EllipsizedLabel(String text) {
      super(text == null ? "" : text);
      this.fullText = text == null ? "" : text;
      // Update the ellipsized text whenever the label is resized — this is what replaces the
      // previous paintComponent-time setText dance that caused a PropertyChange feedback loop.
      addComponentListener(
          new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent e) {
              updateDisplayText();
            }
          });
    }

    @Override
    public void setText(String text) {
      fullText = text == null ? "" : text;
      setToolTipText(fullText.isEmpty() ? null : fullText);
      updateDisplayText();
    }

    @Override
    public String getText() {
      return fullText;
    }

    private void updateDisplayText() {
      int width = getWidth();
      if (width <= 0 || getFont() == null) {
        super.setText(fullText);
        return;
      }
      super.setText(fitText(fullText, getFontMetrics(getFont()), width));
    }

    @Override
    public Dimension getPreferredSize() {
      // Base the preferred size on the full untruncated text, capped at MAX_PREFERRED_WIDTH.
      // Not on the currently-displayed (possibly truncated) text, because that would cause
      // layout to collapse the label once the ellipsized version fits, then re-expand, etc.
      FontMetrics metrics = getFontMetrics(getFont());
      Dimension preferredSize = super.getPreferredSize();
      if (metrics != null && fullText != null) {
        int fullWidth = metrics.stringWidth(fullText);
        preferredSize.width = Math.min(MAX_PREFERRED_WIDTH, fullWidth);
      } else {
        preferredSize.width = Math.min(preferredSize.width, MAX_PREFERRED_WIDTH);
      }
      return preferredSize;
    }

    private static String fitText(String text, FontMetrics metrics, int width) {
      if (text == null || text.isEmpty() || width <= 0) {
        return "";
      }

      if (metrics.stringWidth(text) <= width) {
        return text;
      }

      String ellipsis = "...";
      int ellipsisWidth = metrics.stringWidth(ellipsis);
      int targetWidth = Math.max(0, width - ellipsisWidth);

      int length = text.length();
      while (length > 0 && metrics.stringWidth(text.substring(0, length)) > targetWidth) {
        length--;
      }

      if (length <= 0) {
        return ellipsis;
      }

      return text.substring(0, length) + ellipsis;
    }
  }
}
