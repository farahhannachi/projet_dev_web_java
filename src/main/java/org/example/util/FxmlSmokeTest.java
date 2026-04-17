package org.example.util;

import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;

/**
 * Small helper main to validate that FXML files can be loaded.
 * Not used by the application at runtime.
 */
public final class FxmlSmokeTest {
    public static void main(String[] args) throws Exception {
        Platform.startup(() -> {
        });

        try {
            load("/fxml/ContactPage.fxml");
            System.out.println("OK: ContactPage.fxml loaded");
        } finally {
            Platform.exit();
        }
    }

    private static void load(String resource) throws Exception {
        FXMLLoader loader = new FXMLLoader(FxmlSmokeTest.class.getResource(resource));
        Parent root = loader.load();
        if (root == null) {
            throw new IllegalStateException("Loaded root is null for " + resource);
        }
    }
}

