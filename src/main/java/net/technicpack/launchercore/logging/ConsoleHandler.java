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

package net.technicpack.launchercore.logging;

import io.sentry.Sentry;
import net.technicpack.ui.components.ConsoleFrame;
import net.technicpack.utilslib.Utils;

import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class ConsoleHandler extends Handler {
    private final ConsoleFrame consoleFrame;

    public ConsoleHandler(ConsoleFrame consoleFrame) {
        this.consoleFrame = consoleFrame;
        Utils.getLogger().info("Console Mode Activated");
    }

    @Override
    public void publish(LogRecord record) {
        String msg;

        try {
            msg = getFormatter().format(record);
        } catch (Exception e) {
            Sentry.captureException(e);
            return;
        }

        final AttributeSet attributes = getAttributes(record, msg);
        final String writeText = msg.replace("\n\n", "\n");
        final Document document = consoleFrame.getDocument();

        SwingUtilities.invokeLater(() -> {
            try {
                int offset = document.getLength();
                document.insertString(offset, writeText, attributes);
                consoleFrame.setCaretPosition(document.getLength());
            } catch (BadLocationException e) {
            }
        });
    }

    private AttributeSet getAttributes(LogRecord record, String msg) {
        AttributeSet attributes = consoleFrame.getDefaultAttributes();

        final Level level = record.getLevel();

        if (msg.startsWith("(!!)")) {
            attributes = consoleFrame.getHighlightedAttributes();
        } else if (level == Level.SEVERE) {
            attributes = consoleFrame.getErrorAttributes();
        } else if (level == Level.WARNING) {
            attributes = consoleFrame.getWarnAttributes();
        } else if (level.intValue() < Level.INFO.intValue()) {
            attributes = consoleFrame.getDebugAttributes();
        }

        return attributes;
    }

    @Override
    public void flush() {
        // Nothing to do
    }

    @Override
    public void close() throws SecurityException {
        // Nothing to do
    }
}
