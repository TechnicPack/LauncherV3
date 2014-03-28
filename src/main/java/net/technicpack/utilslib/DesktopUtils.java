package net.technicpack.utilslib;

import java.awt.*;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

public class DesktopUtils {
    public static void browseUrl(String url) {
        try {
            Desktop.getDesktop().browse(new URI(url));
        } catch (IOException ex) {
            //Thrown by Desktop.browse() - just log & ignore
            ex.printStackTrace();
        } catch (URISyntaxException ex) {
            //If we got a bogus URL from the internet, then this will throw.  Log & Ignore
            ex.printStackTrace();
        } catch (RuntimeException ex) {
            //browse() throws a bunch of runtime exceptions if you give it bad input
            //WHICH IS AWESOME
            ex.printStackTrace();
        }
    }
}
