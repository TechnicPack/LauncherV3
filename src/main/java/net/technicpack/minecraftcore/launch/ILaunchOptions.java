package net.technicpack.minecraftcore.launch;

public interface ILaunchOptions {
    String getClientId();
    WindowType getLaunchWindowType();
    int getCustomWidth();
    int getCustomHeight();
    boolean shouldUseStencilBuffer();
}
