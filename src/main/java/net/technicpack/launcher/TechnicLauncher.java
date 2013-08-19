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

public class TechnicLauncher extends Application {

	public static void main(String[] args) {
		launch(args);
	}

	@Override
	public void start(Stage stage) throws Exception {
		stage.initStyle(StageStyle.UNDECORATED);
		stage.setTitle("Technic Launcher");

		DraggableBackground borderPane = new DraggableBackground();
		borderPane.setStyle("-fx-background-color: black;");

		WindowButtons exit = new WindowButtons();
		borderPane.setTop(exit);

		stage.setScene(new Scene(borderPane, 400, 300));
		stage.getScene().getStylesheets().add("technic.css");
		stage.show();
		borderPane.setupDragging();

		AuthenticationService authenticationService = new AuthenticationService();
		AuthResponse response = authenticationService.requestLogin("someUsername", "somePassword", "someClientToken");
		System.out.println(response.getError());
		System.out.println(response.getErrorMessage());
		System.out.println(response.getCause());
	}

	class WindowButtons extends HBox {

		public WindowButtons() {
			Button closeBtn = new Button("Exit");
			closeBtn.setAlignment(Pos.CENTER_RIGHT);

			closeBtn.setOnAction(new EventHandler<ActionEvent>() {

				@Override
				public void handle(ActionEvent actionEvent) {
					Platform.exit();
				}
			});

			StackPane stackPane = new StackPane();
			stackPane.getChildren().add(closeBtn);
			stackPane.setAlignment(Pos.CENTER_RIGHT);
			this.getChildren().add(stackPane);
			HBox.setHgrow(stackPane, Priority.ALWAYS);
		}
	}
}
