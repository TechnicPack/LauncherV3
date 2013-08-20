package net.technicpack.launcher.gui;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

public class BasicWindow extends Stage {
	private final DraggableBackground root;

	public BasicWindow(Stage parent, int width, int height, boolean popup) {
		super();
		this.initOwner(parent);
		this.initStyle(StageStyle.UNDECORATED);

		if (popup) {
			this.initModality(Modality.APPLICATION_MODAL);
			parent.getScene().getRoot().setEffect(new GaussianBlur());
		}

		root = new DraggableBackground();
		root.getStyleClass().add("window");

		WindowButtons windowButtons = new WindowButtons(this);
		root.setTop(windowButtons);

		Scene scene = new Scene(root, width, height);
		scene.getStylesheets().add("technic.css");
		this.setScene(scene);

		this.show();
		root.setupDragging();
	}

	public DraggableBackground getRoot() {
		return root;
	}

	@Override
	public void close() {
		if (getOwner() != null) {
			getOwner().getScene().getRoot().setEffect(null);
		}
		super.close();
	}

	private class WindowButtons extends HBox {

		public WindowButtons(final BasicWindow stage) {
			Button closeButton = new Button("Exit");
			closeButton.setAlignment(Pos.CENTER_RIGHT);

			closeButton.setOnAction(new EventHandler<ActionEvent>() {
				@Override
				public void handle(ActionEvent actionEvent) {
					stage.close();
				}
			});

			StackPane stackPane = new StackPane();
			stackPane.getChildren().add(closeButton);
			stackPane.setAlignment(Pos.CENTER_RIGHT);
			this.getChildren().add(stackPane);
			HBox.setHgrow(stackPane, Priority.ALWAYS);
		}
	}
}
