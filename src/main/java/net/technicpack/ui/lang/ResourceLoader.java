/*
 * This file is part of Technic UI Core.
 * Copyright ©2015 Syndicate, LLC
 *
 * Technic UI Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic UI Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic UI Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.ui.lang;

import net.technicpack.launchercore.install.LauncherDirectories;
import net.technicpack.utilslib.Utils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.util.*;
import java.util.logging.Level;

public class ResourceLoader {
    private Collection<IRelocalizableResource> resources = new LinkedList<IRelocalizableResource>();
    private ResourceBundle stringData;
    private Locale currentLocale;
    private String dottedResourcePath;
    private String slashResourcePath;
    private boolean isDefaultLocaleSupported = true;
    private Locale defaultLocale;
    private File launcherAssets;
    private Locale[] locales = { Locale.ENGLISH };

    public static final String DEFAULT_LOCALE = "default";

    public static final String FONT_OPENSANS = "font.opensans.regular";
    public static final String FONT_RALEWAY = "font.raleway.light";

    public static final Map<String, Font> fontCache = new HashMap<String, Font>();

    public static final Font fallbackFont = new Font("Arial", Font.PLAIN, 12);

    public void setSupportedLanguages(Locale[] locales) {
        this.locales = locales;
    }

    public Font getFontByName(String fontName) {
        Font font;

        if (fontCache.containsKey(fontName))
            return fontCache.get(fontName);

        if (launcherAssets == null)
            return fallbackFont;

        InputStream fontStream = null;
        try {
            String fullName = getString(fontName);
            fontStream = FileUtils.openInputStream(new File(launcherAssets, fullName));

            if (fontStream == null)
                return fallbackFont;

            font = Font.createFont(Font.TRUETYPE_FONT, fontStream);
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback
            return fallbackFont;
        } finally {
            if (fontStream != null)
                IOUtils.closeQuietly(fontStream);
        }
        fontCache.put(fontName, font);

        if (font == null)
            return fallbackFont;
        
        return font;
    }

    public ResourceLoader(LauncherDirectories directories, String... resourcesPath) {
        if (directories == null)
            this.launcherAssets = null;
        else
            this.launcherAssets = new File(directories.getAssetsDirectory(), "launcher");
        dottedResourcePath = "";
        slashResourcePath = "";

        for (String pathToken : resourcesPath) {
            dottedResourcePath += pathToken + ".";
            slashResourcePath += "/" + pathToken;
        }

        Locale defaultLocale = Locale.getDefault();
        this.defaultLocale = matchClosestSupportedLocale(defaultLocale);

        if(!this.defaultLocale.getLanguage().equals(defaultLocale.getLanguage()))
            isDefaultLocaleSupported = false;
    }

    public ResourceLoader(ResourceLoader resourceLoader) {
        this.dottedResourcePath = resourceLoader.dottedResourcePath;
        this.slashResourcePath = resourceLoader.slashResourcePath;
        this.defaultLocale = resourceLoader.defaultLocale;
        this.isDefaultLocaleSupported = resourceLoader.isDefaultLocaleSupported;
        this.stringData = resourceLoader.stringData;
        this.currentLocale = resourceLoader.currentLocale;
    }

    public boolean isDefaultLocaleSupported() {
        return this.isDefaultLocaleSupported;
    }

    public void setLocale(Locale locale) {
        currentLocale = locale;
        stringData = ResourceBundle.getBundle(getBundlePath("lang.UIText"), locale);
        relocalizeResources();
    }

    public void setLocale(String locale) {
        setLocale(getLocaleFromCode(locale));
    }

    public ResourceLoader getVariant(Locale locale) {
        ResourceLoader variant = new ResourceLoader(this);
        variant.setSupportedLanguages(locales);
        variant.setLocale(locale);
        return variant;
    }

    public String getCurrentLocaleCode() {
        return getCodeFromLocale(currentLocale);
    }

    public String getString(String stringKey, String... replacements) {
        String outString = stringData.getString(stringKey);
        try {
            outString = new String(outString.getBytes("ISO-8859-1"), "UTF-8");

            for (int i = 0; i < replacements.length; i++) {
                String find = String.format("{%d}", i);
                String replace = replacements[i];

                if (outString.contains(find)) {
                    outString = outString.replace(find, replace);
                }
            }

            return outString;
        } catch (UnsupportedEncodingException ex) {
            ex.printStackTrace();
            return null;
        }
    }

    public String getLauncherBuild() {
        String build = "0";
        try {
            build = IOUtils.toString(ResourceLoader.class.getResource(getResourcePath("/version")).openStream(), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return build;
    }

    public String getCodeFromLocale(Locale locale) {
        if (locale.getLanguage().isEmpty()) {
            return "default";
        } else if (locale.getCountry().isEmpty()) {
            return locale.getLanguage();
        } else if (locale.getVariant().isEmpty()) {
            return String.format("%s,%s",locale.getLanguage(),locale.getCountry());
        } else {
            return String.format("%s,%s,%s", locale.getLanguage(), locale.getCountry(), locale.getVariant());
        }
    }

    public Locale getLocaleFromCode(String localeCode) {
        if (localeCode == null || localeCode.isEmpty() || localeCode.equals(DEFAULT_LOCALE)) {
            return defaultLocale;
        }

        String[] results = localeCode.split(",");
        String language = "";
        String country = "";
        String variant = "";

        if (results.length > 0) {
            language = results[0];
        }

        if (results.length > 1) {
            country = results[1];
        }

        if (results.length > 2) {
            variant = results[2];
        }

        Locale definiteLocale = new Locale(language,country,variant);

        return matchClosestSupportedLocale(definiteLocale);
    }

    private Locale matchClosestSupportedLocale(Locale definiteLocale) {
        Locale bestSupportedLocale = null;
        int bestLocaleScore = 0;
        for (int i = 0; i < locales.length; i++) {
            Locale testLocale = locales[i];
            int testScore = 0;

            if (testLocale.getLanguage().equals(definiteLocale.getLanguage())) {
                testScore++;

                if (testLocale.getCountry().equals(definiteLocale.getCountry())) {
                    testScore++;

                    if (testLocale.getVariant().equals(definiteLocale.getVariant())) {
                        testScore++;
                    }
                }
            }

            if (testScore != 0 && testScore > bestLocaleScore) {
                bestLocaleScore = testScore;
                bestSupportedLocale = testLocale;
            }
        }

        if (bestSupportedLocale != null) {
            return bestSupportedLocale;
        } else {
            return Locale.getDefault();
        }
    }

    public ImageIcon getIcon(String iconName) {
        return new ImageIcon(ResourceLoader.class.getResource(getResourcePath("/" + iconName)));
    }

    public BufferedImage getImage(String imageName) {
        try {
            return ImageIO.read(ResourceLoader.class.getResourceAsStream(getResourcePath("/" + imageName)));
        } catch (IOException ex) {
            Utils.getLogger().log(Level.SEVERE, ex.getMessage(), ex);
            return null;
        }
    }

    public InputStream getResourceAsStream(String path) {
        return ResourceLoader.class.getResourceAsStream(getResourcePath(path));
    }

    public BufferedImage getCircleClippedImage(String imageName) {
        BufferedImage contentImage = getImage(imageName);
        return getCircleClippedImage(contentImage);
    }

    public BufferedImage colorImage(BufferedImage loadImg, Color color) {
        BufferedImage img = new BufferedImage(loadImg.getWidth(), loadImg.getHeight(),
                BufferedImage.TRANSLUCENT);
        Graphics2D graphics = img.createGraphics();

        graphics.setColor(color);
        graphics.drawImage(loadImg, null, 0, 0);
        graphics.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_IN, 1.0f));
        graphics.fillRect(0, 0, loadImg.getWidth(), loadImg.getHeight());

        graphics.dispose();
        return img;
    }

    public BufferedImage getCircleClippedImage(BufferedImage contentImage) {
        // copy the picture to an image with transparency capabilities
        BufferedImage outputImage = new BufferedImage(contentImage.getWidth(), contentImage.getHeight(), BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = (Graphics2D)outputImage.getGraphics();
        g2.drawImage(contentImage, 0, 0, null);

        // Create the area around the circle to cut out
        Area cutOutArea = new Area(new Rectangle(0, 0, outputImage.getWidth(), outputImage.getHeight()));

        int diameter = (outputImage.getWidth() < outputImage.getHeight())?outputImage.getWidth():outputImage.getHeight();
        cutOutArea.subtract(new Area(new Ellipse2D.Float((outputImage.getWidth() - diameter) / 2, (outputImage.getHeight() - diameter) / 2, diameter, diameter)));

        // Set the fill color to an opaque color
        g2.setColor(Color.WHITE);
        // Set the composite to clear pixels
        g2.setComposite(AlphaComposite.Clear);
        // Turn on antialiasing
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        // Clear the cut out area
        g2.fill(cutOutArea);

        // dispose of the graphics object
        g2.dispose();

        return outputImage;
    }

    public Font getFont(String name, float size) {
        return getFont(name,size,0);
    }

    public Font getFont(String name, float size, int style) {
        return getFontByName(name).deriveFont(style, size);
    }

    private void relocalizeResources() {
        for(IRelocalizableResource resource : resources) {
            resource.relocalize(this);
        }
    }
      
    private String getBundlePath(String bundle) {
        return dottedResourcePath + bundle;
    }

    private String getResourcePath(String resource) {
        return slashResourcePath + resource;
    }

    public void registerResource(IRelocalizableResource resource) {
        if (!resources.contains(resource))
            resources.add(resource);
    }

    public void unregisterResource(IRelocalizableResource resource) {
        if (resources.contains(resource))
            resources.remove(resource);
    }
}
