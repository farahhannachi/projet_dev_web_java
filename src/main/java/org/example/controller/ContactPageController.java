package org.example.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.example.model.Question;
import org.example.model.User;
import org.example.service.QuestionService;
import org.example.service.UserService;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class ContactPageController {
    @FXML private Button profileButton;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;
    @FXML private Label myTicketsCountLabel;
    @FXML private VBox myTicketsList;
    @FXML private VBox myTicketsEmptyState;

    private final UserService userService = new UserService();
    private final QuestionService questionService = new QuestionService();
    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML
    public void initialize() {
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }
        loadMyTickets();
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

        List<Question> latest = questionService.getQuestionsForUser(currentUser.getId(), 6);
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
        card.setAlignment(Pos.CENTER_LEFT);

        VBox left = new VBox(4);
        Label title = new Label(safe(q.getObjet()));
        title.getStyleClass().add("ticket-mini-title");

        String meta = formatDate(q) + " • " + safe(q.getTypeTicket());
        Label metaLabel = new Label(meta);
        metaLabel.getStyleClass().add("ticket-mini-meta");

        left.getChildren().addAll(title, metaLabel);

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
}

