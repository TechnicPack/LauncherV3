package net.technicpack.minecraftcore.microsoft.auth.model;

import com.google.api.client.util.Key;
import net.technicpack.launchercore.exception.MicrosoftAuthException.ExceptionType;

public class XSTSUnauthorized {
    private static final long NO_XBOX_ACCOUNT = 2148916233L;
    private static final long UNDERAGE = 2148916238L;

    @Key(value="Identity") public String identity;
    @Key(value="XErr") public long errorCode;
    @Key(value="Message") public String message;
    @Key(value="Redirect") public String redirect;

    public ExceptionType getExceptionType() {
        if (errorCode == UNDERAGE) {
            return ExceptionType.UNDERAGE;
        } else if (errorCode == NO_XBOX_ACCOUNT) {
            return ExceptionType.NO_XBOX_ACCOUNT;
        }
        return ExceptionType.XSTS;
    }
}
