package org.example.controller;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.input.MouseEvent;
import javafx.scene.shape.Circle;
import org.example.model.ResponseQuestion;
import org.example.service.UserService;
import org.example.util.NavbarOrdonnanceMenu;
import org.example.util.SceneNavigation;

import java.time.format.DateTimeFormatter;
import java.util.List;

public class AccueilController {

    @FXML private StackPane rootContainer;
    @FXML private HBox profileContainer;
    @FXML private Label navbarUsername;
    @FXML private Circle navbarAvatarCircle;
    @FXML private Label navbarAvatarLabel;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;
    @FXML private Button messagesButton;
    @FXML private Button closeNotificationsButton;
    @FXML private Label messagesBadge;
    @FXML private StackPane notificationsOverlay;
    @FXML private VBox notificationsPopup;
    @FXML private VBox notificationsList;
    @FXML private Label notificationsEmptyLabel;
    @FXML private Label popupUnreadBadge;
    @FXML private StackPane detailOverlayContainer;
    @FXML private VBox detailOverlayModal;

    private final UserService userService = new UserService();
    private final org.example.service.ResponseQuestionService responseService = new org.example.service.ResponseQuestionService();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int MAX_NOTIFICATIONS = 8;
    private int unreadCount = 0;
    private boolean outsideClickHandlerBound = false;

    @FXML
    public void initialize() {
        // Show/hide Dashboard option based on user type
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }
        if (messagesButton != null) {
            messagesButton.setVisible(true);
            messagesButton.setManaged(true);
        }
        updateMessagesBadge();
        org.example.model.User navUser = userService.getCurrentUser();
        if (navbarUsername != null && navUser != null) {
            String nom = navUser.getNom() != null ? navUser.getNom() : navUser.getEmail();
            navbarUsername.setText(nom.split(" ")[0]);
        }
        if (navbarAvatarCircle != null) {
            navbarAvatarCircle.setStyle("-fx-fill: #1f6f54; -fx-stroke: white; -fx-stroke-width: 2;");
        }
        Platform.runLater(this::bindOutsideClickHandler);
        NavbarOrdonnanceMenu.wirePopupStyle(rootContainer);
    }

    private void updateMessagesBadge() {
        if (messagesBadge == null) {
            return;
        }
        org.example.model.User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            messagesBadge.setVisible(false);
            messagesBadge.setManaged(false);
            if (popupUnreadBadge != null) {
                popupUnreadBadge.setText("0");
            }
            return;
        }
        unreadCount = responseService.countUnreadResponsesForClient(currentUser.getId());
        messagesBadge.setText(String.valueOf(unreadCount));
        messagesBadge.setVisible(unreadCount > 0);
        messagesBadge.setManaged(unreadCount > 0);
        if (popupUnreadBadge != null) {
            popupUnreadBadge.setText(String.valueOf(unreadCount));
        }
    }

    private void loadNotificationPreviews() {
        if (notificationsList == null || notificationsEmptyLabel == null) {
            return;
        }

        notificationsList.getChildren().clear();
        org.example.model.User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            notificationsEmptyLabel.setVisible(true);
            notificationsEmptyLabel.setManaged(true);
            return;
        }

        List<ResponseQuestion> items = responseService.getLatestResponsesForClient(currentUser.getId(), MAX_NOTIFICATIONS);
        if (items.isEmpty()) {
            notificationsEmptyLabel.setVisible(true);
            notificationsEmptyLabel.setManaged(true);
            return;
        }

        notificationsEmptyLabel.setVisible(false);
        notificationsEmptyLabel.setManaged(false);

        for (ResponseQuestion item : items) {
            notificationsList.getChildren().add(buildNotificationItem(item));
        }
    }

    private HBox buildNotificationItem(ResponseQuestion response) {
        HBox row = new HBox(10);
        row.setAlignment(Pos.TOP_LEFT);
        row.getStyleClass().add("notification-item");
        if (!response.isLuParClient()) {
            row.getStyleClass().add("notification-item-unread");
        }

        VBox textBox = new VBox(4);

        Label title = new Label(safe(response.getQuestionObjet()));
        title.getStyleClass().add("notification-item-title");
        title.setWrapText(true);

        Label preview = new Label(shortText(safe(response.getReponseText())));
        preview.getStyleClass().add("notification-item-preview");
        preview.setWrapText(true);

        String role = response.getReponseRole() != null ? response.getReponseRole().getLabel() : "Reponse";
        String date = response.getCreatedAt() != null ? response.getCreatedAt().format(DATE_FORMAT) : "";
        Label meta = new Label(role + (date.isEmpty() ? "" : " • " + date));
        meta.getStyleClass().add("notification-item-meta");

        textBox.getChildren().addAll(title, preview, meta);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label dot = new Label("●");
        dot.getStyleClass().add("notification-unread-dot");
        dot.setVisible(!response.isLuParClient());
        dot.setManaged(!response.isLuParClient());

        row.getChildren().addAll(textBox, spacer, dot);
        row.setOnMouseClicked(event -> openNotificationDetails(response));
        return row;
    }

    private void openNotificationDetails(ResponseQuestion response) {
        org.example.model.User currentUser = userService.getCurrentUser();
        if (currentUser == null || response == null || detailOverlayContainer == null || detailOverlayModal == null) {
            return;
        }

        detailOverlayModal.getChildren().clear();

        // Construction du header de la modale
        HBox header = new HBox(10);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label title = new Label("Reponse du support");
        title.getStyleClass().add("notification-detail-hero-title");
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("notification-close-btn");
        closeBtn.setOnAction(e -> closeNotificationOverlay(response));

        header.getChildren().addAll(title, spacer, closeBtn);

        // Detail hero
        HBox hero = new HBox(10);
        hero.setAlignment(Pos.CENTER_LEFT);
        hero.getStyleClass().add("notification-detail-hero");

        StackPane icon = new StackPane(new Label("✉"));
        icon.getStyleClass().add("notification-detail-hero-icon");

        VBox heroText = new VBox(2);
        Label heroTitle = new Label("Equipe Support");
        heroTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #1F2937;");
        Label heroSub = new Label(response.getCreatedAt() != null ? response.getCreatedAt().format(DATE_FORMAT) : "-");
        heroSub.getStyleClass().add("notification-detail-hero-subtitle");
        heroText.getChildren().addAll(heroTitle, heroSub);

        Region heroSpacer = new Region();
        HBox.setHgrow(heroSpacer, Priority.ALWAYS);
        Label stateChip = new Label(response.isLuParClient() ? "Lu" : "Nouveau");
        stateChip.getStyleClass().add(response.isLuParClient() ? "notification-chip-seen" : "notification-chip-new");

        hero.getChildren().addAll(icon, heroText, heroSpacer, stateChip);

        // Contenu
        VBox content = new VBox(14);
        content.getStyleClass().add("notification-detail-content");

        Label questionTitle = new Label("Question");
        questionTitle.getStyleClass().add("notification-detail-label");
        Label questionValue = new Label(safe(response.getQuestionObjet()));
        questionValue.getStyleClass().addAll("notification-detail-question", "notification-detail-card");
        questionValue.setWrapText(true);

        Label responseTitle = new Label("Reponse complete");
        responseTitle.getStyleClass().add("notification-detail-label");

        Label responseText = new Label(safe(response.getReponseText()));
        responseText.setWrapText(true);
        responseText.setMinHeight(140);
        responseText.getStyleClass().addAll("notification-detail-text", "notification-detail-card");

        String metaText = "Role: " + (response.getReponseRole() != null ? response.getReponseRole().getLabel() : "-");
        Label meta = new Label(metaText);
        meta.getStyleClass().add("notification-detail-meta");

        content.getChildren().addAll(hero, questionTitle, questionValue, responseTitle, responseText, meta);

        if (response.getFileName() != null && !response.getFileName().isBlank()) {
            Label fileLabel = new Label("Piece jointe: " + response.getFileName());
            fileLabel.getStyleClass().add("notification-detail-file");
            content.getChildren().add(fileLabel);
        }

        ScrollPane scrollPane = new ScrollPane(content);
        scrollPane.setFitToWidth(true);
        scrollPane.setPrefViewportHeight(380);
        scrollPane.getStyleClass().add("notification-detail-scroll");

        detailOverlayModal.getChildren().addAll(header, scrollPane);

        detailOverlayContainer.setVisible(true);
        detailOverlayContainer.setManaged(true);
        detailOverlayContainer.toFront();
        playDetailAnimation(detailOverlayModal);
    }

    private void closeNotificationOverlay(ResponseQuestion response) {
        detailOverlayContainer.setVisible(false);
        detailOverlayContainer.setManaged(false);
        
        if (response != null && !response.isLuParClient()) {
            org.example.model.User currentUser = userService.getCurrentUser();
            if (currentUser != null) {
                boolean changed = responseService.markAsSeenForClient(response.getId(), currentUser.getId());
                if (changed) {
                    response.setLuParClient(true);
                    unreadCount = Math.max(0, unreadCount - 1);
                    updateMessagesBadge();
                    loadNotificationPreviews();
                }
            }
        }
    }

    private void playDetailAnimation(Parent pane) {
        FadeTransition fade = new FadeTransition(javafx.util.Duration.millis(220), pane);
        fade.setFromValue(0.0);
        fade.setToValue(1.0);
        fade.play();

        ScaleTransition scale = new ScaleTransition(javafx.util.Duration.millis(220), pane);
        scale.setFromX(0.96);
        scale.setFromY(0.96);
        scale.setToX(1.0);
        scale.setToY(1.0);
        scale.play();
    }

    private void bindOutsideClickHandler() {
        if (outsideClickHandlerBound || rootContainer == null || rootContainer.getScene() == null) {
            return;
        }
        rootContainer.getScene().addEventFilter(MouseEvent.MOUSE_PRESSED, this::handleSceneClick);
        outsideClickHandlerBound = true;
    }

    private void handleSceneClick(MouseEvent event) {
        if (notificationsOverlay == null || !notificationsOverlay.isVisible()) {
            return;
        }
        if (isInside(messagesButton, event) || isInside(notificationsPopup, event)) {
            return;
        }
        notificationsOverlay.setVisible(false);
        notificationsOverlay.setManaged(false);
    }

    private boolean isInside(javafx.scene.Node node, MouseEvent event) {
        if (node == null || node.getScene() == null) {
            return false;
        }
        javafx.geometry.Point2D point = node.sceneToLocal(event.getSceneX(), event.getSceneY());
        return node.contains(point);
    }

    @FXML
    private void handleSearch() {
        System.out.println("Search clicked");
    }

    @FXML
    private void toggleProfileDropdown() {
        if (notificationsOverlay != null) {
            notificationsOverlay.setVisible(false);
            notificationsOverlay.setManaged(false);
        }
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

    @FXML
    private void goToProfil() {
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
        switchScene("/fxml/Profil.fxml");
    }

    @FXML
    private void handleNavAccueil() {
        // Déjà sur l'accueil
    }

    @FXML
    private void handleNavProduits() {
        /* Déjà sur la vitrine produits / accueil */
    }

    @FXML
    private void handleNavCommandes() {
        /* Hub accueil : pas de page commandes client dédiée pour l’instant */
    }

    @FXML
    private void handleNavOrdonnance() {
        switchScene("/fxml/Ordonnance.fxml");
    }

    @FXML
    private void handleNavMesOrdonnances() {
        switchScene("/fxml/MesOrdonnances.fxml");
    }

    @FXML
    private void handleNavTraitement() {
        switchScene("/fxml/Traitement.fxml");
    }

    @FXML
    private void handleNavGuide() {
        switchScene("/fxml/GuideSante.fxml");
    }

    @FXML
    private void handleNavAbout() {
        switchScene("/fxml/APropos.fxml");
    }

    @FXML
    private void goToMessagesPage() {
        if (notificationsOverlay != null) {
            notificationsOverlay.setVisible(false);
            notificationsOverlay.setManaged(false);
        }
        if (profileDropdown != null) {
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }
        switchScene("/fxml/MessagesPage.fxml");
    }

    private void switchScene(String fxmlPath) {
        if (getClass().getResource(fxmlPath) == null) {
            showError("Navigation", "Page introuvable : " + fxmlPath);
            return;
        }
        SceneNavigation.replaceScene(rootContainer, fxmlPath);
    }

    @FXML
    private void goToDashboard() {
        if (profileDropdown != null) {
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }
        SceneNavigation.replaceScene(rootContainer, "/fxml/Dashboard.fxml");
    }

    @FXML
    private void logout() {
        if (profileDropdown != null) {
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }
        userService.logout();
        SceneNavigation.replaceScene(rootContainer, "/fxml/Login.fxml");
    }

    @FXML
    private void handleContact() {
        if (getClass().getResource("/fxml/ContactPage.fxml") == null) {
            showError("Navigation", "Page Contact introuvable.");
            return;
        }
        SceneNavigation.replaceScene(rootContainer, "/fxml/ContactPage.fxml");
    }

    @FXML
    private void handleMessages() {
        if (notificationsOverlay == null) {
            return;
        }

        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);

        boolean visible = notificationsOverlay.isVisible();
        if (visible) {
            notificationsOverlay.setVisible(false);
            notificationsOverlay.setManaged(false);
            return;
        }

        notificationsOverlay.setVisible(true);
        notificationsOverlay.setManaged(true);
        notificationsOverlay.toFront();

        updateMessagesBadge();
        loadNotificationPreviews();
    }

    @FXML
    private void closeNotificationsPopup() {
        if (notificationsOverlay != null) {
            notificationsOverlay.setVisible(false);
            notificationsOverlay.setManaged(false);
        }
    }

    private String shortText(String text) {
        int max = 95;
        if (text == null || text.isBlank()) {
            return "Aucun contenu.";
        }
        String clean = text.trim().replace('\n', ' ');
        if (clean.length() <= max) {
            return clean;
        }
        return clean.substring(0, Math.max(0, max - 3)) + "...";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
