/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright (C) 2013 Syndicate, LLC
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

package net.technicpack.launcher.ui.components.discover;

import net.technicpack.platform.IPlatformApi;
import net.technicpack.platform.http.HttpPlatformApi;
import net.technicpack.ui.controls.installation.*;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.ui.controls.TiledBackground;
import org.w3c.dom.Document;
import org.xhtmlrenderer.event.DocumentListener;
import org.xhtmlrenderer.resource.ImageResource;
import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.swing.DelegatingUserAgent;
import org.xhtmlrenderer.swing.FSMouseListener;
import org.xhtmlrenderer.swing.ImageResourceLoader;
import org.xhtmlrenderer.swing.SwingReplacedElementFactory;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class DiscoverInfoPanel extends TiledBackground {

    final private XHTMLPanel panel;

    public DiscoverInfoPanel(ResourceLoader loader, String discoverUrl, IPlatformApi platform, final net.technicpack.ui.controls.installation.SplashScreen splash) {
        super(loader.getImage("background_repeat2.png"));

        if (discoverUrl == null)
            discoverUrl = "http://www.technicpack.net/api/discover/";

        final String runnableAccessDiscover = discoverUrl;

        setLayout(new BorderLayout());
        this.panel = new XHTMLPanel();
        panel.setFont(loader.getFont(ResourceLoader.FONT_OPENSANS, 16));
        panel.setDefaultFontFromComponent(true);
        panel.addDocumentListener(new DocumentListener() {
            @Override
            public void documentStarted() {

            }

            @Override
            public void documentLoaded() {
                splash.dispose();
            }

            @Override
            public void onLayoutException(Throwable throwable) {
                throwable.printStackTrace();
            }

            @Override
            public void onRenderException(Throwable throwable) {
                throwable.printStackTrace();
            }
        });

        for (Object listener : panel.getMouseTrackingListeners()) {
            panel.removeMouseTrackingListener((FSMouseListener)listener);
        }
        panel.addMouseTrackingListener(new DiscoverLinkListener(platform));

        DelegatingUserAgent uac = new DelegatingUserAgent();
        ImageResourceLoader imageLoader = new DiscoverResourceLoader();
        imageLoader.setRepaintListener(panel);
        imageLoader.clear();
        uac.setImageResourceLoader(imageLoader);
        panel.getSharedContext().getTextRenderer().setSmoothingThreshold(6.0f);
        panel.getSharedContext().setUserAgentCallback(uac);

        SwingReplacedElementFactory factory = new SwingReplacedElementFactory(panel, imageLoader);
        factory.reset();
        panel.getSharedContext().setReplacedElementFactory(factory);
        panel.getSharedContext().setFontMapping("Raleway", loader.getFont(ResourceLoader.FONT_RALEWAY, 12));

        EventQueue.invokeLater(new Runnable() {
            @Override
            public void run() {
                try {
                    panel.setDocument(runnableAccessDiscover);
                } catch (Exception ex) {
                    //Can't load document from internet- don't beef
                    ex.printStackTrace();

                    splash.dispose();
                }
            }
        });

        add(panel, BorderLayout.CENTER);
    }
}
