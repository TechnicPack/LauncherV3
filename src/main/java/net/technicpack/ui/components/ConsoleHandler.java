package net.technicpack.ui.components;

import java.util.logging.Handler;
import java.util.logging.LogRecord;

public class ConsoleHandler extends Handler {

    private Console console;

    public ConsoleHandler(Console console) {
        this.console = console;
    }

    @Override
    public void publish(LogRecord record) {
        this.console.log(record.getMessage() + '\n', record.getLevel());
    }

    @Override
    public void flush() {

    }

    @Override
    public void close() throws SecurityException {

    }
}
