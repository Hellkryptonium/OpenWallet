package io.openwallet;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.stage.Stage;

/**
 * Minimal JavaFX entry point to verify dependencies are wired up.
 */
public class MainApp extends Application {

    @Override
    public void start(Stage primaryStage) {
        Label placeholder = new Label("OpenWallet setup complete. UI coming soon.");
        BorderPane root = new BorderPane(placeholder);
        primaryStage.setScene(new Scene(root, 640, 360));
        primaryStage.setTitle("OpenWallet");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
