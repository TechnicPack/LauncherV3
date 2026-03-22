package net.technicpack.ui.controls.installation;

import java.awt.CardLayout;
import java.awt.Dimension;
import javax.swing.JPanel;

public class InstallationProgressSlot extends JPanel {
  private static final String EMPTY_CARD = "empty";
  private static final String PROGRESS_CARD = "progress";

  private final CardLayout cardLayout;
  private final InstallationProgressDisplay progressDisplay;
  private boolean progressVisible;

  public InstallationProgressSlot(InstallationProgressDisplay progressDisplay, int preferredWidth) {
    this.progressDisplay = progressDisplay;
    this.cardLayout = new CardLayout();

    int preferredHeight = Math.max(progressDisplay.getExpandedPreferredHeight(), 46);
    Dimension preferredSize = new Dimension(preferredWidth, preferredHeight);

    setOpaque(false);
    setLayout(cardLayout);
    setMinimumSize(preferredSize);
    setPreferredSize(preferredSize);
    setMaximumSize(preferredSize);

    JPanel emptyPanel = new JPanel();
    emptyPanel.setOpaque(false);

    add(emptyPanel, EMPTY_CARD);
    add(progressDisplay, PROGRESS_CARD);

    hideProgress();
  }

  public void showProgress() {
    progressVisible = true;
    progressDisplay.setVisible(true);
    cardLayout.show(this, PROGRESS_CARD);
  }

  public void hideProgress() {
    progressVisible = false;
    progressDisplay.setVisible(false);
    cardLayout.show(this, EMPTY_CARD);
  }

  public boolean isProgressVisible() {
    return progressVisible;
  }
}
