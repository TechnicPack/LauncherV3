package net.technicpack.ui.controls.installation;

import net.technicpack.launchercore.progress.CurrentItemMode;
import net.technicpack.launchercore.progress.ExecutionProgressListener;
import net.technicpack.launchercore.util.DownloadListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GridLayout;

public class InstallationProgressDisplay extends JPanel implements ExecutionProgressListener, DownloadListener {
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
        setComponentHeight(currentItemRow, preferredWidth, 14);
        currentItemRow.setVisible(false);
        currentItemCaptionLabel.setVisible(false);
        currentItemCaptionLabel.setPreferredSize(new Dimension(0, 0));
        currentItemCenter.removeAll();
        currentItemCenter.setLayout(new BorderLayout(8, 0));
        currentItemProgressBar.setMinimumSize(new Dimension(84, 8));
        currentItemProgressBar.setPreferredSize(new Dimension(84, 8));
        currentItemProgressBar.setMaximumSize(new Dimension(84, 8));
        currentItemNameLabel.setMinimumSize(new Dimension(0, 12));
        currentItemNameLabel.setPreferredSize(new Dimension(0, 12));
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

    private void applyCurrentItemChanged(String label, CurrentItemMode mode, Float percent, long generation) {
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
            super(text);
            this.fullText = text;
        }

        @Override
        public void setText(String text) {
            fullText = text == null ? "" : text;
            setToolTipText(fullText.isEmpty() ? null : fullText);
            super.setText(fullText);
        }

        @Override
        public String getText() {
            return fullText;
        }

        @Override
        protected void paintComponent(Graphics graphics) {
            String displayText = fitText(fullText, getFontMetrics(getFont()), getWidth());
            super.setText(displayText);
            super.paintComponent(graphics);
            super.setText(fullText);
        }

        @Override
        public Dimension getPreferredSize() {
            Dimension preferredSize = super.getPreferredSize();
            preferredSize.width = Math.min(preferredSize.width, MAX_PREFERRED_WIDTH);
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
