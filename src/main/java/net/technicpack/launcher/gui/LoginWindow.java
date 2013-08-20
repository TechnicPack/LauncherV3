package net.technicpack.launcher.gui;

import javafx.geometry.Pos;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

public class LoginWindow extends BasicWindow {
	private final TextField userField;
	private final PasswordField passwordField;

	public LoginWindow(Stage parent) {
		super(parent, 300, 200, true);

		userField = new TextField("blsdfksdf");
		userField.setPrefWidth(200);
		userField.setEditable(true);
		passwordField = new PasswordField();
		passwordField.setPrefWidth(200);

		VBox pane = new VBox();
		pane.setAlignment(Pos.CENTER);
		pane.getChildren().add(userField);
		pane.getChildren().add(passwordField);
		getRoot().setCenter(pane);
	}
}
