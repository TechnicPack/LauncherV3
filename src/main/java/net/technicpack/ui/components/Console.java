package net.technicpack.ui.components;

import net.technicpack.launchercore.logging.RotatingFileHandler;
import net.technicpack.utilslib.Utils;

import javax.swing.*;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import java.util.logging.Level;

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
        log(line, Level.INFO);
    }

    /**
     * Log a message given the {@link AttributeSet}.
     *
     * @param line       line
     */
    public void log(String line, Level level) {
        line = "[B#" + build + "] " + line;

        AttributeSet attributes = consoleFrame.getDefaultAttributes();

        if (line.startsWith("(!!)")) {
            attributes = consoleFrame.getHighlightedAttributes();
        } else if (level == Level.SEVERE) {
            attributes = consoleFrame.getErrorAttributes();
        } else if (level == Level.WARNING) {
            attributes = consoleFrame.getInfoAttributes();
        } else if (level.intValue() < Level.INFO.intValue()) {
            attributes = consoleFrame.getDebugAttributes();
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