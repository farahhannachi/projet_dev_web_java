package org.example.controller;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.model.ResponseQuestion;
import org.example.model.User;
import org.example.service.ResponseQuestionService;
import org.example.service.UserService;

import java.io.IOException;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class MessagesPageController {
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private Button profileButton;
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
        int count = responseService.countResponsesForClient(currentUser.getId());
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
    }

    @FXML
    private void toggleProfileDropdown() {
        boolean isVisible = profileDropdown.isVisible();
        profileDropdown.setVisible(!isVisible);
        profileDropdown.setManaged(!isVisible);
    }

    @FXML
    private void showProfile() {
        profileDropdown.setVisible(false);
        profileDropdown.setManaged(false);
    }

    @FXML
    private void goToDashboard() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) profileButton.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void logout() throws IOException {
        userService.logout();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) profileButton.getScene().getWindow();
        stage.setScene(scene);
    }

    @FXML
    private void goHome() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Accueil.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) profileButton.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }

    @FXML
    private void goContact() {
        try {
            URL fxmlUrl = getClass().getResource("/fxml/ContactPage.fxml");
            if (fxmlUrl == null) {
                throw new IllegalStateException("FXML not found on classpath: /fxml/ContactPage.fxml");
            }
            System.out.println("Loading ContactPage.fxml from: " + fxmlUrl);

            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) profileButton.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (Exception e) {
            e.printStackTrace();
            Throwable c = e;
            while ((c = c.getCause()) != null) {
                System.err.println("Caused by: " + c);
            }
        }
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

