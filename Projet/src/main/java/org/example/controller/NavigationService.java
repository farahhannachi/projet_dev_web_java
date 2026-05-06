package org.example.controller;

import javafx.scene.layout.Pane;

/**
 * Service de navigation pour gérer les changements de vue dans le content pane
 * Architecture: SPA (Single Page Application) - Pas de nouvelles fenêtres/Stage
 * Approche: Utiliser un StackPane pour "swap" les vues dynamiquement
 */
public class NavigationService {
    private static NavigationService instance;
    private Pane contentPane; // Le centre du BorderPane du Dashboard

    private NavigationService() {}

    public static NavigationService getInstance() {
        if (instance == null) {
            instance = new NavigationService();
        }
        return instance;
    }

    public void setContentPane(Pane pane) {
        this.contentPane = pane;
    }

    public void showView(Pane view) {
        if (contentPane != null) {
            contentPane.getChildren().clear();
            contentPane.getChildren().add(view);
        }
    }

    public Pane getContentPane() {
        return contentPane;
    }
}

