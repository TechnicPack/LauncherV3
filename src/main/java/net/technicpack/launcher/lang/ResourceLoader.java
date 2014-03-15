/*
 * This file is part of The Technic Launcher Version 3.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * The Technic Launcher is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * The Technic Launcher  is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with The Technic Launcher.  If not, see <http://www.gnu.org/licenses/>.
 */
package net.technicpack.launcher.lang;

import org.apache.commons.io.IOUtils;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.geom.Area;
import java.awt.geom.Ellipse2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.Collection;
import java.util.LinkedList;
import java.util.Locale;
import java.util.ResourceBundle;

public class ResourceLoader {
    private Collection<IRelocalizableResource> resources = new LinkedList<IRelocalizableResource>();
    private ResourceBundle stringData;
    private Locale currentLocale;

    public static final Locale[] SUPPORTED_LOCALES = { Locale.ENGLISH };
    public static final String DEFAULT_LOCALE = "default";

    public static final String FONT_OPENSANS_BOLD = "font.opensans.bold";
    public static final String FONT_OPENSANS = "font.opensans.regular";
    public static final String FONT_RALEWAY = "font.raleway.light";

    public void setLocale(Locale locale) {
        currentLocale = locale;
        stringData = ResourceBundle.getBundle(getBundlePath("lang.UIText"), locale);
        relocalizeResources();
    }

    public void setLocale(String locale) {
        setLocale(getLocaleFromCode(locale));
    }

    public String getCurrentLocaleCode() {
        return getCodeFromLocale(currentLocale);
    }

    public String getString(String stringKey, String... replacements) {
        String outString = stringData.getString(stringKey);

        for (int i = 0; i < replacements.length; i++) {
            String find = String.format("{%d}", i);
            String replace = replacements[i];

            if (outString.contains(find)) {
                outString = outString.replace(find, replace);
            }
        }

        return outString;
    }

    public String getLauncherBuild() {
        String build = "0";
        try {
            build = IOUtils.toString(ResourceLoader.class.getResource(getResourcePath("/version")).openStream(), "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "3."+build;
    }

    private String getCodeFromLocale(Locale locale) {
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

    private Locale getLocaleFromCode(String localeCode) {
        if (localeCode == null || localeCode.isEmpty() || localeCode.equals(DEFAULT_LOCALE)) {
            return Locale.getDefault();
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
        for (int i = 0; i < SUPPORTED_LOCALES.length; i++) {
            Locale testLocale = SUPPORTED_LOCALES[i];
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
            ex.printStackTrace();
            return null;
        }
    }

    public BufferedImage getCircleClippedImage(String imageName) {
        BufferedImage contentImage = getImage(imageName);

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
        Font font;
        try {
            font = Font.createFont(Font.TRUETYPE_FONT, ResourceLoader.class.getResourceAsStream(getResourcePath("/fonts/"+getString(name)))).deriveFont(size).deriveFont(style);
        } catch (Exception e) {
            e.printStackTrace();
            // Fallback
            font = new Font("Arial", Font.PLAIN, 12);
        }
        return font;
    }

    private void relocalizeResources() {
        for(IRelocalizableResource resource : resources) {
            resource.relocalize(this);
        }
    }
      
    private String getBundlePath(String bundle) {
        return "net.technicpack.launcher.resources." + bundle;
    }

    private String getResourcePath(String resource) {
        return "/net/technicpack/launcher/resources" + resource;
    }

    public void registerResource(IRelocalizableResource resource) {
        if (!resources.contains(resource))
            resources.add(resource);
    }
}
