/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright ©2015 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with the Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.launcher.ui.components.news;

import net.technicpack.launcher.ui.UIConstants;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.ui.controls.RoundedButton;
import net.technicpack.ui.controls.list.SimpleScrollbarUI;
import net.technicpack.launchercore.image.ImageRepository;
import net.technicpack.platform.io.AuthorshipInfo;
import net.technicpack.platform.io.NewsArticle;
import net.technicpack.utilslib.DesktopUtils;

import javax.swing.*;
import javax.swing.event.HyperlinkEvent;
import java.awt.*;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

public class NewsInfoPanel extends JPanel implements PropertyChangeListener {
    private ResourceLoader resources;
    private ImageRepository<AuthorshipInfo> avatarRepo;

    JTextPane newsText;
    JScrollPane newsScroller;
    AuthorshipWidget authorshipInfo;
    JLabel title;

    private String url = "";

    public NewsInfoPanel(ResourceLoader resources, ImageRepository<AuthorshipInfo> avatarRepo) {

        this.resources = resources;
        this.avatarRepo = avatarRepo;

        initComponents();
    }

    public void setArticle(NewsArticle article) {
        if (article == null) {
            newsText.setText("");
            url = "";
            return;
        }

        title.setText(article.getTitle());
        url = article.getUrl();
        newsText.setText("<html><head><style type=\"text/css\">a{color:#309aeb}body{font-family:\"" + newsText.getFont().getFamily() + "\";color:#D0D0D0}</style></head><body>"+
            article.getContent() +
            "</body></html>");

        authorshipInfo.setAuthorshipInfo(article.getAuthorshipInfo(), avatarRepo.startImageJob(article.getAuthorshipInfo()));

        EventQueue.invokeLater(() -> newsText.scrollRectToVisible(new Rectangle(0, 0, 1, 1)));
    }

    protected void visitCurrentItem() {
        if (url != null && !url.isEmpty()) {
            DesktopUtils.browseUrl(url);
        }
    }

    private void initComponents() {
        setLayout(new GridBagLayout());
        setBorder(BorderFactory.createEmptyBorder(20,20,18,16));
        setBackground(UIConstants.COLOR_CENTRAL_BACK_OPAQUE);

        title = new JLabel("");
        title.setForeground(UIConstants.COLOR_WHITE_TEXT);
        title.setFont(resources.getFont(ResourceLoader.FONT_RALEWAY, 26));
        title.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));

        title.addMouseListener(new MouseListener() {
            @Override
            public void mouseClicked(MouseEvent e) {
                visitCurrentItem();
            }

            @Override
            public void mousePressed(MouseEvent e) {

            }

            @Override
            public void mouseReleased(MouseEvent e) {

            }

            @Override
            public void mouseEntered(MouseEvent e) {

            }

            @Override
            public void mouseExited(MouseEvent e) {

            }
        });

        add(title, new GridBagConstraints(0, 0, 2, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.HORIZONTAL, new Insets(0, 0, 0, 0), 0, 0));

        authorshipInfo = new AuthorshipWidget(resources);
        add(authorshipInfo, new GridBagConstraints(0, 1, 2, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 8, 0), 0, 0));

        newsText = new JTextPane();
        newsText.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        newsText.setOpaque(false);
        newsText.setForeground(UIConstants.COLOR_WHITE_TEXT);
        newsText.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        newsText.setEditable(false);
        newsText.setHighlighter(null);
        newsText.setAlignmentX(LEFT_ALIGNMENT);
        newsText.setContentType("text/html");
        newsText.addHyperlinkListener(e -> {
            if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                if (e.getURL() != null)
                    DesktopUtils.browseUrl(e.getURL().toString());
            }
        });
        newsText.addComponentListener(new ComponentListener() {
            @Override
            public void componentResized(ComponentEvent e) {
                NewsInfoPanel.this.revalidate();
            }

            @Override
            public void componentMoved(ComponentEvent e) {

            }

            @Override
            public void componentShown(ComponentEvent e) {

            }

            @Override
            public void componentHidden(ComponentEvent e) {

            }
        });

        newsScroller = new JScrollPane(newsText, JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        newsScroller.getVerticalScrollBar().setUI(new SimpleScrollbarUI(UIConstants.COLOR_SCROLL_TRACK, UIConstants.COLOR_SCROLL_THUMB));
        newsScroller.getVerticalScrollBar().setPreferredSize(new Dimension(10, 10));
        newsScroller.setBorder(BorderFactory.createEmptyBorder());
        newsScroller.setMaximumSize(new Dimension(32000,900));
        newsScroller.setOpaque(false);
        newsScroller.getViewport().setOpaque(false);

        JPanel newsTextPanel = new JPanel();
        newsTextPanel.setLayout(new BoxLayout(newsTextPanel, BoxLayout.PAGE_AXIS));
        newsTextPanel.setOpaque(false);
        newsTextPanel.add(newsScroller);
        newsTextPanel.add(Box.createVerticalGlue());

        add(newsTextPanel, new GridBagConstraints(0, 2, 2, 1, 1.0, 1.0, GridBagConstraints.NORTHWEST, GridBagConstraints.BOTH, new Insets(0, 0, 10, 0), 0, 0));

        add(Box.createGlue(), new GridBagConstraints(0, 3, 1, 1, 1.0, 0.0, GridBagConstraints.NORTHWEST, GridBagConstraints.NONE, new Insets(0, 0, 0, 0), 0, 0));

        RoundedButton discussButton = new RoundedButton(resources.getString("launcher.news.discuss"));
        discussButton.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        discussButton.setBorder(BorderFactory.createEmptyBorder(5, 17, 10, 17));
        discussButton.setForeground(UIConstants.COLOR_BUTTON_BLUE);
        discussButton.setHoverForeground(UIConstants.COLOR_BLUE);
        discussButton.setAlignmentX(RIGHT_ALIGNMENT);
        discussButton.setContentAreaFilled(false);
        discussButton.addActionListener(e -> visitCurrentItem());
        add(discussButton, new GridBagConstraints(1, 3, 1, 1, 0.0, 0.0, GridBagConstraints.SOUTHEAST, GridBagConstraints.BOTH, new Insets(0, 0, 0, 0), 0, 0));
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
        invalidate();
    }
}
