package net.technicpack.launcher.ui.components;

import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

class OptionsDialogClientIdSuffixTest {
    @Test
    void returnsLastFourClientIdCharactersIgnoringSeparators() throws Exception {
        String clientId = "12345678-1234-1234-1234-123456789abc";

        assertEquals("9abc", invokeClientIdSuffix(clientId));
    }

    @Test
    void leavesShortClientIdsVisible() throws Exception {
        assertEquals("9abc", invokeClientIdSuffix("9abc"));
    }

    private static String invokeClientIdSuffix(String clientId) throws Exception {
        try {
            Method method = OptionsDialog.class.getDeclaredMethod("getClientIdSuffix", String.class);
            method.setAccessible(true);
            return (String) method.invoke(null, clientId);
        } catch (NoSuchMethodException e) {
            fail("OptionsDialog.getClientIdSuffix should expose the visible client ID suffix", e);
            return null;
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof Exception exception) {
                throw exception;
            }
            throw e;
        }
    }
}
