package org.example.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.stage.FileChooser;
import org.example.model.*;
import org.example.service.QuestionService;
import org.example.service.ResponseQuestionService;
import org.example.service.UserService;
import org.example.util.PdfExportUtil;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

public class ResponseQuestionAdminController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Filters (Questions)
    @FXML private TextField questionSearchField;
    @FXML private ComboBox<String> questionStatutFilterCombo;
    @FXML private ComboBox<String> questionPrioriteFilterCombo;

    // Questions table
    @FXML private TableView<Question> questionTable;
    @FXML private TableColumn<Question, String> clientColumn;
    @FXML private TableColumn<Question, String> objetColumn;
    @FXML private TableColumn<Question, String> typeTicketColumn;
    @FXML private TableColumn<Question, String> dateColumn;
    @FXML private TableColumn<Question, String> prioriteColumn;
    @FXML private TableColumn<Question, String> statutColumn;
    @FXML private TableColumn<Question, Void> questionActionsColumn;
    @FXML private Label questionsCountLabel;

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

    private final ResponseQuestionService responseService = new ResponseQuestionService();
    private final QuestionService questionService = new QuestionService();
    private final UserService userService = new UserService();

    private Question selectedQuestion;
    private ResponseQuestion editingResponse;
    private File selectedFile;

    @FXML
    public void initialize() {
        // Be defensive: if some fx:id are missing (old FXML in target/ cache), avoid NPE that breaks FXMLLoader.
        setupFilterControls();
        setupQuestionTable();
        setupResponseForm();

        // Hidden by default until admin clicks “Répondre”
        closeReplyPanel();

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
        questionTable.setItems(FXCollections.observableArrayList(questions));
        if (questionsCountLabel != null) {
            questionsCountLabel.setText(questions.size() + " questions");
        }
    }

    /* =========================
       Reply panel
       ========================= */

    private void openReplyForQuestion(Question q) {
        if (q == null) return;

        selectedQuestion = q;
        fillQuestionDetails(q);

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
    private void handleCloseReplyPanel() {
        closeReplyPanel();
    }

    private void closeReplyPanel() {
        if (replyPanel != null) {
            replyPanel.setVisible(false);
            replyPanel.setManaged(false);
        }
        selectedQuestion = null;
        editingResponse = null;
        selectedFile = null;
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
            formErrorLabel.setText("La réponse doit contenir au moins 3 caractères.");
            return;
        }

        ResponseQuestion response = editingResponse != null ? editingResponse : new ResponseQuestion();
        response.setQuestionId(selectedQuestion.getId());
        response.setAuteurType(auteurTypeCombo.getValue());
        response.setReponseRole(reponseRoleCombo.getValue());
        response.setActionType(actionTypeCombo.getValue());
        response.setImpactStatut(impactStatutCombo.getValue());
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

        // Optionnel: on peut marquer la question comme 'en_cours' ou 'ferme' via service plus tard
        handleResetForm();
        reloadQuestions();
    }

    private void deleteLatestResponseForQuestion(Question q) {
        if (q == null) return;

        ResponseQuestion latest = responseService.getLatestByQuestionId(q.getId());
        if (latest == null) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Suppression");
            alert.setHeaderText(null);
            alert.setContentText("Aucune réponse à supprimer pour cette question.");
            alert.showAndWait();
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Suppression");
        alert.setHeaderText("Supprimer la dernière réponse ?");
        alert.setContentText("Cette action est irréversible.");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            responseService.delete(latest.getId());
            if (editingResponse != null && editingResponse.getId() == latest.getId()) {
                handleResetForm();
            }
            reloadQuestions();
        }
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

    private Button actionButton(String icon, String tooltip, String... styleClasses) {
        Button b = new Button(icon);
        b.getStyleClass().addAll(styleClasses);
        b.setTooltip(new Tooltip(tooltip));
        return b;
    }
}
