/**
 * This file is part of Technic Launcher Core.
 * Copyright (C) 2013 Syndicate, LLC
 *
 * Technic Launcher Core is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Technic Launcher Core is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License,
 * as well as a copy of the GNU Lesser General Public License,
 * along with Technic Launcher Core.  If not, see <http://www.gnu.org/licenses/>.
 */

package net.technicpack.utilslib;

import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;

public class XMLUtils {
    static DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
    static DocumentBuilder docBuilder;

    public static Document getXMLFromURL(String url) {
        Document xmlDoc = null;

        try {
            docBuilder = builderFactory.newDocumentBuilder();
            xmlDoc = docBuilder.parse(url);
        } catch (ParserConfigurationException error) {
            System.err.println("Error reading parser configuration.");
            return xmlDoc;
        } catch (SAXException error) {
            System.err.println("Error parsing document.");
            return xmlDoc;
        } catch (IOException error) {
            System.err.println("Error accessing URL.");
            return xmlDoc;
        }

        xmlDoc.getDocumentElement().normalize();
        return xmlDoc;
    }
}
