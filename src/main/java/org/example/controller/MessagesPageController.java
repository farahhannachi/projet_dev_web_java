package org.example.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.scene.control.Alert;
import org.example.model.ResponseQuestion;
import org.example.model.User;
import org.example.service.ResponseQuestionService;
import org.example.service.UserService;
import org.example.util.NavbarOrdonnanceMenu;
import org.example.util.SceneNavigation;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class MessagesPageController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private HBox profileContainer;
    @FXML private Label navbarUsername;
    @FXML private Circle navbarAvatarCircle;
    @FXML private Label navbarAvatarLabel;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;
    @FXML private Button messagesButton;
    @FXML private Label messagesBadge;

    @FXML private TableView<ResponseQuestion> messagesTable;
    @FXML private TableColumn<ResponseQuestion, String> questionColumn;
    @FXML private TableColumn<ResponseQuestion, String> responseColumn;
    @FXML private TableColumn<ResponseQuestion, String> roleColumn;
    @FXML private TableColumn<ResponseQuestion, String> createdAtColumn;

    private final UserService userService = new UserService();
    private final ResponseQuestionService responseService = new ResponseQuestionService();

    @FXML
    public void initialize() {
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }
        setupTable();
        loadMessages();
        updateMessagesBadge();
        User navUser = userService.getCurrentUser();
        if (navbarUsername != null && navUser != null) {
            String nom = navUser.getNom() != null ? navUser.getNom() : navUser.getEmail();
            navbarUsername.setText(nom.split(" ")[0]);
        }
        if (navbarAvatarCircle != null) {
            navbarAvatarCircle.setStyle("-fx-fill: #1f6f54; -fx-stroke: white; -fx-stroke-width: 2;");
        }
        NavbarOrdonnanceMenu.wirePopupStyle(profileContainer);
    }

    private Node navAnchor() {
        return messagesTable != null ? messagesTable : profileContainer;
    }

    private void setupTable() {
        questionColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getQuestionObjet())));
        responseColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getShortReponseText()));
        roleColumn.setCellValueFactory(data -> new SimpleStringProperty(label(data.getValue())));
        createdAtColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDate(data.getValue())));
    }

    private void loadMessages() {
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            messagesTable.setItems(FXCollections.observableArrayList());
            return;
        }
        List<ResponseQuestion> responses = responseService.getResponsesForClient(currentUser.getId());
        messagesTable.setItems(FXCollections.observableArrayList(responses));
    }

    private void updateMessagesBadge() {
        if (messagesBadge == null) {
            return;
        }
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            messagesBadge.setVisible(false);
            messagesBadge.setManaged(false);
            return;
        }
        int count = responseService.countUnreadResponsesForClient(currentUser.getId());
        messagesBadge.setText(String.valueOf(count));
        messagesBadge.setVisible(count > 0);
        messagesBadge.setManaged(count > 0);
    }

    @FXML
    private void handleSearch() {
        System.out.println("Search clicked");
    }

    @FXML
    private void handleMessages() {
        loadMessages();
        updateMessagesBadge();
    }

    @FXML
    private void handleNavProduits() {
        switchScene("/fxml/Accueil.fxml");
    }

    @FXML
    private void handleNavCommandes() {
        switchScene("/fxml/Accueil.fxml");
    }

    @FXML
    private void handleNavTraitement() {
        switchScene("/fxml/Traitement.fxml");
    }

    @FXML
    private void goToCreerOrdonnance() {
        switchScene("/fxml/Ordonnance.fxml");
    }

    @FXML
    private void goToMesOrdonnances() {
        switchScene("/fxml/MesOrdonnances.fxml");
    }

    @FXML
    private void handleNavGuide() {
        switchScene("/fxml/GuideSante.fxml");
    }

    @FXML
    private void handleNavAbout() {
        switchScene("/fxml/APropos.fxml");
    }

    private void switchScene(String fxmlPath) {
        if (getClass().getResource(fxmlPath) == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Page introuvable : " + fxmlPath);
            alert.showAndWait();
            return;
        }
        SceneNavigation.replaceScene(navAnchor(), fxmlPath);
    }

    @FXML
    private void toggleProfileDropdown() {
        boolean next = !profileDropdown.isVisible();
        profileDropdown.setVisible(next);
        profileDropdown.setManaged(next);
        if (next) {
            profileDropdown.toFront();
            Node parent = profileDropdown.getParent();
            if (parent != null) {
                parent.toFront();
            }
        }
    }

    @FXML
    private void goToProfil() {
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
        switchScene("/fxml/Profil.fxml");
    }

    @FXML
    private void goToDashboard() {
        if (profileDropdown != null) {
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }
        SceneNavigation.replaceScene(navAnchor(), "/fxml/Dashboard.fxml");
    }

    @FXML
    private void logout() {
        if (profileDropdown != null) {
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }
        userService.logout();
        SceneNavigation.replaceScene(navAnchor(), "/fxml/Login.fxml");
    }

    @FXML
    private void goHome() {
        SceneNavigation.replaceScene(navAnchor(), "/fxml/Accueil.fxml");
    }

    @FXML
    private void goContact() {
        if (getClass().getResource("/fxml/ContactPage.fxml") == null) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText("Page Contact introuvable.");
            alert.showAndWait();
            return;
        }
        SceneNavigation.replaceScene(navAnchor(), "/fxml/ContactPage.fxml");
    }

    private String label(ResponseQuestion response) {
        return response.getReponseRole() != null ? response.getReponseRole().getLabel() : "";
    }

    private String formatDate(ResponseQuestion response) {
        return response.getCreatedAt() != null ? response.getCreatedAt().format(DATE_FORMAT) : "";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

