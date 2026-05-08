package org.example.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import org.example.model.User;
import org.example.service.ResponseQuestionService;
import org.example.service.UserService;
import org.example.util.NavbarOrdonnanceMenu;
import org.example.util.SceneNavigation;

/** Barre client partagée pour les écrans boutique (panier, produits, etc.). */
public class FrontShopNavBarController {

    public enum ActiveShopPage {
        ACCUEIL,
        PRODUITS,
        SERVICES,
        PANIER,
        ADRESSES,
        COMMANDES
    }

    @FXML private VBox navRoot;
    @FXML private HBox profileContainer;
    @FXML private Label navbarUsername;
    @FXML private Circle navbarAvatarCircle;
    @FXML private Label navbarAvatarLabel;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;
    @FXML private Label messagesBadge;

    @FXML private Button btnAccueil;
    @FXML private Button btnProduits;
    @FXML private Button btnServices;
    @FXML private Button btnPanier;
    @FXML private Button btnAdresses;
    @FXML private Button btnCommandes;

    private final UserService userService = new UserService();
    private final ResponseQuestionService responseService = new ResponseQuestionService();

    private Runnable onSearchTap;

    @FXML
    public void initialize() {
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }
        User navUser = userService.getCurrentUser();
        if (navbarUsername != null && navUser != null) {
            String nom = navUser.getNom() != null ? navUser.getNom() : navUser.getEmail();
            if (nom != null && nom.contains(" ")) {
                navbarUsername.setText(nom.substring(0, nom.indexOf(' ')));
            } else {
                navbarUsername.setText(nom != null ? nom : "Utilisateur");
            }
        }
        if (navbarAvatarCircle != null) {
            navbarAvatarCircle.setStyle("-fx-fill: #1f6f54; -fx-stroke: white; -fx-stroke-width: 2;");
        }
        updateMessagesBadge();
        Platform.runLater(() -> NavbarOrdonnanceMenu.wirePopupStyle(navRoot));
    }

    /** Définit la page mise en avant et l'action du bouton loupe (peut être null). */
    public void configure(ActiveShopPage active, Runnable onSearchTap) {
        this.onSearchTap = onSearchTap;
        applyActive(active);
        updateMessagesBadge();
    }

    public void configure(ActiveShopPage active, javafx.scene.control.TextField searchField) {
        configure(active, searchField != null ? searchField::requestFocus : null);
    }

    private void applyActive(ActiveShopPage active) {
        clearActive(btnAccueil, btnProduits, btnServices, btnPanier, btnAdresses, btnCommandes);
        Button target = switch (active) {
            case ACCUEIL -> btnAccueil;
            case PRODUITS -> btnProduits;
            case SERVICES -> btnServices;
            case PANIER -> btnPanier;
            case ADRESSES -> btnAdresses;
            case COMMANDES -> btnCommandes;
        };
        if (target != null) {
            target.getStyleClass().add("menu-item-active");
        }
    }

    private void clearActive(Button... buttons) {
        for (Button b : buttons) {
            if (b != null) {
                b.getStyleClass().remove("menu-item-active");
            }
        }
    }

    private void updateMessagesBadge() {
        if (messagesBadge == null) {
            return;
        }
        User current = userService.getCurrentUser();
        if (current == null) {
            messagesBadge.setVisible(false);
            messagesBadge.setManaged(false);
            return;
        }
        int count = responseService.countUnreadResponsesForClient(current.getId());
        messagesBadge.setText(String.valueOf(count));
        messagesBadge.setVisible(count > 0);
        messagesBadge.setManaged(count > 0);
    }

    private void closeProfileDropdown() {
        if (profileDropdown != null) {
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }
    }

    @FXML
    private void toggleProfileDropdown() {
        boolean next = profileDropdown == null || !profileDropdown.isVisible();
        if (profileDropdown != null) {
            profileDropdown.setVisible(next);
            profileDropdown.setManaged(next);
            if (next && profileDropdown.getParent() != null) {
                profileDropdown.toFront();
            }
        }
    }

    @FXML
    private void handleNavbarSearch() {
        closeProfileDropdown();
        if (onSearchTap != null) {
            onSearchTap.run();
        }
    }

    @FXML
    private void goToMessagesPage() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navRoot, "/fxml/MessagesPage.fxml");
    }

    @FXML
    private void goToProfil() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navRoot, "/fxml/Profil.fxml");
    }

    @FXML
    private void goToDashboard() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navRoot, "/fxml/Dashboard.fxml");
    }

    @FXML
    private void logout() {
        closeProfileDropdown();
        userService.logout();
        SceneNavigation.replaceScene(navRoot, "/fxml/Login.fxml");
    }

    @FXML private void handleNavAccueil() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navRoot, "/fxml/Accueil.fxml");
    }

    @FXML private void handleNavProduits() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navRoot, "/fxml/FrontProduits.fxml");
    }

    @FXML private void handleNavServices() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navRoot, "/fxml/FrontServices.fxml");
    }

    @FXML private void handleNavPanier() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navRoot, "/fxml/FrontCommande.fxml");
    }

    @FXML private void handleNavAdresses() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navRoot, "/fxml/FrontMesAdresses.fxml");
    }

    @FXML private void handleNavCommandes() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navRoot, "/fxml/FrontMesCommandes.fxml");
    }

    @FXML private void handleNavCreerOrdonnance() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navRoot, "/fxml/Ordonnance.fxml");
    }

    @FXML private void handleNavMesOrdonnances() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navRoot, "/fxml/MesOrdonnances.fxml");
    }

    @FXML private void handleNavTraitement() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navRoot, "/fxml/Traitement.fxml");
    }

    @FXML private void handleNavGuide() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navRoot, "/fxml/GuideSante.fxml");
    }

    @FXML private void handleNavContact() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navRoot, "/fxml/ContactPage.fxml");
    }

    @FXML private void handleNavAbout() {
        closeProfileDropdown();
        SceneNavigation.replaceScene(navRoot, "/fxml/APropos.fxml");
    }

    /** Pour tests ou autres contrôleurs parents. */
    public VBox getNavRoot() {
        return navRoot;
    }
}
