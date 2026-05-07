package org.example;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.example.util.SceneNavigation;

import java.net.URL;

public class ResponseQuestionAdminLauncher extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ResponseQuestionAdmin.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root, 1200, 800);
        URL cssUrl = SceneNavigation.class.getResource(SceneNavigation.STYLESHEET_PATH);
        if (cssUrl != null) {
            scene.getStylesheets().add(cssUrl.toExternalForm());
        }
        primaryStage.setTitle("CuraVita - Response Questions Admin");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}

