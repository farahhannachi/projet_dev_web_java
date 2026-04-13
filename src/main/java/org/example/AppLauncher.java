package org.example;

import javafx.application.Application;

/**
 * Point d'entree "classique" pour eviter l'erreur
 * "JavaFX runtime components are missing" avec certains modes de lancement.
 */
public final class AppLauncher {

    private AppLauncher() {
        // Utility class
    }

    public static void main(String[] args) {
        Application.launch(CuraVitaApp.class, args);
    }
}

