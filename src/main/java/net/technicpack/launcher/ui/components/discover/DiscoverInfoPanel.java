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

package net.technicpack.launcher.ui.components.discover;

import net.technicpack.launcher.io.LauncherFileSystem;
import net.technicpack.launcher.ui.components.modpacks.ModpackSelector;
import net.technicpack.platform.IPlatformApi;
import net.technicpack.ui.lang.ResourceLoader;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.IOUtils;
import org.w3c.dom.Document;
import org.xhtmlrenderer.event.DocumentListener;
import org.xhtmlrenderer.resource.XMLResource;
import org.xhtmlrenderer.simple.XHTMLPanel;
import org.xhtmlrenderer.swing.DelegatingUserAgent;
import org.xhtmlrenderer.swing.FSMouseListener;
import org.xhtmlrenderer.swing.ImageResourceLoader;
import org.xhtmlrenderer.swing.SwingReplacedElementFactory;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.EventQueue;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.logging.Level;

public class DiscoverInfoPanel extends JPanel {

    final private XHTMLPanel panel;
    final private LauncherFileSystem fileSystem;
    final private ResourceLoader resources;
    private ActionListener loadListener = null;

    public DiscoverInfoPanel(final ResourceLoader loader, String discoverUrl, final IPlatformApi platform, final LauncherFileSystem fileSystem, final ModpackSelector modpackSelector) {
        this.fileSystem = fileSystem;
        this.resources = loader;

        if (discoverUrl == null)
            discoverUrl = "https://api.technicpack.net/discover/";

        final String runnableAccessDiscover = discoverUrl;

        setLayout(new BorderLayout());
        this.panel = new XHTMLPanel();
        panel.setFont(resources.getFont(ResourceLoader.FONT_OPENSANS, 16));
        panel.setDefaultFontFromComponent(true);
        panel.addDocumentListener(new DocumentListener() {
            private boolean hasReloaded = false;

            @Override
            public void documentStarted() {
                // Unused
            }

            @Override
            public void documentLoaded() {
                triggerLoadListener();
            }

            @Override
            public void onLayoutException(Throwable throwable) {
                Utils.getLogger().log(Level.SEVERE, "Discover page layout exception", throwable);

                if (!hasReloaded) {
                    hasReloaded = true;

                    EventQueue.invokeLater(() -> panel.setDocument(getDiscoverDocumentFromResource(), runnableAccessDiscover));
                }
            }

            @Override
            public void onRenderException(Throwable throwable) {
                Utils.getLogger().log(Level.SEVERE, "Discover page render exception", throwable);

                if (!hasReloaded) {
                    hasReloaded = true;

                    EventQueue.invokeLater(() -> panel.setDocument(getDiscoverDocumentFromResource(), runnableAccessDiscover));
                }
            }
        });

        for (FSMouseListener listener : panel.getMouseTrackingListeners()) {
            panel.removeMouseTrackingListener(listener);
        }
        panel.addMouseTrackingListener(new DiscoverLinkListener(platform, modpackSelector));

        final DelegatingUserAgent uac = new DelegatingUserAgent();
        ImageResourceLoader imageLoader = new DiscoverResourceLoader();
        imageLoader.setRepaintListener(panel);
        imageLoader.clear();
        uac.setImageResourceLoader(imageLoader);
        panel.getSharedContext().getTextRenderer().setSmoothingThreshold(6.0f);
        panel.getSharedContext().setUserAgentCallback(uac);

        SwingReplacedElementFactory factory = new SwingReplacedElementFactory(panel, imageLoader);
        factory.reset();
        panel.getSharedContext().setReplacedElementFactory(factory);
        panel.getSharedContext().setFontMapping("Raleway", resources.getFont(ResourceLoader.FONT_RALEWAY, 12));

        EventQueue.invokeLater(() -> {
            try {
                Path localCache = fileSystem.getCacheDirectory().resolve("discover.html");
                panel.setDocument(getDiscoverDocument(runnableAccessDiscover, localCache), runnableAccessDiscover);
            } catch (Exception e) {
                //Can't load document from internet- don't beef
                e.printStackTrace();

                triggerLoadListener();
            }
        });

        add(panel, BorderLayout.CENTER);
    }

    public void setLoadListener(ActionListener listener) {
        this.loadListener = listener;
    }

    protected void triggerLoadListener() {
        final ActionListener deferredListener = loadListener;
        if (deferredListener != null) {
            EventQueue.invokeLater(new Runnable() {
                @Override
                public void run() {
                    deferredListener.actionPerformed(new ActionEvent(this, 0, "loaded"));
                }
            });
            loadListener = null;
        }
    }

    protected Document getDiscoverDocument(String url, Path localCache) {
        //Attempt to retrieve the discover page from the live site, then a local cache, then an internal resource
        Document doc = getDiscoverDocumentFromLiveSite(url, localCache);
        if (doc != null)
            return doc;

        if (!Files.exists(localCache)) {
            doc = getDiscoverDocumentFromLocalCache(localCache);
            if (doc != null)
                return doc;
        }

        return getDiscoverDocumentFromResource();
    }

    protected Document getDiscoverDocumentFromLiveSite(String url, Path localCache) {
        try {
            HttpURLConnection conn = Utils.openHttpConnection(new URL(url));
            byte[] data;
            try (InputStream stream = conn.getInputStream()) {
                data = IOUtils.toByteArray(stream);
            }

            Document doc = XMLResource.load(new ByteArrayInputStream(data)).getDocument();
            if (doc != null) {
                try (OutputStream stream = Files.newOutputStream(localCache)) {
                    stream.write(data);
                }
                return doc;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected Document getDiscoverDocumentFromLocalCache(Path localCache) {
        try (InputStream stream = Files.newInputStream(localCache)) {
            return XMLResource.load(stream).getDocument();
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    protected Document getDiscoverDocumentFromResource() {
        return XMLResource.load(resources.getResourceAsStream("/discoverFallback.html")).getDocument();
    }
}
