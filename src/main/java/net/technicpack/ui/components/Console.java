package net.technicpack.ui.components;

import net.technicpack.launchercore.logging.RotatingFileHandler;
import net.technicpack.utilslib.Utils;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

public class Console {
    private ConsoleFrame frame = null;
    private RotatingFileHandler handler = null;
    private final ConsoleFrame consoleFrame;
    private final String build;

    public Console(ConsoleFrame consoleFrame, String build) {
        this.consoleFrame = consoleFrame;
        this.build = build;
        Utils.getLogger().info("Console Mode Activated");
    }

    public ConsoleFrame getFrame() {
        return frame;
    }

    public void setRotatingFileHandler(RotatingFileHandler handler) {
        this.handler = handler;
    }

    public RotatingFileHandler getHandler() {
        return handler;
    }

    public void destroyConsole() {
        if (frame != null) {
            frame.setVisible(false);
            frame.dispose();
        }
    }

    /**
     * Log a message.
     *
     * @param line line
     */
    public void log(String line) {
        log(line, null);
    }

    /**
     * Log a message given the {@link AttributeSet}.
     *
     * @param line       line
     * @param attributes attribute set, or null for none
     */
    public void log(String line, AttributeSet attributes) {
        line = "[B#" + build + "] " + line;

        if (line.startsWith("(!!)")) {
            attributes = consoleFrame.getHighlightedAttributes();
        }

        final String writeText = line.replace("\n\n", "\n");
        final AttributeSet writeAttributes = (attributes != null) ? attributes : consoleFrame.getDefaultAttributes();
        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                try {
                    int offset = consoleFrame.getDocument().getLength();
                    consoleFrame.getDocument().insertString(offset, writeText, writeAttributes);
                    consoleFrame.setCaretPosition(consoleFrame.getDocument().getLength());
                } catch (BadLocationException ble) {
                } catch (NullPointerException npe) {
                }
            }
        });
    }
}