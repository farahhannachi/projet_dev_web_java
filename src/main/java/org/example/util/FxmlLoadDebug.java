package org.example.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

/**
 * Small helper to debug FXML load errors from the command line.
 * Not used by the application at runtime.
 */
public final class FxmlLoadDebug {
    public static void main(String[] args) {
        Platform.startup(() -> {
            try {
                FXMLLoader loader = new FXMLLoader(FxmlLoadDebug.class.getResource("/fxml/ContactPage.fxml"));
                Parent root = loader.load();
                System.out.println("FXML loaded successfully: " + root);
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                Platform.exit();
            }
        });
    }
}

