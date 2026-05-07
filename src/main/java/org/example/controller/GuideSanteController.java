package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.MenuButton;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.web.WebView;
import org.example.model.User;
import org.example.service.UserService;
import org.example.util.NavbarOrdonnanceMenu;
import org.example.util.SceneNavigation;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

/**
 * Affiche le Guide Santé (contenu HTML/CSS/JS) dans un {@link WebView}, avec la même navbar que les autres écrans client.
 */
public class GuideSanteController {

    @FXML private WebView webView;
    @FXML private HBox profileContainer;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;
    @FXML private Circle navbarAvatarCircle;
    @FXML private Label navbarUsername;
    @FXML private Label navbarAvatarLabel;

    private final UserService userService = UserService.getInstance();

    @FXML
    public void initialize() {
        User currentUser = userService.getCurrentUser();
        if (navbarUsername != null && currentUser != null) {
            String nom = currentUser.getNom() != null ? currentUser.getNom() : currentUser.getEmail();
            navbarUsername.setText(nom.split(" ")[0]);
        }
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }
        if (navbarAvatarCircle != null) {
            navbarAvatarCircle.setStyle("-fx-fill: #1f6f54; -fx-stroke: white; -fx-stroke-width: 2;");
        }
        NavbarOrdonnanceMenu.wirePopupStyle(profileContainer);

        if (webView != null) {
            webView.setZoom(0.92);
            javafx.scene.web.WebEngine engine = webView.getEngine();
            try {
                String css1 = readUtf8("/html/guide-styles-part1.css");
                String css2 = readUtf8("/html/guide-styles-part2.css");
                String body = readUtf8("/html/guide-body.html");
                String html = "<!DOCTYPE html><html><head><meta charset=\"UTF-8\">"
                        + "<style>html,body{margin:0;padding:0;}</style>"
                        + "<style>"
                        + escapeForCdata(css1)
                        + escapeForCdata(css2)
                        + "</style></head><body>"
                        + body
                        + "</body></html>";
                engine.loadContent(html);
            } catch (IOException e) {
                engine.loadContent("<html><body style='font-family:sans-serif;padding:2rem;'>"
                        + "<p>Impossible de charger le Guide Santé.</p><pre>"
                        + escapeHtml(e.getMessage())
                        + "</pre></body></html>");
            }
        }
    }

    /** Évite de casser le HTML si le CSS contient accidentellement {@code </style>}. */
    private static String escapeForCdata(String css) {
        return css.replace("</", "<\\/"); // suffisant pour '</style>' dans les URLs data:, etc.
    }

    private static String escapeHtml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static String readUtf8(String classpathPath) throws IOException {
        try (InputStream in = GuideSanteController.class.getResourceAsStream(classpathPath)) {
            if (in == null) {
                throw new IOException("Ressource introuvable : " + classpathPath);
            }
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private javafx.scene.Node navAnchor() {
        return webView != null ? webView : profileContainer;
    }

    private void closeProfileDropdown() {
        if (profileDropdown != null) {
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }
    }

    @FXML
    private void toggleProfileDropdown() {
        if (profileDropdown != null) {
            boolean next = !profileDropdown.isVisible();
            profileDropdown.setVisible(next);
            profileDropdown.setManaged(next);
            if (next) {
                profileDropdown.toFront();
                javafx.scene.Node parent = profileDropdown.getParent();
                if (parent != null) {
                    parent.toFront();
                }
            }
        }
    }

    @FXML
    private void handleNavGuide() {
        /* Déjà sur cette page */
    }

    @FXML
    private void handleNavProduits() {
        SceneNavigation.replaceScene(navAnchor(), "/fxml/Accueil.fxml");
    }

    @FXML
    private void handleNavCommandes() {
        SceneNavigation.replaceScene(navAnchor(), "/fxml/Accueil.fxml");
    }

    @FXML
    private void handleNavContact() {
        SceneNavigation.replaceScene(navAnchor(), "/fxml/ContactPage.fxml");
    }

    @FXML
    private void handleNavAbout() {
        SceneNavigation.replaceScene(navAnchor(), "/fxml/APropos.fxml");
    }

    @FXML
    private void handleNavbarSearch() {
        if (webView != null) {
            webView.requestFocus();
        }
    }

    @FXML
    private void goToAccueil() {
        SceneNavigation.replaceScene(navAnchor(), "/fxml/Accueil.fxml");
    }

    @FXML
    private void goToTraitement() {
        SceneNavigation.replaceScene(navAnchor(), "/fxml/Traitement.fxml");
    }

    @FXML
    private void goToMesOrdonnances() {
        SceneNavigation.replaceScene(navAnchor(), "/fxml/MesOrdonnances.fxml");
    }

    @FXML
    private void goToCreerOrdonnance() {
        SceneNavigation.replaceScene(navAnchor(), "/fxml/Ordonnance.fxml");
    }

    @FXML
    private void goToProfil() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navAnchor(), "/fxml/Profil.fxml");
    }

    @FXML
    private void goToMessagesPage() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navAnchor(), "/fxml/MessagesPage.fxml");
    }

    @FXML
    private void goToDashboard() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navAnchor(), "/fxml/Dashboard.fxml");
    }

    @FXML
    private void logout() {
        closeProfileDropdown();
        userService.logout();
        SceneNavigation.replaceScene(navAnchor(), "/fxml/Login.fxml");
    }
}
