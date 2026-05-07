package org.example.controller;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import org.example.model.*;
import org.example.service.QuestionService;
import org.example.service.ResponseQuestionService;
import org.example.service.UserService;
import org.example.service.GroqAiService;
import org.example.service.GeminiVisionService;
import org.example.util.PdfExportUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ResponseQuestionAdminController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Filters (Questions)
    @FXML private TextField questionSearchField;
    @FXML private ComboBox<String> questionStatutFilterCombo;
    @FXML private ComboBox<String> questionPrioriteFilterCombo;

    // Questions table
    @FXML private SplitPane mainSplitPane;
    @FXML private TableView<Question> questionTable;
    @FXML private TableColumn<Question, String> clientColumn;
    @FXML private TableColumn<Question, String> objetColumn;
    @FXML private TableColumn<Question, String> typeTicketColumn;
    @FXML private TableColumn<Question, String> dateColumn;
    @FXML private TableColumn<Question, String> prioriteColumn;
    @FXML private TableColumn<Question, String> statutColumn;
    @FXML private TableColumn<Question, Void> questionActionsColumn;
    @FXML private Label questionsCountLabel;

    // Root overlay
    @FXML private StackPane adminRootStack;
    @FXML private StackPane aiSummaryOverlay;
    @FXML private VBox aiSummaryModal;

    // Reply panel
    @FXML private javafx.scene.layout.VBox replyPanel;
    @FXML private Label replyModeLabel;

    @FXML private Label detailClientLabel;
    @FXML private Label detailObjetLabel;
    @FXML private Label detailTypeLabel;
    @FXML private Label detailPrioriteLabel;
    @FXML private TextArea detailDescriptionArea;
    @FXML private Label detailFileLabel;

    // Response form
    @FXML private ComboBox<AuteurType> auteurTypeCombo;
    @FXML private ComboBox<ReponseRole> reponseRoleCombo;
    @FXML private ComboBox<ActionType> actionTypeCombo;
    @FXML private ComboBox<ImpactStatut> impactStatutCombo;
    @FXML private TextArea reponseTextArea;
    @FXML private Label fileInfoLabel;
    @FXML private Label formErrorLabel;
    @FXML private Button saveButton;
    @FXML private Button chooseFileButton;
    @FXML private Button aiSuggestButton;
    @FXML private Label aiSuggestionStatusLabel;
    @FXML private ListView<ResponseQuestion> responsesList;
    @FXML private Label responsesCountLabel;
    @FXML private Button editResponseButton;
    @FXML private Button previewResponseButton;
    @FXML private Button deleteResponseButton;

    private final ResponseQuestionService responseService = new ResponseQuestionService();
    private final QuestionService questionService = new QuestionService();
    private final UserService userService = new UserService();
    private final GroqAiService groqAiService = new GroqAiService();
    private final GeminiVisionService geminiVisionService = new GeminiVisionService();

    private Question selectedQuestion;
    private ResponseQuestion editingResponse;
    private File selectedFile;
    private String aiSuggestedText;
    private boolean aiSuggestionPending;

    @FXML
    public void initialize() {
        // Be defensive: if some fx:id are missing (old FXML in target/ cache), avoid NPE that breaks FXMLLoader.
        setupFilterControls();
        setupQuestionTable();
        setupResponseForm();
        setupResponsesList();

        // Hidden by default until admin clicks “Répondre”
        if (replyPanel != null) {
            replyPanel.setVisible(false);
            replyPanel.setManaged(false);
        }

        reloadQuestions();
    }

    private void setupFilterControls() {
        // Statut/Priorité lists (DB uses lowercase)
        if (questionStatutFilterCombo != null) {
            questionStatutFilterCombo.setItems(FXCollections.observableArrayList("ouvert", "en_cours", "ferme"));
            questionStatutFilterCombo.valueProperty().addListener((obs, o, n) -> reloadQuestions());
        }
        if (questionPrioriteFilterCombo != null) {
            questionPrioriteFilterCombo.setItems(FXCollections.observableArrayList("basse", "normale", "haute"));
            questionPrioriteFilterCombo.valueProperty().addListener((obs, o, n) -> reloadQuestions());
        }

        if (questionSearchField != null) {
            questionSearchField.textProperty().addListener((obs, o, n) -> reloadQuestions());
        }
    }

    private void setupQuestionTable() {
        if (questionTable == null || clientColumn == null || objetColumn == null || typeTicketColumn == null || dateColumn == null
                || prioriteColumn == null || statutColumn == null || questionActionsColumn == null) {
            return;
        }

        clientColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getUtilisateurDisplay())));
        objetColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getObjet())));
        typeTicketColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getTypeTicket())));
        dateColumn.setCellValueFactory(data -> new SimpleStringProperty(formatQuestionDate(data.getValue())));

        // Priority colored text
        prioriteColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getPriorite())));
        prioriteColumn.setCellFactory(col -> new TableCell<>() {
            private final Label label = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    label.getStyleClass().clear();
                    String p = item.toLowerCase();
                    if (p.contains("haute")) {
                        label.getStyleClass().add("priority-high");
                    } else if (p.contains("basse")) {
                        label.getStyleClass().add("priority-low");
                    } else {
                        label.getStyleClass().add("priority-normal");
                    }
                    label.setText(capitalize(p));
                    setGraphic(label);
                }
            }
        });

        // Status pill
        statutColumn.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getStatut())));
        statutColumn.setCellFactory(col -> new TableCell<>() {
            private final Label badge = new Label();

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setGraphic(null);
                } else {
                    badge.getStyleClass().setAll("status-pill");
                    String s = item.toLowerCase();
                    if (s.contains("ouvert")) {
                        badge.getStyleClass().add("status-open");
                    } else if (s.contains("en_cours") || s.contains("trait")) {
                        badge.getStyleClass().add("status-progress");
                    } else {
                        badge.getStyleClass().add("status-closed");
                    }
                    badge.setText(capitalize(s));
                    setGraphic(badge);
                }
            }
        });

        // Actions (Répondre / Supprimer / Export)
        questionActionsColumn.setCellFactory(col -> new TableCell<>() {
            private final Button replyBtn = actionButton("✎", "Répondre", "btn-action-circle", "btn-action-circle-primary");
            private final Button exportBtn = actionButton("⎙", "Exporter PDF", "btn-action-circle", "btn-action-circle-pdf");
            private final Button deleteBtn = actionButton("🗑", "Supprimer réponse", "btn-action-circle", "btn-action-circle-danger");
            private final HBox box = new HBox(8, replyBtn, exportBtn, deleteBtn);

            {
                replyBtn.setOnAction(e -> {
                    Question q = getTableView().getItems().get(getIndex());
                    openReplyForQuestion(q);
                });

                exportBtn.setOnAction(e -> {
                    Question q = getTableView().getItems().get(getIndex());
                    exportPdfForQuestion(q);
                });

                deleteBtn.setOnAction(e -> {
                    Question q = getTableView().getItems().get(getIndex());
                    deleteLatestResponseForQuestion(q);
                });

                box.setMinHeight(Region.USE_PREF_SIZE);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : box);
            }
        });

        questionTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            selectedQuestion = newVal;
            if (newVal != null && replyPanel.isVisible()) {
                fillQuestionDetails(newVal);
            }
        });

        // Fixed cell size for consistent row height calculation
        questionTable.setFixedCellSize(48);
    }

    private void setupResponseForm() {
        if (auteurTypeCombo == null || reponseRoleCombo == null || actionTypeCombo == null || impactStatutCombo == null) {
            return;
        }

        auteurTypeCombo.setItems(FXCollections.observableArrayList(AuteurType.values()));
        reponseRoleCombo.setItems(FXCollections.observableArrayList(ReponseRole.values()));
        actionTypeCombo.setItems(FXCollections.observableArrayList(ActionType.values()));
        impactStatutCombo.setItems(FXCollections.observableArrayList(ImpactStatut.values()));

        // sensible defaults for admin
        auteurTypeCombo.getSelectionModel().select(AuteurType.AGENT);
        reponseRoleCombo.getSelectionModel().select(ReponseRole.SOLUTION);
        actionTypeCombo.getSelectionModel().select(ActionType.AUCUNE);
        impactStatutCombo.getSelectionModel().select(ImpactStatut.AUCUN);

        if (reponseTextArea != null) {
            reponseTextArea.textProperty().addListener((obs, o, n) -> {
                if (n != null && !n.isBlank()) {
                    updateAutoSelectionsFromText(n);
                    clearAiSuggestion();
                }
            });
        }
    }

    private void setupResponsesList() {
        if (responsesList == null) {
            return;
        }
        responsesList.setCellFactory(list -> new ListCell<>() {
            @Override
            protected void updateItem(ResponseQuestion item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String date = item.getCreatedAt() != null ? item.getCreatedAt().format(DATE_FORMAT) : "";
                    String role = item.getReponseRole() != null ? item.getReponseRole().getLabel() : "";
                    String preview = item.getReponseText() != null ? item.getReponseText().trim() : "";
                    if (preview.length() > 80) {
                        preview = preview.substring(0, 80) + "...";
                    }
                    setText(date + " • " + role + " • " + preview);
                }
            }
        });
    }

    /* =========================
       Filters
       ========================= */

    @FXML
    private void handleQuestionFilter() {
        reloadQuestions();
    }

    @FXML
    private void handleClearQuestionFilters() {
        if (questionSearchField != null) questionSearchField.clear();
        questionStatutFilterCombo.setValue(null);
        questionPrioriteFilterCombo.setValue(null);
        reloadQuestions();
    }

    private void reloadQuestions() {
        if (questionTable == null) {
            return;
        }

        String search = questionSearchField != null ? questionSearchField.getText() : null;
        String statut = questionStatutFilterCombo != null ? questionStatutFilterCombo.getValue() : null;
        String priorite = questionPrioriteFilterCombo != null ? questionPrioriteFilterCombo.getValue() : null;

        List<Question> questions = questionService.searchQuestions(search, statut, priorite);
        javafx.collections.ObservableList<Question> items = FXCollections.observableArrayList(questions);
        questionTable.setItems(items);
        if (questionsCountLabel != null) {
            questionsCountLabel.setText(questions.size() + " questions");
        }
        // Force layout refresh so rows render even when parent vgrow is lazy
        Platform.runLater(() -> {
            questionTable.refresh();
            questionTable.requestLayout();
        });
    }

    /* =========================
       Reply panel
       ========================= */

    private void openReplyForQuestion(Question q) {
        if (q == null) return;

        selectedQuestion = q;
        fillQuestionDetails(q);
        clearAiSuggestion();
        loadResponsesForQuestion(q);

        // If already answered, allow editing latest response
        ResponseQuestion latest = responseService.getLatestByQuestionId(q.getId());
        if (latest != null) {
            editingResponse = latest;
            replyModeLabel.setText("Éditer réponse");
            auteurTypeCombo.setValue(latest.getAuteurType());
            reponseRoleCombo.setValue(latest.getReponseRole());
            actionTypeCombo.setValue(latest.getActionType());
            impactStatutCombo.setValue(latest.getImpactStatut());
            reponseTextArea.setText(latest.getReponseText());
            selectedFile = null;
            fileInfoLabel.setText(latest.getFileName() != null ? latest.getFileName() : "Aucun fichier");
        } else {
            editingResponse = null;
            replyModeLabel.setText("Nouvelle réponse");
            reponseTextArea.clear();
            selectedFile = null;
            fileInfoLabel.setText("Aucun fichier");
        }

        replyPanel.setVisible(true);
        replyPanel.setManaged(true);
        if (mainSplitPane != null) {
            mainSplitPane.setDividerPositions(0.70);
        }
    }

    private void fillQuestionDetails(Question q) {
        detailClientLabel.setText(safe(q.getUtilisateurDisplay()));
        detailObjetLabel.setText(safe(q.getObjet()));
        detailTypeLabel.setText(safe(q.getTypeTicket()));
        detailPrioriteLabel.setText(safe(q.getPriorite()));
        detailDescriptionArea.setText(safe(q.getDescription()));
        detailFileLabel.setText(q.getFileName() != null ? q.getFileName() : "Aucun");
    }

    @FXML
    private void handleAnalyzeImage() {
        if (selectedQuestion == null) {
            showInfoAlert("Analyse Image", "Veuillez d'abord sélectionner une question.");
            return;
        }

        String filePath = selectedQuestion.getFilePath();
        String fileName = selectedQuestion.getFileName();
        String fileType = selectedQuestion.getFileType();

        if (filePath == null || filePath.isBlank()) {
            showInfoAlert("Analyse Image", "Cette question n'a pas de pièce jointe.");
            return;
        }

        if (!GeminiVisionService.isImageFile(fileType, fileName)) {
            showInfoAlert("Analyse Image", "La pièce jointe n'est pas une image (" + safe(fileName) + ").");
            return;
        }

        // Show loading in overlay
        showImageAnalysisOverlay("Analyse en cours...", "Gemini analyse l'image : " + safe(fileName), true);

        CompletableFuture
                .supplyAsync(() -> analyzeImageSafe(filePath))
                .thenAccept(result -> Platform.runLater(() ->
                    showImageAnalysisOverlay(result, "Analyse de : " + safe(fileName), false)
                ));
    }

    private String analyzeImageSafe(String filePath) {
        try {
            return geminiVisionService.analyzeImage(filePath);
        } catch (Exception e) {
            return "Erreur analyse image : " + e.getMessage();
        }
    }

    private void showImageAnalysisOverlay(String content, String subtitle, boolean loading) {
        if (aiSummaryOverlay == null || aiSummaryModal == null) {
            showInfoAlert("Analyse Image IA", content);
            return;
        }

        aiSummaryModal.getChildren().clear();

        // Header
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.getStyleClass().add("notification-popup-header");

        Label titleLabel = new Label(loading ? "⏳ Analyse en cours..." : "🔍 Analyse Image IA");
        titleLabel.getStyleClass().add("notification-popup-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("notification-close-btn");
        closeBtn.setOnAction(e -> {
            aiSummaryOverlay.setVisible(false);
            aiSummaryOverlay.setManaged(false);
        });

        header.getChildren().addAll(titleLabel, spacer, closeBtn);

        // Subtitle
        Label sub = new Label(subtitle);
        sub.getStyleClass().add("notification-item-meta");
        sub.setStyle("-fx-padding: 0 0 6 0;");

        // Content
        TextArea area = new TextArea(content);
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefHeight(350);
        area.setStyle(
            "-fx-background-color: #0f1a2e;" +
            "-fx-text-fill: #e2e8f0;" +
            "-fx-control-inner-background: #0f1a2e;" +
            "-fx-font-size: 13;" +
            "-fx-border-color: rgba(22,163,74,0.3);" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;"
        );
        VBox.setVgrow(area, javafx.scene.layout.Priority.ALWAYS);

        // Close button
        Button bottomClose = new Button("Fermer");
        bottomClose.setStyle(
            "-fx-background-color: rgba(22,163,74,0.15);" +
            "-fx-text-fill: #16a34a;" +
            "-fx-font-size: 13;" +
            "-fx-font-weight: 700;" +
            "-fx-padding: 10 24;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;"
        );
        bottomClose.setOnAction(e -> {
            aiSummaryOverlay.setVisible(false);
            aiSummaryOverlay.setManaged(false);
        });

        aiSummaryModal.getChildren().addAll(header, sub, area, bottomClose);

        aiSummaryOverlay.setVisible(true);
        aiSummaryOverlay.setManaged(true);
        aiSummaryOverlay.toFront();
    }

    private void showInfoAlert(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    private void handleCloseReplyPanel() {
        closeReplyPanel();
    }

    private void closeReplyPanel() {
        if (replyPanel != null) {
            replyPanel.setVisible(false);
            replyPanel.setManaged(false);
        }
        if (mainSplitPane != null) {
            mainSplitPane.setDividerPositions(1.0);
        }
        selectedQuestion = null;
        editingResponse = null;
        selectedFile = null;
        clearAiSuggestion();
        clearResponsesList();
    }

    /* =========================
       Response CRUD
       ========================= */

    @FXML
    private void handleChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
        File file = chooser.showOpenDialog(reponseTextArea.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            fileInfoLabel.setText(file.getName());
        }
    }

    @FXML
    private void handleResetForm() {
        formErrorLabel.setText("");
        replyModeLabel.setText(selectedQuestion != null ? "Nouvelle réponse" : "");
        editingResponse = null;
        reponseTextArea.clear();
        selectedFile = null;
        fileInfoLabel.setText("Aucun fichier");
        auteurTypeCombo.getSelectionModel().select(AuteurType.AGENT);
        reponseRoleCombo.getSelectionModel().select(ReponseRole.SOLUTION);
        actionTypeCombo.getSelectionModel().select(ActionType.AUCUNE);
        impactStatutCombo.getSelectionModel().select(ImpactStatut.AUCUN);
        clearAiSuggestion();
    }

    @FXML
    private void handleSave() {
        formErrorLabel.setText("");

        if (selectedQuestion == null) {
            formErrorLabel.setText("Sélectionnez une question puis cliquez sur Répondre.");
            return;
        }

        if (auteurTypeCombo.getValue() == null || reponseRoleCombo.getValue() == null ||
                actionTypeCombo.getValue() == null || impactStatutCombo.getValue() == null) {
            formErrorLabel.setText("Veuillez choisir Auteur, Rôle, Action et Impact.");
            return;
        }

        String responseText = reponseTextArea.getText() != null ? reponseTextArea.getText().trim() : "";
        if (responseText.length() < 3) {
            if (aiSuggestionPending && aiSuggestedText != null && !aiSuggestedText.isBlank()) {
                if (!confirmAiSuggestion()) {
                    return;
                }
                responseText = aiSuggestedText.trim();
                reponseTextArea.setText(responseText);
                clearAiSuggestion();
            } else {
                formErrorLabel.setText("La réponse doit contenir au moins 3 caractères.");
                return;
            }
        } else {
            clearAiSuggestion();
        }

        AuteurType resolvedAuteur = AuteurType.AGENT;
        ReponseRole resolvedRole = resolveRoleFromResponse(responseText);
        ActionType resolvedAction = resolveActionFromResponse(responseText);
        ImpactStatut resolvedImpact = ImpactStatut.FERME;

        auteurTypeCombo.setValue(resolvedAuteur);
        reponseRoleCombo.setValue(resolvedRole);
        actionTypeCombo.setValue(resolvedAction);
        impactStatutCombo.setValue(resolvedImpact);

        ResponseQuestion response = editingResponse != null ? editingResponse : new ResponseQuestion();
        response.setQuestionId(selectedQuestion.getId());
        response.setAuteurType(resolvedAuteur);
        response.setReponseRole(resolvedRole);
        response.setActionType(resolvedAction);
        response.setImpactStatut(resolvedImpact);
        response.setReponseText(responseText);

        // lu_par_client is managed by client view, keep default false
        response.setLuParClient(false);

        // optional utilisateur for response = current admin if available
        User currentUser = userService.getCurrentUser();
        response.setUtilisateurId(currentUser != null ? currentUser.getId() : null);

        if (selectedFile != null) {
            FileMetadata meta = storeFile(selectedFile);
            response.setFileName(meta.fileName);
            response.setFilePath(meta.filePath);
            response.setFileType(meta.fileType);
            response.setFileSize(meta.fileSize);
        }

        if (editingResponse == null) {
            int id = responseService.create(response);
            if (id == 0) {
                formErrorLabel.setText("Erreur lors de la création de la réponse.");
                return;
            }
        } else {
            if (!responseService.update(response)) {
                formErrorLabel.setText("Erreur lors de la mise à jour de la réponse.");
                return;
            }
        }

        if (!questionService.updateQuestionStatus(selectedQuestion.getId(), "ferme")) {
            formErrorLabel.setText("Réponse envoyée, mais statut non mis à jour.");
        }

        handleResetForm();
        loadResponsesForQuestion(selectedQuestion);
        reloadQuestions();
    }

    @FXML
    private void handleEditSelectedResponse() {
        if (responsesList == null) {
            return;
        }
        ResponseQuestion selected = responsesList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            formErrorLabel.setText("Sélectionnez une réponse à modifier.");
            return;
        }
        loadResponseForEdit(selected);
    }

    private void loadResponsesForQuestion(Question question) {
        if (question == null || responsesList == null) {
            return;
        }
        ResponseQuestionFilter filter = new ResponseQuestionFilter();
        filter.setQuestionId(question.getId());
        List<ResponseQuestion> items = responseService.findAllFiltered(filter, "createdAt", false);
        responsesList.setItems(FXCollections.observableArrayList(items));
        if (responsesCountLabel != null) {
            responsesCountLabel.setText(items.size() + " réponses");
        }
    }

    private void clearResponsesList() {
        if (responsesList != null) {
            responsesList.getItems().clear();
        }
        if (responsesCountLabel != null) {
            responsesCountLabel.setText("");
        }
    }

    private void loadResponseForEdit(ResponseQuestion response) {
        editingResponse = response;
        replyModeLabel.setText("Éditer réponse");
        auteurTypeCombo.setValue(response.getAuteurType());
        reponseRoleCombo.setValue(response.getReponseRole());
        actionTypeCombo.setValue(response.getActionType());
        impactStatutCombo.setValue(response.getImpactStatut());
        reponseTextArea.setText(response.getReponseText());
        selectedFile = null;
        fileInfoLabel.setText(response.getFileName() != null ? response.getFileName() : "Aucun fichier");
        updateAutoSelectionsFromText(response.getReponseText());
        clearAiSuggestion();
    }

    @FXML
    private void handlePreviewSelectedResponse() {
        ResponseQuestion selected = getSelectedResponseOrWarn();
        if (selected == null) {
            return;
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Aperçu réponse");
        alert.setHeaderText(selected.getQuestionObjet() != null ? selected.getQuestionObjet() : "Réponse");

        String meta = "Auteur: " + labelOf(selected.getAuteurType()) + "\n" +
                "Rôle: " + labelOf(selected.getReponseRole()) + "\n" +
                "Action: " + labelOf(selected.getActionType()) + "\n" +
                "Impact: " + labelOf(selected.getImpactStatut()) + "\n";

        TextArea area = new TextArea(meta + "\n" + safe(selected.getReponseText()));
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefWidth(600);
        area.setPrefHeight(360);

        alert.getDialogPane().setContent(area);
        alert.showAndWait();
    }

    @FXML
    private void handleDeleteSelectedResponse() {
        ResponseQuestion selected = getSelectedResponseOrWarn();
        if (selected == null) {
            return;
        }
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer cette réponse ?");
        alert.setContentText("Cette action est irréversible.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            responseService.delete(selected.getId());
            if (editingResponse != null && editingResponse.getId() == selected.getId()) {
                handleResetForm();
            }
            loadResponsesForQuestion(selectedQuestion);
        }
    }

    private ResponseQuestion getSelectedResponseOrWarn() {
        if (responsesList == null) {
            return null;
        }
        ResponseQuestion selected = responsesList.getSelectionModel().getSelectedItem();
        if (selected == null) {
            formErrorLabel.setText("Sélectionnez une réponse.");
            return null;
        }
        return selected;
    }

    private void updateAutoSelectionsFromText(String responseText) {
        AuteurType resolvedAuteur = AuteurType.AGENT;
        ReponseRole resolvedRole = resolveRoleFromResponse(responseText);
        ActionType resolvedAction = resolveActionFromResponse(responseText);
        ImpactStatut resolvedImpact = ImpactStatut.FERME;

        auteurTypeCombo.setValue(resolvedAuteur);
        reponseRoleCombo.setValue(resolvedRole);
        actionTypeCombo.setValue(resolvedAction);
        impactStatutCombo.setValue(resolvedImpact);
    }

    private String labelOf(AuteurType type) {
        return type != null ? type.getLabel() : "";
    }

    private String labelOf(ReponseRole role) {
        return role != null ? role.getLabel() : "";
    }

    private String labelOf(ActionType action) {
        return action != null ? action.getLabel() : "";
    }

    private String labelOf(ImpactStatut impact) {
        return impact != null ? impact.getLabel() : "";
    }

    /* =========================
       PDF export
       ========================= */

    @FXML
    private void handleExportPdf() {
        try {
            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exporter PDF");
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            chooser.setInitialFileName("response_questions.pdf");

            File file = chooser.showSaveDialog(questionTable.getScene().getWindow());
            if (file == null) return;

            // Export current filtered list: responses linked to currently displayed questions
            // (simple approach: export all responses without extra filter)
            List<ResponseQuestion> data = responseService.findAllFiltered(new ResponseQuestionFilter(), "createdAt", false);
            PdfExportUtil.exportResponseQuestions(data, file.toPath());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void exportPdfForQuestion(Question q) {
        if (q == null) return;
        try {
            ResponseQuestionFilter filter = new ResponseQuestionFilter();
            filter.setQuestionId(q.getId());
            List<ResponseQuestion> data = responseService.findAllFiltered(filter, "createdAt", false);

            FileChooser chooser = new FileChooser();
            chooser.setTitle("Exporter PDF - " + safe(q.getObjet()));
            chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF", "*.pdf"));
            chooser.setInitialFileName("ticket_" + q.getId() + ".pdf");

            File file = chooser.showSaveDialog(questionTable.getScene().getWindow());
            if (file != null) {
                PdfExportUtil.exportResponseQuestions(data, file.toPath());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /* =========================
       Helpers
       ========================= */

    private FileMetadata storeFile(File file) {
        try {
            Path uploadDir = Path.of("uploads", "response_questions");
            Files.createDirectories(uploadDir);

            String fileName = UUID.randomUUID().toString() + "_" + file.getName();
            Path destination = uploadDir.resolve(fileName);
            Files.copy(file.toPath(), destination, StandardCopyOption.REPLACE_EXISTING);

            String fileType = Files.probeContentType(destination);
            long size = Files.size(destination);
            int sizeInt = (int) Math.min(Integer.MAX_VALUE, size);

            return new FileMetadata(file.getName(), destination.toString(), fileType != null ? fileType : "", sizeInt);
        } catch (IOException e) {
            e.printStackTrace();
            return new FileMetadata(file.getName(), file.getAbsolutePath(), "", (int) Math.min(Integer.MAX_VALUE, file.length()));
        }
    }

    private static class FileMetadata {
        private final String fileName;
        private final String filePath;
        private final String fileType;
        private final Integer fileSize;

        FileMetadata(String fileName, String filePath, String fileType, Integer fileSize) {
            this.fileName = fileName;
            this.filePath = filePath;
            this.fileType = fileType;
            this.fileSize = fileSize;
        }
    }

    private String formatQuestionDate(Question question) {
        return question.getCreatedAt() != null ? question.getCreatedAt().format(DATE_FORMAT) : "";
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }

    private String capitalize(String v) {
        if (v == null || v.isBlank()) return "";
        String s = v.trim().replace('_', ' ');
        return Character.toUpperCase(s.charAt(0)) + s.substring(1);
    }

    private ReponseRole resolveRoleFromResponse(String responseText) {
        String text = responseText != null ? responseText.toLowerCase() : "";
        if (text.contains("solution") || text.contains("problème résolu")) {
            return ReponseRole.SOLUTION;
        } else if (text.contains("décision") || text.contains("decision")) {
            return ReponseRole.DECISION;
        } else if (text.contains("info") || text.contains("information")) {
            return ReponseRole.INFO;
        } else if (text.contains("demande") || text.contains("preuve")) {
            return ReponseRole.DEMANDE_PREUVE;
        }
        return ReponseRole.QUESTION;
    }

    private ActionType resolveActionFromResponse(String responseText) {
        String text = responseText != null ? responseText.toLowerCase() : "";
        if (text.contains("remboursement")) {
            return ActionType.REMBOURSEMENT;
        } else if (text.contains("remplacement")) {
            return ActionType.REMPLACEMENT;
        } else if (text.contains("retour accept")) {
            return ActionType.RETOUR_ACCEPTE;
        } else if (text.contains("retour refus")) {
            return ActionType.RETOUR_REFUSE;
        } else if (text.contains("escalade")) {
            return ActionType.ESCALADE;
        }
        return ActionType.AUCUNE;
    }

    private void deleteLatestResponseForQuestion(Question question) {
        if (question == null) return;
        ResponseQuestion latest = responseService.getLatestByQuestionId(question.getId());
        if (latest != null) {
            Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
            alert.setTitle("Suppression");
            alert.setHeaderText("Supprimer la réponse ?");
            alert.setContentText("Cette action est irréversible.");
            if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                responseService.delete(latest.getId());
                loadResponsesForQuestion(question);
                reloadQuestions();
            }
        } else {
            Alert info = new Alert(Alert.AlertType.INFORMATION);
            info.setTitle("Information");
            info.setHeaderText(null);
            info.setContentText("Aucune réponse à supprimer pour cette question.");
            info.showAndWait();
        }
    }

    private Button actionButton(String icon, String tooltip, String... styleClasses) {
        Button b = new Button(icon);
        b.getStyleClass().addAll(styleClasses);
        b.setTooltip(new Tooltip(tooltip));
        return b;
    }

    @FXML
    private void handleSummarizeByPriority() {
        if (questionTable == null || questionTable.getItems().isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Résumé IA");
            alert.setHeaderText(null);
            alert.setContentText("Aucune question à résumer.");
            alert.showAndWait();
            return;
        }

        String groupedText = buildGroupedQuestions(questionTable.getItems());
        CompletableFuture
                .supplyAsync(() -> summarizeSafe(groupedText))
                .thenAccept(summary -> Platform.runLater(() -> showSummaryDialog(summary)));
    }

    @FXML
    private void handleSuggestResponseAi() {
        formErrorLabel.setText("");
        if (selectedQuestion == null) {
            formErrorLabel.setText("Sélectionnez une question puis cliquez sur Répondre.");
            return;
        }
        if (aiSuggestButton != null) {
            aiSuggestButton.setDisable(true);
        }
        if (aiSuggestionStatusLabel != null) {
            aiSuggestionStatusLabel.setText("Génération...");
        }

        CompletableFuture
                .supplyAsync(() -> suggestResponseSafe(selectedQuestion))
                .thenAccept(text -> Platform.runLater(() -> applyAiSuggestion(text)));
    }

    private String summarizeSafe(String groupedText) {
        try {
            return groqAiService.summarizeByPriority(groupedText);
        } catch (Exception e) {
            return "Erreur IA: " + e.getMessage();
        }
    }

    private String suggestResponseSafe(Question question) {
        try {
            return groqAiService.suggestResponse(question.getObjet(), question.getDescription(), question.getPriorite());
        } catch (Exception e) {
            return "Erreur IA: " + e.getMessage();
        }
    }

    private void applyAiSuggestion(String suggestion) {
        if (aiSuggestButton != null) {
            aiSuggestButton.setDisable(false);
        }
        if (suggestion == null || suggestion.isBlank()) {
            if (aiSuggestionStatusLabel != null) {
                aiSuggestionStatusLabel.setText("");
            }
            formErrorLabel.setText("Impossible de générer la suggestion IA.");
            return;
        }
        if (suggestion.startsWith("Erreur IA:")) {
            if (aiSuggestionStatusLabel != null) {
                aiSuggestionStatusLabel.setText("");
            }
            formErrorLabel.setText(suggestion);
            return;
        }

        aiSuggestedText = suggestion.trim();
        aiSuggestionPending = true;
        if (reponseTextArea != null) {
            reponseTextArea.clear();
            reponseTextArea.setPromptText(aiSuggestedText);
        }

        updateAutoSelectionsFromText(aiSuggestedText);

        if (aiSuggestionStatusLabel != null) {
            aiSuggestionStatusLabel.setText("Suggestion prête");
        }
    }

    private void clearAiSuggestion() {
        aiSuggestedText = null;
        aiSuggestionPending = false;
        if (reponseTextArea != null) {
            reponseTextArea.setPromptText("Tapez votre réponse...");
        }
        if (aiSuggestionStatusLabel != null) {
            aiSuggestionStatusLabel.setText("");
        }
    }

    private boolean confirmAiSuggestion() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation IA");
        alert.setHeaderText("Envoyer la suggestion IA ?");
        alert.setContentText("La suggestion IA sera utilisée comme réponse.");
        return alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK;
    }

    private String buildGroupedQuestions(List<Question> questions) {
        StringBuilder sb = new StringBuilder();
        sb.append("Questions par priorité\n\n");

        List<Question> haute = new ArrayList<>();
        List<Question> normale = new ArrayList<>();
        List<Question> basse = new ArrayList<>();

        for (Question q : questions) {
            String p = q.getPriorite() != null ? q.getPriorite().toLowerCase() : "";
            if (p.contains("haute")) {
                haute.add(q);
            } else if (p.contains("basse")) {
                basse.add(q);
            } else {
                normale.add(q);
            }
        }

        appendQuestions(sb, "Haute", haute);
        appendQuestions(sb, "Normale", normale);
        appendQuestions(sb, "Basse", basse);

        return sb.toString();
    }

    private void appendQuestions(StringBuilder sb, String title, List<Question> questions) {
        sb.append(title).append(":\n");
        if (questions.isEmpty()) {
            sb.append("- Aucune\n\n");
            return;
        }
        for (Question q : questions) {
            sb.append("- ").append(safe(q.getObjet())).append(" | ")
                    .append(safe(q.getDescription())).append("\n");
        }
        sb.append("\n");
    }

    private void showSummaryDialog(String summary) {
        if (aiSummaryOverlay == null || aiSummaryModal == null) {
            // Fallback if FXML fields not injected
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Résumé IA");
            alert.setHeaderText("Synthèse des questions par priorité");
            TextArea area = new TextArea(summary != null ? summary : "");
            area.setEditable(false);
            area.setWrapText(true);
            alert.getDialogPane().setContent(area);
            alert.showAndWait();
            return;
        }

        aiSummaryModal.getChildren().clear();

        // Header row
        HBox header = new HBox(10);
        header.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        header.getStyleClass().add("notification-popup-header");

        Label titleLabel = new Label("Résumé IA");
        titleLabel.getStyleClass().add("notification-popup-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, javafx.scene.layout.Priority.ALWAYS);

        Button closeBtn = new Button("✕");
        closeBtn.getStyleClass().add("notification-close-btn");
        closeBtn.setOnAction(e -> {
            aiSummaryOverlay.setVisible(false);
            aiSummaryOverlay.setManaged(false);
        });

        header.getChildren().addAll(titleLabel, spacer, closeBtn);

        // Subtitle
        Label subtitle = new Label("Synthèse des questions par priorité");
        subtitle.getStyleClass().add("notification-item-meta");
        subtitle.setStyle("-fx-padding: 0 0 6 0;");

        // Summary text in a scroll pane
        TextArea area = new TextArea(summary != null ? summary : "");
        area.setEditable(false);
        area.setWrapText(true);
        area.setPrefHeight(380);
        area.setStyle(
            "-fx-background-color: #0f1a2e;" +
            "-fx-text-fill: #e2e8f0;" +
            "-fx-control-inner-background: #0f1a2e;" +
            "-fx-font-size: 13;" +
            "-fx-border-color: rgba(22,163,74,0.3);" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;"
        );
        VBox.setVgrow(area, javafx.scene.layout.Priority.ALWAYS);

        // Close button at bottom
        Button bottomClose = new Button("Fermer");
        bottomClose.getStyleClass().addAll("notification-close-btn");
        bottomClose.setStyle(
            "-fx-background-color: rgba(22,163,74,0.15);" +
            "-fx-text-fill: #16a34a;" +
            "-fx-font-size: 13;" +
            "-fx-font-weight: 700;" +
            "-fx-padding: 10 24;" +
            "-fx-background-radius: 10;" +
            "-fx-cursor: hand;"
        );
        bottomClose.setOnAction(e -> {
            aiSummaryOverlay.setVisible(false);
            aiSummaryOverlay.setManaged(false);
        });

        aiSummaryModal.getChildren().addAll(header, subtitle, area, bottomClose);

        aiSummaryOverlay.setVisible(true);
        aiSummaryOverlay.setManaged(true);
        aiSummaryOverlay.toFront();
    }
}
