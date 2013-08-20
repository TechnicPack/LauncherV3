package net.technicpack.launcher;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;
import javafx.stage.StageStyle;
import net.technicpack.launcher.auth.AuthResponse;
import net.technicpack.launcher.auth.AuthenticationService;
import net.technicpack.launcher.auth.Response;
import net.technicpack.launcher.gui.DraggableBackground;
import net.technicpack.launcher.gui.LauncherWindow;
import net.technicpack.launcher.gui.LoginWindow;

public class TechnicLauncher extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		LauncherWindow launcherWindow = new LauncherWindow();
	}
}
