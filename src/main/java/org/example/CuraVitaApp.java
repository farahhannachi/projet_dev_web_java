package org.example;

import javafx.application.Application;
import javafx.stage.Stage;
import org.example.util.SceneNavigation;

public class CuraVitaApp extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Démarrer le serveur HTTP local pour les QR codes PDF
        org.example.util.QRPdfServerService.getInstance();

        primaryStage.setTitle("CuraVita - Pharmacy Management System");
        SceneNavigation.replaceStageScene(primaryStage, "/fxml/Login.fxml");
        primaryStage.setFullScreenExitHint("Press ESC to exit fullscreen");
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
