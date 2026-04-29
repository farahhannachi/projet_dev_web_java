package org.example.controller;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.model.Question;
import org.example.model.ResponseQuestion;
import org.example.model.ResponseQuestionFilter;
import org.example.model.User;
import org.example.service.QuestionService;
import org.example.service.ResponseQuestionService;
import org.example.service.UserService;
import org.example.service.GroqAiService;
import org.example.util.PdfExportUtil;

import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class ContactPageController {
    @FXML private StackPane rootContainer;
    @FXML private Button profileButton;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;
    @FXML private Button messagesButton;
    @FXML private Label messagesBadge;
    @FXML private StackPane notificationsOverlay;
    @FXML private VBox notificationsPopup;
    @FXML private VBox notificationsList;
    @FXML private Label notificationsEmptyLabel;
    @FXML private Label popupUnreadBadge;
    @FXML private Button closeNotificationsButton;
    @FXML private StackPane detailOverlayContainer;
    @FXML private VBox detailOverlayModal;
    @FXML private Label myTicketsCountLabel;
    @FXML private VBox myTicketsList;
    @FXML private VBox myTicketsEmptyState;

    // Chatbot
    @FXML private Button chatToggleButton;
    @FXML private VBox chatPanel;
    @FXML private ScrollPane chatScrollPane;
    @FXML private VBox chatMessagesContainer;
    @FXML private TextField chatInputField;

    private final UserService userService = new UserService();
    private final QuestionService questionService = new QuestionService();
    private final ResponseQuestionService responseQuestionService = new ResponseQuestionService();
    private final GroqAiService groqAiService = new GroqAiService();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    private static final int MAX_NOTIFICATIONS = 8;
    private int unreadCount = 0;
    private boolean outsideClickHandlerBound = false;

    @FXML
    public void initialize() {
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }
        ContactQuestionController formController = getContactFormController();
        if (formController != null) {
            formController.setOnQuestionSaved(this::loadMyTickets);
        }
        loadMyTickets();
        updateMessagesBadge();
        Platform.runLater(this::bindOutsideClickHandler);
        // Chatbot welcome
        Platform.runLater(() -> {
            addBotMessage("Bonjour ! \uD83D\uDC4B Je suis l'assistant CuraVita.\nComment puis-je vous aider ?");
            addSuggestionChips();
        });
    }

    private void updateMessagesBadge() {
        if (messagesBadge == null) {
            return;
        }
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            messagesBadge.setVisible(false);
            messagesBadge.setManaged(false);
            if (popupUnreadBadge != null) {
                popupUnreadBadge.setText("0");
            }
            return;
        }
        unreadCount = responseQuestionService.countUnreadResponsesForClient(currentUser.getId());
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
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            notificationsEmptyLabel.setVisible(true);
            notificationsEmptyLabel.setManaged(true);
            return;
        }

        List<ResponseQuestion> items = responseQuestionService.getLatestResponsesForClient(currentUser.getId(), MAX_NOTIFICATIONS);
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

        Label preview = new Label(shortText(safe(response.getReponseText()), 95));
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
        User currentUser = userService.getCurrentUser();
        if (currentUser == null || response == null || detailOverlayContainer == null || detailOverlayModal == null) {
            return;
        }

        detailOverlayModal.getChildren().clear();

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
            User currentUser = userService.getCurrentUser();
            if (currentUser != null) {
                boolean changed = responseQuestionService.markAsSeenForClient(response.getId(), currentUser.getId());
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

    private boolean isInside(Node node, MouseEvent event) {
        if (node == null || node.getScene() == null) {
            return false;
        }
        return node.contains(node.sceneToLocal(event.getSceneX(), event.getSceneY()));
    }

    private void loadMyTickets() {
        if (myTicketsCountLabel == null || myTicketsList == null || myTicketsEmptyState == null) {
            return;
        }

        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            myTicketsCountLabel.setText("0");
            myTicketsList.getChildren().clear();
            myTicketsEmptyState.setVisible(true);
            myTicketsEmptyState.setManaged(true);
            return;
        }

        int total = questionService.countQuestionsForUser(currentUser.getId());
        myTicketsCountLabel.setText(String.valueOf(total));

        List<Question> latest = questionService.getQuestionsForUser(currentUser.getId(), 0);
        myTicketsList.getChildren().clear();

        if (latest.isEmpty()) {
            myTicketsEmptyState.setVisible(true);
            myTicketsEmptyState.setManaged(true);
            return;
        }

        myTicketsEmptyState.setVisible(false);
        myTicketsEmptyState.setManaged(false);

        for (Question q : latest) {
            myTicketsList.getChildren().add(buildTicketMiniCard(q));
        }
    }

    private HBox buildTicketMiniCard(Question q) {
        HBox card = new HBox(12);
        card.getStyleClass().add("ticket-mini-card");
        card.setAlignment(Pos.TOP_LEFT);

        VBox left = new VBox(4);
        Label title = new Label(safe(q.getObjet()));
        title.getStyleClass().add("ticket-mini-title");

        String meta = formatDate(q) + " • " + safe(q.getTypeTicket());
        Label metaLabel = new Label(meta);
        metaLabel.getStyleClass().add("ticket-mini-meta");

        Label description = new Label(shortText(q.getDescription(), 110));
        description.getStyleClass().add("ticket-mini-description");
        description.setWrapText(true);
        description.setMaxWidth(320);

        HBox actions = new HBox(6,
                actionButton("✎", "Modifier ticket", "btn-action-circle", "btn-action-circle-primary", () -> editQuestion(q)),
                actionButton("⎙", "Exporter ticket en PDF", "btn-action-circle", "btn-action-circle-pdf", () -> exportQuestionPdf(q)),
                actionButton("🗑", "Supprimer ticket", "btn-action-circle", "btn-action-circle-danger", () -> deleteQuestion(q))
        );
        actions.getStyleClass().add("ticket-mini-actions");

        left.getChildren().addAll(title, metaLabel, description, actions);

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        VBox right = new VBox(6);
        right.setAlignment(Pos.CENTER_RIGHT);

        Label statut = new Label(formatLabel(q.getStatut()));
        statut.getStyleClass().addAll("ticket-badge", statusClass(q.getStatut()));

        Label priorite = new Label(formatLabel(q.getPriorite()));
        priorite.getStyleClass().addAll("ticket-badge", priorityClass(q.getPriorite()));

        right.getChildren().addAll(statut, priorite);

        card.getChildren().addAll(left, spacer, right);
        return card;
    }

    private Button actionButton(String icon, String tooltip, String class1, String class2, Runnable handler) {
        Button button = new Button(icon);
        button.getStyleClass().addAll(class1, class2, "ticket-mini-action");
        button.setText(icon);
        button.setAccessibleText(tooltip);
        button.setOnAction(event -> handler.run());
        return button;
    }

    private void editQuestion(Question q) {
        if (q == null) {
            return;
        }
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            showError("Session invalide", "Veuillez vous reconnecter puis reessayer.");
            return;
        }

        Question fresh = questionService.getQuestionForUserById(q.getId(), currentUser.getId());
        if (fresh == null) {
            showError("Ticket introuvable", "Le ticket n'existe plus ou ne vous appartient pas.");
            loadMyTickets();
            return;
        }

        ContactQuestionController formController = getContactFormController();
        if (formController != null) {
            formController.startEditQuestion(fresh);
        }
    }

    private ContactQuestionController getContactFormController() {
        return ContactQuestionController.getActiveInstance();
    }

    private void deleteQuestion(Question q) {
        if (q == null) {
            return;
        }
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            showError("Session invalide", "Veuillez vous reconnecter puis reessayer.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Suppression ticket");
        confirm.setHeaderText("Supprimer ce ticket ?");
        confirm.setContentText("Cette action supprimera aussi les reponses associees.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isEmpty() || result.get() != ButtonType.OK) {
            return;
        }

        boolean deleted = questionService.deleteQuestionForUser(q.getId(), currentUser.getId());
        if (!deleted) {
            showError("Suppression impossible", "Le ticket n'a pas pu etre supprime.");
        }
        loadMyTickets();
    }

    private void exportQuestionPdf(Question q) {
        if (q == null) {
            return;
        }
        User currentUser = userService.getCurrentUser();
        if (currentUser == null) {
            showError("Session invalide", "Veuillez vous reconnecter puis reessayer.");
            return;
        }

        Question fresh = questionService.getQuestionForUserById(q.getId(), currentUser.getId());
        if (fresh == null) {
            showError("Export impossible", "Le ticket n'existe plus ou ne vous appartient pas.");
            loadMyTickets();
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Exporter le ticket en PDF");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
        chooser.setInitialFileName("ticket_" + fresh.getId() + ".pdf");

        Stage stage = (Stage) myTicketsList.getScene().getWindow();
        File file = chooser.showSaveDialog(stage);
        if (file == null) {
            return;
        }

        try {
            List<ResponseQuestion> data = buildExportData(fresh);
            PdfExportUtil.exportResponseQuestions(data, file.toPath());
        } catch (IOException e) {
            showError("Export echoue", "Erreur pendant la generation du PDF.");
        }
    }

    private List<ResponseQuestion> buildExportData(Question question) {
        ResponseQuestionFilter filter = new ResponseQuestionFilter();
        filter.setQuestionId(question.getId());

        List<ResponseQuestion> items = responseQuestionService.findAllFiltered(filter, "createdAt", false);
        if (!items.isEmpty()) {
            return items;
        }

        // Keep the same professional PDF layout even if no admin response exists yet.
        ResponseQuestion placeholder = new ResponseQuestion();
        placeholder.setId(question.getId());
        placeholder.setQuestionId(question.getId());
        placeholder.setQuestionObjet(question.getObjet());
        placeholder.setReponseText(question.getDescription() == null || question.getDescription().isBlank()
                ? "Aucune reponse admin pour ce ticket."
                : "Question client: " + question.getDescription());
        placeholder.setCreatedAt(question.getCreatedAt());
        return List.of(placeholder);
    }

    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private String statusClass(String statut) {
        String s = statut != null ? statut.trim().toLowerCase() : "";
        return switch (s) {
            case "ouvert" -> "badge-status-open";
            case "en_traitement", "en traitement" -> "badge-status-progress";
            case "resolu", "résolu" -> "badge-status-done";
            case "ferme", "fermé" -> "badge-status-closed";
            default -> "badge-status-neutral";
        };
    }

    private String priorityClass(String priorite) {
        String p = priorite != null ? priorite.trim().toLowerCase() : "";
        return switch (p) {
            case "haute" -> "badge-priority-high";
            case "basse" -> "badge-priority-low";
            default -> "badge-priority-normal";
        };
    }

    private String formatDate(Question q) {
        return q.getCreatedAt() != null ? q.getCreatedAt().format(DATE_FORMAT) : "";
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private String shortText(String value, int max) {
        if (value == null || value.isBlank()) {
            return "Sans description.";
        }
        String clean = value.trim().replace('\n', ' ');
        if (clean.length() <= max) {
            return clean;
        }
        return clean.substring(0, Math.max(0, max - 3)) + "...";
    }

    private String formatLabel(String v) {
        if (v == null) {
            return "";
        }
        String s = v.trim();
        if (s.isEmpty()) {
            return s;
        }
        return s.substring(0, 1).toUpperCase() + s.substring(1);
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
        boolean isVisible = profileDropdown.isVisible();
        profileDropdown.setVisible(!isVisible);
        profileDropdown.setManaged(!isVisible);
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


    @FXML
    private void goHome() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Accueil.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) profileButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            showError("Navigation", "Impossible d'ouvrir la page d'accueil.");
        }
    }

    @FXML
    private void showProfile() {
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
    }

    @FXML
    private void goToDashboard() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) profileButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            showError("Navigation", "Impossible d'ouvrir le dashboard.");
        }
    }

    @FXML
    private void logout() {
        try {
            userService.logout();
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) profileButton.getScene().getWindow();
            stage.setScene(scene);
        } catch (IOException e) {
            showError("Navigation", "Impossible de se deconnecter.");
        }
    }

    /* =========================
       CHATBOT
       ========================= */

    @FXML
    private void toggleChatbot() {
        if (chatPanel == null) return;
        boolean show = !chatPanel.isVisible();
        chatPanel.setVisible(show);
        chatPanel.setManaged(show);
        if (show) {
            chatPanel.toFront();
            if (chatToggleButton != null) chatToggleButton.toFront();
            Platform.runLater(() -> {
                if (chatInputField != null) chatInputField.requestFocus();
            });
        }
    }

    @FXML
    private void handleChatSend() {
        if (chatInputField == null || chatMessagesContainer == null) return;
        String text = chatInputField.getText();
        if (text == null || text.isBlank()) return;

        chatInputField.clear();
        addUserMessage(text);

        // Show typing indicator
        Label typing = new Label("\u2728 R\u00e9flexion en cours...");
        typing.getStyleClass().add("chat-typing");
        chatMessagesContainer.getChildren().add(typing);
        scrollChatToBottom();

        String userMsg = text;
        java.util.concurrent.CompletableFuture
                .supplyAsync(() -> chatWithAi(userMsg))
                .thenAccept(reply -> Platform.runLater(() -> {
                    chatMessagesContainer.getChildren().remove(typing);
                    addBotMessage(reply);
                }));
    }

    private String chatWithAi(String userMessage) {
        try {
            String systemPrompt = "Vous etes l'assistant virtuel CuraVita, une pharmacie en ligne. " +
                    "Repondez de maniere courte, professionnelle et amicale en francais. " +
                    "Vous pouvez aider avec: medicaments, traitements, utilisation de l'application, " +
                    "commandes, ordonnances, et questions generales de sante. " +
                    "Si la question est hors sujet, redirigez poliment l'utilisateur.";

            // Use the existing Groq chatCompletion via a simple wrapper
            String body = "{" +
                    "\"model\":\"llama-3.1-8b-instant\"," +
                    "\"messages\":[" +
                    "{\"role\":\"system\",\"content\":\"" + escapeJsonChat(systemPrompt) + "\"}," +
                    "{\"role\":\"user\",\"content\":\"" + escapeJsonChat(userMessage) + "\"}" +
                    "]," +
                    "\"temperature\":0.4," +
                    "\"max_tokens\":512" +
                    "}";

            String apiKey = System.getenv("GROQ_API_KEY");
            if (apiKey == null || apiKey.isBlank()) return "Cl\u00e9 API manquante.";

            java.net.http.HttpRequest request = java.net.http.HttpRequest.newBuilder()
                    .uri(java.net.URI.create("https://api.groq.com/openai/v1/chat/completions"))
                    .timeout(java.time.Duration.ofSeconds(30))
                    .header("Authorization", "Bearer " + apiKey)
                    .header("Content-Type", "application/json")
                    .POST(java.net.http.HttpRequest.BodyPublishers.ofString(body))
                    .build();

            java.net.http.HttpClient client = java.net.http.HttpClient.newHttpClient();
            java.net.http.HttpResponse<String> response = client.send(request,
                    java.net.http.HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return "Erreur IA: " + response.statusCode();
            }

            // Extract content from response
            String json = response.body();
            int contentIdx = json.indexOf("\"content\"");
            if (contentIdx < 0) return "Pas de r\u00e9ponse.";
            int colonIdx = json.indexOf(':', contentIdx + 9);
            int firstQ = json.indexOf('"', colonIdx + 1);
            int secondQ = findChatStringEnd(json, firstQ + 1);
            if (firstQ < 0 || secondQ < 0) return "Pas de r\u00e9ponse.";
            return json.substring(firstQ + 1, secondQ)
                    .replace("\\n", "\n").replace("\\t", "\t")
                    .replace("\\\"", "\"").replace("\\\\", "\\");

        } catch (Exception e) {
            return "Erreur: " + e.getMessage();
        }
    }

    private void addUserMessage(String text) {
        // Bubble
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(240);
        bubble.getStyleClass().add("chat-bubble-user");

        // Timestamp
        Label time = new Label(java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        time.getStyleClass().add("chat-time-right");

        VBox msgCol = new VBox(2, bubble, time);
        msgCol.setAlignment(Pos.CENTER_RIGHT);

        // Avatar
        Label avatar = new Label("\uD83D\uDC64");
        avatar.getStyleClass().add("chat-avatar-user");

        HBox row = new HBox(8, msgCol, avatar);
        row.setAlignment(Pos.CENTER_RIGHT);
        chatMessagesContainer.getChildren().add(row);
        scrollChatToBottom();
    }

    private void addBotMessage(String text) {
        // Avatar
        Label avatar = new Label("\uD83E\uDD16");
        avatar.getStyleClass().add("chat-avatar-bot");

        // Bubble
        Label bubble = new Label(text);
        bubble.setWrapText(true);
        bubble.setMaxWidth(240);
        bubble.getStyleClass().add("chat-bubble-bot");

        // Timestamp
        Label time = new Label(java.time.LocalTime.now().format(
                java.time.format.DateTimeFormatter.ofPattern("HH:mm")));
        time.getStyleClass().add("chat-time-left");

        VBox msgCol = new VBox(2, bubble, time);
        msgCol.setAlignment(Pos.CENTER_LEFT);

        HBox row = new HBox(8, avatar, msgCol);
        row.setAlignment(Pos.CENTER_LEFT);
        chatMessagesContainer.getChildren().add(row);
        scrollChatToBottom();
    }

    private void scrollChatToBottom() {
        Platform.runLater(() -> {
            if (chatScrollPane != null) chatScrollPane.setVvalue(1.0);
        });
    }

    private void addSuggestionChips() {
        if (chatMessagesContainer == null) return;

        String[] suggestions = {
            "\uD83D\uDC8A Effets secondaires",
            "\uD83D\uDCE6 Suivi commande",
            "\uD83D\uDCC4 Envoyer ordonnance"
        };
        String[] questions = {
            "Quels sont les effets secondaires possibles d'un médicament ?",
            "Comment suivre l'état de ma commande ?",
            "Comment envoyer mon ordonnance via l'application ?"
        };

        javafx.scene.layout.FlowPane chips = new javafx.scene.layout.FlowPane(8, 8);
        chips.getStyleClass().add("chat-chips-row");

        for (int i = 0; i < suggestions.length; i++) {
            Button chip = new Button(suggestions[i]);
            chip.getStyleClass().add("chat-chip");
            final String q = questions[i];
            chip.setOnAction(e -> {
                chatMessagesContainer.getChildren().remove(chips);
                chatInputField.setText(q);
                handleChatSend();
            });
            chips.getChildren().add(chip);
        }

        chatMessagesContainer.getChildren().add(chips);
        scrollChatToBottom();
    }

    private static int findChatStringEnd(String json, int start) {
        boolean escaped = false;
        for (int i = start; i < json.length(); i++) {
            char c = json.charAt(i);
            if (escaped) { escaped = false; continue; }
            if (c == '\\') { escaped = true; continue; }
            if (c == '"') return i;
        }
        return -1;
    }

    private static String escapeJsonChat(String value) {
        if (value == null) return "";
        return value.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\n", "\\n").replace("\r", "").replace("\t", "\\t");
    }
}
