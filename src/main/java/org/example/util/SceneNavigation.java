package org.example.util;

import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.net.URL;
import java.util.Objects;

/**
 * Centralise le chargement FXML, la feuille de styles et le passage plein écran
 * pour éviter les routes incohérentes entre les contrôleurs.
 */
public final class SceneNavigation {

    public static final String STYLESHEET_PATH = "/css/styles.css";

    private SceneNavigation() {}

    public static void replaceScene(Node anchorNode, String fxmlClasspath) {
        replaceScene(anchorNode, fxmlClasspath, true);
    }

    /**
     * Remplace la scène du stage qui affiche {@code anchorNode}.
     *
     * @param anchorNode  tout nœud déjà affiché (pour récupérer Stage / Scene)
     * @param fxmlClasspath chemin classpath vers le FXML (ex. "/fxml/Accueil.fxml")
     * @param fullscreen  si true, applique {@link Stage#setFullScreen(boolean)}
     */
    public static void replaceScene(Node anchorNode, String fxmlClasspath, boolean fullscreen) {
        Objects.requireNonNull(fxmlClasspath, "fxmlClasspath");
        try {
            if (anchorNode == null || anchorNode.getScene() == null) {
                System.err.println("[SceneNavigation] anchor sans Scene — abandon : " + fxmlClasspath);
                return;
            }
            URL fxmlUrl = SceneNavigation.class.getResource(fxmlClasspath);
            if (fxmlUrl == null) {
                System.err.println("[SceneNavigation] FXML introuvable : " + fxmlClasspath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Scene newScene = new Scene(root);
            URL cssUrl = SceneNavigation.class.getResource(STYLESHEET_PATH);
            if (cssUrl != null) {
                newScene.getStylesheets().add(cssUrl.toExternalForm());
            }
            Stage stage = (Stage) anchorNode.getScene().getWindow();
            stage.setScene(newScene);
            stage.setFullScreen(fullscreen);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /** Pour le démarrage de l'application ({@link javafx.application.Application#start}) lorsqu'aucun nœud ancré n'existe encore. */
    public static void replaceStageScene(Stage stage, String fxmlClasspath) {
        replaceStageScene(stage, fxmlClasspath, true);
    }

    public static void replaceStageScene(Stage stage, String fxmlClasspath, boolean fullscreen) {
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(fxmlClasspath, "fxmlClasspath");
        try {
            URL fxmlUrl = SceneNavigation.class.getResource(fxmlClasspath);
            if (fxmlUrl == null) {
                System.err.println("[SceneNavigation] FXML introuvable : " + fxmlClasspath);
                return;
            }
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Scene newScene = new Scene(root);
            URL cssUrl = SceneNavigation.class.getResource(STYLESHEET_PATH);
            if (cssUrl != null) {
                newScene.getStylesheets().add(cssUrl.toExternalForm());
            }
            stage.setScene(newScene);
            stage.setFullScreen(fullscreen);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
