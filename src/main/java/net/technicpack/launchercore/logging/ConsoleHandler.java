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

import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.SwingUtilities;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.Element;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;

public class ConsoleHandler extends Handler {
    private final ConsoleFrame consoleFrame;
    private final BlockingQueue<LogRecord> logQueue = new LinkedBlockingQueue<>();
    private final Thread workerThread;
    private volatile boolean running = true;

    private static final int MAX_DOC_CHARS = 300_000;
    private static final int MAX_LINES = 2500;

    public ConsoleHandler(ConsoleFrame consoleFrame) {
        this.consoleFrame = consoleFrame;
        Utils.getLogger().info("Console Mode Activated");

        workerThread = new Thread(this::processLogs, "ConsoleHandler-Worker");
        workerThread.setDaemon(true); // So it won't prevent app shutdown
        workerThread.start();
    }

    @Override
    public void publish(LogRecord record) {
        if (!isLoggable(record)) return;
        logQueue.offer(record); // Won't block
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

    private void processLogs() {
        try {
            while (running) {
                LogRecord first = logQueue.take(); // Blocks until one is available

                List<LogRecord> batch = new ArrayList<>();
                batch.add(first);
                logQueue.drainTo(batch); // Grab rest of the available ones

                SwingUtilities.invokeLater(() -> writeBatchToUI(batch));
            }
        } catch (InterruptedException ignored) {
            Thread.currentThread().interrupt(); // Restore interrupt flag
        }
    }

    private void trimDocument(Document doc) {
        try {
            Element root = doc.getDefaultRootElement();
            int lineCount = root.getElementCount();

            // First trim by line count
            int excessLines = lineCount - MAX_LINES;
            int offsetByLines = -1;

            if (excessLines > 0) {
                Element trimLine = root.getElement(excessLines - 1);
                if (trimLine != null) {
                    offsetByLines = trimLine.getEndOffset();
                }
            }

            // Then check char count — but snap to line boundary
            int offsetByChars = -1;
            if (doc.getLength() > MAX_DOC_CHARS) {
                for (int i = 0; i < root.getElementCount(); i++) {
                    Element line = root.getElement(i);
                    if (line.getEndOffset() >= doc.getLength() - MAX_DOC_CHARS) {
                        offsetByChars = line.getEndOffset(); // full line
                        break;
                    }
                }
            }

            int finalTrimOffset = Math.max(offsetByLines, offsetByChars);

            if (finalTrimOffset > 0 && finalTrimOffset < doc.getLength()) {
                doc.remove(0, finalTrimOffset);
            }

        } catch (BadLocationException e) {
            Sentry.captureException(e);
        }
    }

    private boolean isScrollAtBottom(JScrollPane scrollPane) {
        JScrollBar vertical = scrollPane.getVerticalScrollBar();
        int bottom = vertical.getMaximum() - vertical.getVisibleAmount();
        return vertical.getValue() >= (bottom - 30); // Allow slight margin
    }


    private void writeBatchToUI(List<LogRecord> batch) {
        final Document document = consoleFrame.getDocument();

        for (LogRecord record : batch) {
            String msg;
            try {
                msg = getFormatter().format(record);
            } catch (Exception e) {
                Sentry.captureException(e);
                continue;
            }

            final AttributeSet attributes = getAttributes(record, msg);
            final String writeText = msg.replace("\n\n", "\n");

            try {
                int offset = document.getLength();
                document.insertString(offset, writeText, attributes);
            } catch (BadLocationException e) {
                Sentry.captureException(e);
            }
        }

        trimDocument(document);

        final JScrollPane scrollPane = consoleFrame.getScrollPane();
        final boolean shouldScroll = isScrollAtBottom(scrollPane);

        // Only scroll to bottom if we were already at the bottom
        if (shouldScroll) {
            // Defer scrolling to allow the scroll pane and text pane to catch up.
            // This is necessary because otherwise the scrolling will start "lagging" behind.
            SwingUtilities.invokeLater(() -> {
                try {
                    JTextPane textPane = consoleFrame.getTextPane();
                    Rectangle rect = textPane.modelToView(document.getLength());
                    if (rect != null) {
                        textPane.scrollRectToVisible(rect);
                    }
                } catch (BadLocationException e) {
                    Sentry.captureException(e);
                }
            });
        }
    }

    @Override
    public void flush() {
        // Nothing to do
    }

    @Override
    public void close() {
        running = false;
        workerThread.interrupt(); // Wake up blocking take()
    }
}
