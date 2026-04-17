package org.example.controller;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.example.model.User;
import org.example.service.QuestionService;
import org.example.service.UserService;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.UUID;

public class ContactQuestionController {
    @FXML private TextField objetField;
    @FXML private ComboBox<String> typeTicketCombo;
    @FXML private ComboBox<String> prioriteCombo;
    @FXML private TextArea descriptionArea;
    @FXML private Label fileInfoLabel;
    @FXML private Label formErrorLabel;
    @FXML private Label formSuccessLabel;

    private final QuestionService questionService = new QuestionService();
    private final UserService userService = new UserService();
    private File selectedFile;

    @FXML
    public void initialize() {
        typeTicketCombo.setItems(FXCollections.observableArrayList(
                "general", "commande", "livraison", "remboursement", "produit", "autre"));
        prioriteCombo.setItems(FXCollections.observableArrayList("basse", "normale", "haute"));
        typeTicketCombo.getSelectionModel().select("general");
        prioriteCombo.getSelectionModel().select("normale");
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
        formErrorLabel.setText("");
        formSuccessLabel.setText("");

        String objet = objetField.getText() != null ? objetField.getText().trim() : "";
        String description = descriptionArea.getText() != null ? descriptionArea.getText().trim() : "";
        String typeTicket = typeTicketCombo.getValue();
        String priorite = prioriteCombo.getValue();

        if (objet.isEmpty() || description.length() < 3) {
            formErrorLabel.setText("Objet et description (min 3 caracteres) requis.");
            return;
        }
        if (typeTicket == null || priorite == null) {
            formErrorLabel.setText("Type et priorite requis.");
            return;
        }

        FileMetadata metadata = null;
        if (selectedFile != null) {
            metadata = storeFile(selectedFile);
        }

        User currentUser = userService.getCurrentUser();
        Integer userId = currentUser != null ? currentUser.getId() : null;

        boolean created = questionService.createQuestion(
                typeTicket,
                objet,
                description,
                priorite,
                "ouvert",
                metadata != null ? metadata.fileName : null,
                metadata != null ? metadata.filePath : null,
                metadata != null ? metadata.fileType : null,
                metadata != null ? metadata.fileSize : null,
                LocalDateTime.now(),
                userId
        );

        if (created) {
            formSuccessLabel.setText("Question envoyee avec succes.");
            resetForm();
        } else {
            formErrorLabel.setText("Erreur lors de l'envoi.");
        }
    }

    @FXML
    private void handleClose() {
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

    private void resetForm() {
        objetField.clear();
        descriptionArea.clear();
        selectedFile = null;
        fileInfoLabel.setText("Aucun fichier");
        typeTicketCombo.getSelectionModel().select("general");
        prioriteCombo.getSelectionModel().select("normale");
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
