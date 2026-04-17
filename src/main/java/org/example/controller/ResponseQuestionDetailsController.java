package org.example.controller;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Stage;
import org.example.model.ResponseQuestion;

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;

public class ResponseQuestionDetailsController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private Label idLabel;
    @FXML private Label questionLabel;
    @FXML private Label utilisateurLabel;
    @FXML private Label auteurTypeLabel;
    @FXML private Label reponseRoleLabel;
    @FXML private Label actionTypeLabel;
    @FXML private Label impactStatutLabel;
    @FXML private Label createdAtLabel;
    @FXML private Label luParClientLabel;
    @FXML private Label fileLabel;
    @FXML private TextArea reponseTextArea;

    private ResponseQuestion response;

    public void setResponse(ResponseQuestion response) {
        this.response = response;
        idLabel.setText(String.valueOf(response.getId()));
        questionLabel.setText(safe(response.getQuestionObjet()));
        utilisateurLabel.setText(safe(response.getUtilisateurNom()));
        auteurTypeLabel.setText(response.getAuteurType() != null ? response.getAuteurType().getLabel() : "");
        reponseRoleLabel.setText(response.getReponseRole() != null ? response.getReponseRole().getLabel() : "");
        actionTypeLabel.setText(response.getActionType() != null ? response.getActionType().getLabel() : "");
        impactStatutLabel.setText(response.getImpactStatut() != null ? response.getImpactStatut().getLabel() : "");
        createdAtLabel.setText(response.getCreatedAt() != null ? response.getCreatedAt().format(DATE_FORMAT) : "");
        luParClientLabel.setText(response.isLuParClient() ? "Oui" : "Non");
        fileLabel.setText(response.getFileName() != null ? response.getFileName() : "Aucun fichier");
        reponseTextArea.setText(safe(response.getReponseText()));
    }

    @FXML
    private void handleOpenFile() {
        if (response == null || response.getFilePath() == null || response.getFilePath().isEmpty()) {
            return;
        }
        try {
            Path filePath = Path.of(response.getFilePath());
            File file = filePath.toFile();
            if (file.exists()) {
                Desktop.getDesktop().open(file);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) idLabel.getScene().getWindow();
        stage.close();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}

