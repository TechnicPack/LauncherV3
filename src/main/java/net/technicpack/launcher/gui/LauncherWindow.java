package net.technicpack.launcher.gui;

public class LauncherWindow extends BasicWindow {

	public LauncherWindow() {
		super(null, 800, 600, false);

		LoginWindow loginWindow = new LoginWindow(this);
	}
}
