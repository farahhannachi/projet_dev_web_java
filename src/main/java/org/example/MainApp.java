package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.image.Image;
import javafx.stage.Stage;

public class MainApp extends Application {

    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        // Set app icon
        try {
            primaryStage.getIcons().add(new Image(getClass().getResourceAsStream("/logo.png")));
        } catch (Exception e) {
            System.err.println("Logo not found, skipping icon.");
        }
        navigateTo("/Backoffice/Dashboard.fxml", "CuraVita - Pharmacie en Ligne");
    }

    public static void navigateTo(String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(MainApp.class.getResource(fxmlPath));
            Scene scene = new Scene(root, 1200, 750);
            scene.getStylesheets().add(MainApp.class.getResource("/Backoffice/style.css").toExternalForm());
            primaryStage.setTitle(title);
            primaryStage.setScene(scene);
            primaryStage.show();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void main(String[] args) {
        launch(args);
    }
}
