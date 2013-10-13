package net.technicpack.launchercore.util;

import java.io.IOException;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

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
