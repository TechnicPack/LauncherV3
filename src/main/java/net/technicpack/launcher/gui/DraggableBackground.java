package net.technicpack.launcher.gui;

import javafx.event.EventHandler;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.BorderPane;
import javafx.stage.Window;

public class DraggableBackground extends BorderPane {
	private double initialX = 0;
	private double initialY = 0;

	public DraggableBackground() {
		super();
	}

	public void setupDragging() {
		final Window window = getScene().getWindow();

		this.setOnMousePressed(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (mouseEvent.getButton() != MouseButton.MIDDLE) {
					initialX = mouseEvent.getSceneX();
					initialY = mouseEvent.getSceneY();
				} else {
					window.centerOnScreen();
					initialX = window.getX();
					initialY = window.getY();
				}
			}
		});

		this.setOnMouseDragged(new EventHandler<MouseEvent>() {
			@Override
			public void handle(MouseEvent mouseEvent) {
				if (mouseEvent.getButton() != MouseButton.MIDDLE) {
					window.setX(mouseEvent.getScreenX() - initialX);
					window.setY(mouseEvent.getScreenY() - initialY);
				}
			}
		});
	}
}
