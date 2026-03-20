package net.technicpack.launcher.launch;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class InstallerTest {
    @Test
    void detectsCreateProcessAccessDeniedMessage() {
        IOException exception = new IOException("CreateProcess error=5, Access is denied");
        assertTrue(Installer.isCreateProcessAccessDenied(exception));
    }

    @Test
    void doesNotThrowOnNullMessage() {
        IOException exception = new IOException((String) null);
        assertFalse(Installer.isCreateProcessAccessDenied(exception));
    }

    @Test
    void ignoresOtherIoMessages() {
        IOException exception = new IOException("CreateProcess error=2, The system cannot find the file specified");
        assertFalse(Installer.isCreateProcessAccessDenied(exception));
    }
}
