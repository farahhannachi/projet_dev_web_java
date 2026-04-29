package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Button;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.TextInputControl;
import javafx.application.Platform;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.model.Question;
import org.example.model.User;
import org.example.service.QuestionService;
import org.example.service.UserService;
import org.example.service.SpeechToTextService;
import org.example.service.GroqAiService;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ContactQuestionController {
    private static ContactQuestionController activeInstance;

    @FXML private Label formTitleLabel;
    @FXML private Label formSubtitleLabel;
    @FXML private TextField objetField;
    @FXML private ComboBox<String> typeTicketCombo;
    @FXML private TextArea descriptionArea;
    @FXML private Label autoPriorityLabel;
    @FXML private Label fileInfoLabel;
    @FXML private Label formErrorLabel;
    @FXML private Label formSuccessLabel;
    @FXML private Button submitButton;
    @FXML private Button objetMicButton;
    @FXML private Button descriptionMicButton;

    private final QuestionService questionService = new QuestionService();
    private final UserService userService = new UserService();
    private final SpeechToTextService speechToTextService = new SpeechToTextService();
    private final GroqAiService groqAiService = new GroqAiService();
    private final HttpClient httpClient = HttpClient.newHttpClient();
    private TextInputControl activeMicTarget;

    // Profanity filter API key - configure via environment variable: PROFANITY_API_KEY
    private static final String PROFANITY_API_KEY = System.getenv().getOrDefault("PROFANITY_API_KEY", "");
    private static final String PROFANITY_API_URL = "https://api.api-ninjas.com/v1/profanityfilter";
    private Button activeMicButton;
    private boolean submitting;
    private File selectedFile;
    private Question editingQuestion;
    private Runnable onQuestionSaved;

    @FXML
    public void initialize() {
        activeInstance = this;
        typeTicketCombo.setItems(FXCollections.observableArrayList(
                "general", "commande", "livraison", "remboursement", "produit", "autre"));
        typeTicketCombo.getSelectionModel().select("general");
        if (autoPriorityLabel != null) {
            autoPriorityLabel.setText("Automatique");
        }
    }

    public static ContactQuestionController getActiveInstance() {
        return activeInstance;
    }

    @FXML
    private void handleChooseFile() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Choisir un fichier");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"));
        File file = chooser.showOpenDialog(objetField.getScene().getWindow());
        if (file != null) {
            selectedFile = file;
            fileInfoLabel.setText(file.getName());
        }
    }

    @FXML
    private void handleSubmit() {
        stopSpeechIfRunning();
        if (submitting) {
            return;
        }
        formErrorLabel.setText("");
        formSuccessLabel.setText("");

        String objet = objetField.getText() != null ? objetField.getText().trim() : "";
        String description = descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";
        String typeTicket = typeTicketCombo.getValue();

        if (objet.isEmpty() || description.length() < 3) {
            formErrorLabel.setText("Objet et description (min 3 caracteres) requis.");
            return;
        }
        if (typeTicket == null) {
            formErrorLabel.setText("Type requis.");
            return;
        }

        User currentUser = userService.getCurrentUser();
        Integer userId = currentUser != null ? currentUser.getId() : null;

        if (userId == null) {
            formErrorLabel.setText("Session invalide. Veuillez vous reconnecter.");
            return;
        }

        FileMetadata metadata = null;
        if (selectedFile != null) {
            metadata = storeFile(selectedFile);
        }

        String statusValue = editingQuestion != null && editingQuestion.getStatut() != null
                ? editingQuestion.getStatut()
                : "ouvert";

        submitting = true;
        if (submitButton != null) {
            submitButton.setDisable(true);
        }
        if (autoPriorityLabel != null) {
            autoPriorityLabel.setText("Detection...");
        }

        FileMetadata finalMetadata = metadata;
        
        // First check for profanity
        CompletableFuture
                .supplyAsync(() -> checkProfanitySafe(objet + " " + description))
                .thenAccept(hasProfanity -> Platform.runLater(() -> {
                    if (hasProfanity) {
                        submitting = false;
                        if (submitButton != null) {
                            submitButton.setDisable(false);
                        }
                        if (autoPriorityLabel != null) {
                            autoPriorityLabel.setText("");
                        }
                        formErrorLabel.setText("Votre message contient des termes inappropriés. Veuillez modifier votre texte.");
                        return;
                    }
                    
                    // No profanity, proceed with priority detection and save
                    CompletableFuture
                            .supplyAsync(() -> detectPrioritySafe(objet, description))
                            .thenAccept(priority -> Platform.runLater(() -> saveQuestionWithPriority(
                                    objet,
                                    description,
                                    typeTicket,
                                    priority,
                                    statusValue,
                                    finalMetadata,
                                    userId
                            )));
                }));
    }

    private String detectPrioritySafe(String objet, String description) {
        String localOverride = heuristicPriority(objet + " " + description);
        if (localOverride != null) {
            return localOverride;
        }
        try {
            String result = groqAiService.detectPriority(objet, description);
            return normalizePriority(result);
        } catch (Exception e) {
            return "normale";
        }
    }

    /**
     * Checks if text contains profanity using API Ninjas profanity filter.
     * Returns true if profanity is detected, false otherwise.
     * If API key is not configured, allows all text (returns false).
     */
    private boolean checkProfanitySafe(String text) {
        if (PROFANITY_API_KEY == null || PROFANITY_API_KEY.isBlank()) {
            System.err.println("WARNING: PROFANITY_API_KEY not configured. Skipping profanity check.");
            return false; // Allow if no API key configured
        }
        
        if (text == null || text.isBlank()) {
            return false;
        }
        
        try {
            String encodedText = URLEncoder.encode(text, StandardCharsets.UTF_8);
            String url = PROFANITY_API_URL + "?text=" + encodedText;
            
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("X-Api-Key", PROFANITY_API_KEY)
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() == 200) {
                String body = response.body();
                // API returns {"has_profanity": true/false, "profanity_count": n, "censored": "..."}
                // Simple parsing - look for "has_profanity": true
                return body.contains("\"has_profanity\": true") || body.contains("\"has_profanity\":true");
            } else {
                System.err.println("Profanity API error: HTTP " + response.statusCode() + " - " + response.body());
                return false; // Allow on API error (fail open)
            }
        } catch (Exception e) {
            System.err.println("Profanity check failed: " + e.getMessage());
            e.printStackTrace();
            return false; // Allow on exception (fail open)
        }
    }

    private String heuristicPriority(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String t = text.toLowerCase();
        String[] highKeywords = {
                "urgence", "urgent", "urgence vitale", "risque", "critique",
                "mourir", "mort", "inconscient", "respire", "respirer",
                "sang", "hemorragie", "hémorragie", "douleur intense", "douleur",
                "fièvre", "fievre", "convulsion", "choc"
        };
        for (String keyword : highKeywords) {
            if (t.contains(keyword)) {
                return "haute";
            }
        }
        return null;
    }

    private String normalizePriority(String value) {
        if (value == null) {
            return "normale";
        }
        String v = value.trim().toLowerCase();
        if (v.contains("haute")) {
            return "haute";
        }
        if (v.contains("basse")) {
            return "basse";
        }
        return "normale";
    }

    private void saveQuestionWithPriority(String objet,
                                          String description,
                                          String typeTicket,
                                          String priorite,
                                          String statusValue,
                                          FileMetadata metadata,
                                          Integer userId) {
        String fileNameValue;
        String filePathValue;
        String fileTypeValue;
        Integer fileSizeValue;

        if (metadata != null) {
            fileNameValue = metadata.fileName;
            filePathValue = metadata.filePath;
            fileTypeValue = metadata.fileType;
            fileSizeValue = metadata.fileSize;
        } else if (editingQuestion != null) {
            fileNameValue = editingQuestion.getFileName();
            filePathValue = editingQuestion.getFilePath();
            fileTypeValue = editingQuestion.getFileType();
            fileSizeValue = editingQuestion.getFileSize();
        } else {
            fileNameValue = null;
            filePathValue = null;
            fileTypeValue = null;
            fileSizeValue = null;
        }

        boolean saved;
        if (editingQuestion == null) {
            saved = questionService.createQuestion(
                    typeTicket,
                    objet,
                    description,
                    priorite,
                    statusValue,
                    fileNameValue,
                    filePathValue,
                    fileTypeValue,
                    fileSizeValue,
                    LocalDateTime.now(),
                    userId
            );
        } else {
            saved = questionService.updateQuestionForUser(
                    editingQuestion.getId(),
                    userId,
                    typeTicket,
                    objet,
                    description,
                    priorite,
                    statusValue,
                    fileNameValue,
                    filePathValue,
                    fileTypeValue,
                    fileSizeValue
            );
        }

        if (autoPriorityLabel != null) {
            autoPriorityLabel.setText(capitalizePriority(priorite));
        }

        if (saved) {
            if (editingQuestion == null) {
                formSuccessLabel.setText("Question envoyee avec succes.");
            } else {
                formSuccessLabel.setText("Question modifiee avec succes.");
            }
            resetFormToCreateMode();
            if (onQuestionSaved != null) {
                onQuestionSaved.run();
            }
        } else {
            formErrorLabel.setText(editingQuestion == null
                    ? "Erreur lors de l'envoi."
                    : "Erreur lors de la modification.");
        }

        submitting = false;
        if (submitButton != null) {
            submitButton.setDisable(false);
        }
    }

    private String capitalizePriority(String value) {
        if (value == null || value.isBlank()) {
            return "Automatique";
        }
        String v = value.trim().toLowerCase();
        return Character.toUpperCase(v.charAt(0)) + v.substring(1);
    }

    @FXML
    private void handleClose() {
        stopSpeechIfRunning();
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/Accueil.fxml"));
            javafx.scene.Parent root = loader.load();
            javafx.scene.Scene scene = new javafx.scene.Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) objetField.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleObjetMic() {
        toggleSpeech(objetField, objetMicButton);
    }

    @FXML
    private void handleDescriptionMic() {
        toggleSpeech(descriptionArea, descriptionMicButton);
    }

    private void toggleSpeech(TextInputControl target, Button sourceButton) {
        if (speechToTextService.isRunning()) {
            stopSpeechIfRunning();
            return;
        }
        activeMicTarget = target;
        activeMicButton = sourceButton;
        updateMicButtons(true);
        formErrorLabel.setText("");
        formSuccessLabel.setText("");

        try {
            speechToTextService.start(
                    partial -> Platform.runLater(() -> updatePartialText(partial)),
                    text -> Platform.runLater(() -> appendFinalText(text)),
                    error -> Platform.runLater(() -> handleSpeechError(error))
            );
        } catch (IOException e) {
            handleSpeechError(e.getMessage());
        }
    }

    private void updatePartialText(String partial) {
        if (activeMicTarget == null || partial == null || partial.isBlank()) {
            return;
        }
        String existing = activeMicTarget.getText();
        if (existing == null || existing.isBlank()) {
            activeMicTarget.setText(partial);
        }
    }

    private void appendFinalText(String text) {
        if (activeMicTarget == null || text == null || text.isBlank()) {
            return;
        }
        String existing = activeMicTarget.getText();
        String spacer = (existing == null || existing.isBlank()) ? "" : " ";
        activeMicTarget.setText((existing == null ? "" : existing) + spacer + text);
    }

    private void handleSpeechError(String message) {
        stopSpeechIfRunning();
        if (formErrorLabel != null) {
            String suffix = message != null ? ": " + message : "";
            formErrorLabel.setText("Erreur micro" + suffix);
        }
    }

    private void stopSpeechIfRunning() {
        if (speechToTextService.isRunning()) {
            speechToTextService.stop();
        }
        updateMicButtons(false);
        activeMicTarget = null;
        activeMicButton = null;
    }

    private void updateMicButtons(boolean listening) {
        if (objetMicButton != null) {
            objetMicButton.setText(listening && objetMicButton == activeMicButton ? "⏹" : "🎤");
        }
        if (descriptionMicButton != null) {
            descriptionMicButton.setText(listening && descriptionMicButton == activeMicButton ? "⏹" : "🎤");
        }
    }

    private void resetFormToCreateMode() {
        stopSpeechIfRunning();
        objetField.clear();
        descriptionArea.clear();
        selectedFile = null;
        editingQuestion = null;
        fileInfoLabel.setText("Aucun fichier");
        typeTicketCombo.getSelectionModel().select("general");
        if (autoPriorityLabel != null) {
            autoPriorityLabel.setText("Automatique");
        }
        if (formTitleLabel != null) {
            formTitleLabel.setText("Nouveau Ticket");
        }
        if (formSubtitleLabel != null) {
            formSubtitleLabel.setText("Decrivez votre probleme en detail");
        }
        if (submitButton != null) {
            submitButton.setText("Envoyer le ticket");
        }
    }

    public void setOnQuestionSaved(Runnable onQuestionSaved) {
        this.onQuestionSaved = onQuestionSaved;
    }

    public void startEditQuestion(Question question) {
        if (question == null) {
            return;
        }
        editingQuestion = question;
        selectedFile = null;
        formErrorLabel.setText("");
        formSuccessLabel.setText("");

        if (formTitleLabel != null) {
            formTitleLabel.setText("Modifier Ticket");
        }
        if (formSubtitleLabel != null) {
            formSubtitleLabel.setText("Mettez a jour les informations de votre ticket");
        }
        if (submitButton != null) {
            submitButton.setText("Enregistrer les modifications");
        }

        objetField.setText(question.getObjet());
        descriptionArea.setText(question.getDescription());
        if (autoPriorityLabel != null) {
            autoPriorityLabel.setText(capitalizePriority(question.getPriorite()));
        }

        if (question.getTypeTicket() != null && typeTicketCombo.getItems().contains(question.getTypeTicket())) {
            typeTicketCombo.getSelectionModel().select(question.getTypeTicket());
        }

        if (question.getFileName() != null && !question.getFileName().isBlank()) {
            fileInfoLabel.setText(question.getFileName());
        } else {
            fileInfoLabel.setText("Aucun fichier");
        }
    }

    private FileMetadata storeFile(File file) {
        try {
            Path uploadDir = Path.of("uploads", "questions");
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
}
