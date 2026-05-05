package org.example.controller;

import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.model.User;
import org.example.service.UserService;

import java.io.IOException;
import java.util.regex.Pattern;

public class UserManagementController {

    @FXML private StackPane mainStack;
    @FXML private StackPane overlayPane;
    @FXML private VBox addClientModal;
    @FXML private VBox editClientModal;
    @FXML private VBox deleteConfirmModal;
    @FXML private VBox successModal;

    @FXML private TextField searchField;
    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, String> nameColumn;
    @FXML private TableColumn<User, String> emailColumn;
    @FXML private TableColumn<User, String> roleColumn;
    @FXML private TableColumn<User, String> statusColumn;
    @FXML private TableColumn<User, String> dateColumn;
    @FXML private TableColumn<User, String> actionColumn;

    @FXML private Label totalClientsLabel;
    @FXML private Label activeClientsLabel;
    @FXML private Label blockedClientsLabel;

    @FXML private TextField addModalNameField;
    @FXML private TextField addModalEmailField;
    @FXML private PasswordField addModalPasswordField;
    @FXML private ComboBox<String> addModalRoleCombo;
    @FXML private Label addModalErrorLabel;
    @FXML private Label addModalNameError;
    @FXML private Label addModalEmailError;
    @FXML private Label addModalPasswordError;
    @FXML private Label addReqLength;
    @FXML private Label addReqUppercase;
    @FXML private Label addReqNumber;

    @FXML private TextField editModalNameField;
    @FXML private TextField editModalEmailField;
    @FXML private PasswordField editModalPasswordField;
    @FXML private ComboBox<String> editModalRoleCombo;
    @FXML private Label editModalErrorLabel;
    @FXML private Label editModalNameError;
    @FXML private Label editModalEmailError;
    @FXML private Label editModalPasswordError;
    @FXML private Label editReqLength;
    @FXML private Label editReqUppercase;
    @FXML private Label editReqNumber;

    @FXML private Label deleteConfirmMessage;
    @FXML private Label successMessage;

    private UserService userService = new UserService();
    private ObservableList<User> userList = FXCollections.observableArrayList();
    private ObservableList<User> originalUserList = FXCollections.observableArrayList();
    private FilteredList<User> filteredData;
    private User userToDelete;
    private User userToEdit;
    private boolean isSorted = false;

    private static final String EMAIL_PATTERN = "^[a-zA-Z][a-zA-Z0-9._-]*@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    private static final Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);

    @FXML
    public void initialize() {
        setupTable();
        setupSearch();
        setupRoleCombo();
        loadUsers();
        loadStatistics();
    }

    private void setupTable() {
        nameColumn.setCellValueFactory(new PropertyValueFactory<>("nom"));
        nameColumn.setCellFactory(column -> createColoredCell("#f0f9ff", "#1e40af"));
        
        emailColumn.setCellValueFactory(new PropertyValueFactory<>("email"));
        emailColumn.setCellFactory(column -> createColoredCell("#f5f3ff", "#4c1d95"));
        
        roleColumn.setCellValueFactory(new PropertyValueFactory<>("type"));
        roleColumn.setCellFactory(column -> createColoredCell("#f3f4f6", "#111827"));
        
        statusColumn.setCellValueFactory(cellData -> {
            User user = cellData.getValue();
            String status = (user.isBlocked()) ? "🔒 BLOQUÉ" : "✅ ACTIF";
            return javafx.beans.binding.Bindings.createStringBinding(() -> status);
        });
        statusColumn.setCellFactory(column -> createColoredCell("#f0fdf4", "#166534"));
        
        dateColumn.setCellValueFactory(new PropertyValueFactory<>("createdAt"));
        dateColumn.setCellFactory(column -> createColoredCell("#fdf2f8", "#831843"));

        actionColumn.setCellFactory(col -> new TableCell<User, String>() {
            private final Button editBtn = new Button("✏️ Edit");
            private final Button blockBtn = new Button("🔒 Block");
            private final Button deleteBtn = new Button("❌ Del");

            {
                editBtn.setStyle("-fx-padding: 5 10; -fx-font-size: 10; -fx-background-color: #007bff; -fx-text-fill: white; -fx-cursor: hand; -fx-border-radius: 4;");
                blockBtn.setStyle("-fx-padding: 5 10; -fx-font-size: 10; -fx-background-color: #ffc107; -fx-text-fill: black; -fx-cursor: hand; -fx-border-radius: 4;");
                deleteBtn.setStyle("-fx-padding: 5 10; -fx-font-size: 10; -fx-background-color: #dc3545; -fx-text-fill: white; -fx-cursor: hand; -fx-border-radius: 4;");

                editBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    showEditModal(user);
                });
                blockBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleBlockUserDirect(user);
                });
                deleteBtn.setOnAction(e -> {
                    User user = getTableView().getItems().get(getIndex());
                    showDeleteConfirm(user);
                });
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox hbox = new HBox(5, editBtn, blockBtn, deleteBtn);
                    hbox.setStyle("-fx-alignment: CENTER; -fx-background-color: #fefce8; -fx-padding: 5;");
                    setGraphic(hbox);
                }
            }
        });

        userTable.setItems(userList);
    }
    
    // Create colored table cells
    private <T> TableCell<User, T> createColoredCell(String bgColor, String textColor) {
        return new TableCell<User, T>() {
            @Override
            protected void updateItem(T item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item != null ? item.toString() : "");
                    setStyle(String.format("-fx-background-color: %s; -fx-text-fill: %s; -fx-padding: 12 10; -fx-font-size: 12;", bgColor, textColor));
                }
            }
        };
    }

    private void setupSearch() {
        filteredData = new FilteredList<>(userList, p -> true);
        searchField.textProperty().addListener((obs, oldVal, newVal) -> {
            filteredData.setPredicate(user -> {
                if (newVal == null || newVal.isEmpty()) return true;
                String lowerFilter = newVal.toLowerCase();
                return user.getNom().toLowerCase().contains(lowerFilter) ||
                       user.getEmail().toLowerCase().contains(lowerFilter) ||
                       user.getType().toLowerCase().contains(lowerFilter);
            });
        });
        SortedList<User> sortedData = new SortedList<>(filteredData);
        sortedData.comparatorProperty().bind(userTable.comparatorProperty());
        userTable.setItems(sortedData);
    }

    private void setupRoleCombo() {
        addModalRoleCombo.setItems(FXCollections.observableArrayList("client", "admin"));
        addModalRoleCombo.setValue("client");
        editModalRoleCombo.setItems(FXCollections.observableArrayList("client", "admin"));
    }

    private void loadUsers() {
        userList.clear();
        userList.addAll(userService.getAllUsers());
        // Maintain a copy of original order
        originalUserList.clear();
        originalUserList.addAll(userService.getAllUsers());
        isSorted = false;
    }

    private void loadStatistics() {
        int total = userList.size();
        long active = userList.stream().filter(u -> !u.isBlocked()).count();
        long blocked = userList.stream().filter(User::isBlocked).count();

        totalClientsLabel.setText(String.valueOf(total));
        activeClientsLabel.setText(String.valueOf(active));
        blockedClientsLabel.setText(String.valueOf(blocked));
    }

    @FXML
    private void handleAddClient() {
        addModalErrorLabel.setText("");
        addModalNameField.clear();
        addModalEmailField.clear();
        addModalPasswordField.clear();
        addModalRoleCombo.setValue("client");
        showModal(addClientModal);
    }

    @FXML
    private void handleAddClientConfirm() {
        String name = addModalNameField.getText().trim();
        String email = addModalEmailField.getText().trim();
        String password = addModalPasswordField.getText();
        String role = addModalRoleCombo.getValue();

        String errors = validateForm(name, email, password);
        if (!errors.isEmpty()) {
            addModalErrorLabel.setText(errors);
            return;
        }

        if (userService.addUser(name, email, password, role)) {
            loadUsers();
            loadStatistics();
            showSuccess("✅ Client ajouté!");
            closeModal();
        } else {
            addModalErrorLabel.setText("❌ Email déjà utilisé");
        }
    }

    private void showEditModal(User user) {
        userToEdit = user;
        editModalErrorLabel.setText("");
        editModalNameField.setText(user.getNom());
        editModalEmailField.setText(user.getEmail());
        editModalPasswordField.clear();
        editModalRoleCombo.setValue(user.getType());
        showModal(editClientModal);
    }

    @FXML
    private void handleEditClientConfirm() {
        if (userToEdit == null) return;

        String name = editModalNameField.getText().trim();
        String email = editModalEmailField.getText().trim();
        String password = editModalPasswordField.getText();
        String role = editModalRoleCombo.getValue();

        if (name.isEmpty() || email.isEmpty()) {
            editModalErrorLabel.setText("❌ Nom et email obligatoires");
            return;
        }

        if (!isValidEmail(email)) {
            editModalErrorLabel.setText("❌ Email invalide");
            return;
        }

        String passwordToUse = password.isEmpty() ? userToEdit.getPassword() : password;

        if (userService.updateUser(userToEdit.getId(), name, email, passwordToUse, role)) {
            loadUsers();
            loadStatistics();
            showSuccess("✅ Client modifié!");
            closeModal();
        } else {
            editModalErrorLabel.setText("❌ Erreur BD");
        }
    }

    private String validateForm(String name, String email, String password) {
        StringBuilder errors = new StringBuilder();

        if (name == null || name.isEmpty()) {
            errors.append("❌ Nom obligatoire\n");
        }
        if (email == null || email.isEmpty()) {
            errors.append("❌ Email obligatoire\n");
        } else if (!isValidEmail(email)) {
            errors.append("❌ Email invalide\n");
        }
        if (password == null || password.length() < 6) {
            errors.append("❌ Min 6 caractères\n");
        }

        return errors.toString();
    }

    private boolean isValidEmail(String email) {
        return emailPattern.matcher(email).matches();
    }

    // ===== ADD MODAL VALIDATION METHODS =====
    
    @FXML
    private void validateAddModalName() {
        String name = addModalNameField.getText().trim();
        
        if (name.isEmpty()) {
            addModalNameError.setText("");
            return;
        }
        
        if (isValidName(name)) {
            addModalNameError.setText("✓ Valide");
            addModalNameError.setStyle("-fx-text-fill: #10b981;");
        } else {
            addModalNameError.setText("✗ Pas de chiffres");
            addModalNameError.setStyle("-fx-text-fill: #dc2626;");
        }
    }
    
    @FXML
    private void validateAddModalEmail() {
        String email = addModalEmailField.getText().trim();
        
        if (email.isEmpty()) {
            addModalEmailError.setText("");
            return;
        }
        
        if (emailPattern.matcher(email).matches()) {
            addModalEmailError.setText("✓ Valide");
            addModalEmailError.setStyle("-fx-text-fill: #10b981;");
        } else {
            addModalEmailError.setText("✗ Format invalide");
            addModalEmailError.setStyle("-fx-text-fill: #dc2626;");
        }
    }
    
    @FXML
    private void validateAddModalPassword() {
        String password = addModalPasswordField.getText();
        
        if (password.isEmpty()) {
            addReqLength.setText("✗ Au moins 6 caractères");
            addReqUppercase.setText("✗ Majuscule (A-Z)");
            addReqNumber.setText("✗ Chiffre (0-9)");
            addReqLength.setStyle("-fx-text-fill: #dc2626;");
            addReqUppercase.setStyle("-fx-text-fill: #dc2626;");
            addReqNumber.setStyle("-fx-text-fill: #dc2626;");
            return;
        }
        
        boolean hasMin = password.length() >= 6;
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasNum = password.matches(".*[0-9].*");
        
        updateRequirementLabel(addReqLength, hasMin, "✓ Au moins 6 caractères", "✗ Au moins 6 caractères");
        updateRequirementLabel(addReqUppercase, hasUpper, "✓ Majuscule (A-Z)", "✗ Majuscule (A-Z)");
        updateRequirementLabel(addReqNumber, hasNum, "✓ Chiffre (0-9)", "✗ Chiffre (0-9)");
    }
    
    // ===== EDIT MODAL VALIDATION METHODS =====
    
    @FXML
    private void validateEditModalName() {
        String name = editModalNameField.getText().trim();
        
        if (name.isEmpty()) {
            editModalNameError.setText("");
            return;
        }
        
        if (isValidName(name)) {
            editModalNameError.setText("✓ Valide");
            editModalNameError.setStyle("-fx-text-fill: #10b981;");
        } else {
            editModalNameError.setText("✗ Pas de chiffres");
            editModalNameError.setStyle("-fx-text-fill: #dc2626;");
        }
    }
    
    @FXML
    private void validateEditModalEmail() {
        String email = editModalEmailField.getText().trim();
        
        if (email.isEmpty()) {
            editModalEmailError.setText("");
            return;
        }
        
        if (emailPattern.matcher(email).matches()) {
            editModalEmailError.setText("✓ Valide");
            editModalEmailError.setStyle("-fx-text-fill: #10b981;");
        } else {
            editModalEmailError.setText("✗ Format invalide");
            editModalEmailError.setStyle("-fx-text-fill: #dc2626;");
        }
    }
    
    @FXML
    private void validateEditModalPassword() {
        String password = editModalPasswordField.getText();
        
        if (password.isEmpty()) {
            editReqLength.setText("✗ Au moins 6 caractères");
            editReqUppercase.setText("✗ Majuscule (A-Z)");
            editReqNumber.setText("✗ Chiffre (0-9)");
            editReqLength.setStyle("-fx-text-fill: #dc2626;");
            editReqUppercase.setStyle("-fx-text-fill: #dc2626;");
            editReqNumber.setStyle("-fx-text-fill: #dc2626;");
            return;
        }
        
        boolean hasMin = password.length() >= 6;
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasNum = password.matches(".*[0-9].*");
        
        updateRequirementLabel(editReqLength, hasMin, "✓ Au moins 6 caractères", "✗ Au moins 6 caractères");
        updateRequirementLabel(editReqUppercase, hasUpper, "✓ Majuscule (A-Z)", "✗ Majuscule (A-Z)");
        updateRequirementLabel(editReqNumber, hasNum, "✓ Chiffre (0-9)", "✗ Chiffre (0-9)");
    }
    
    // ===== HELPER METHODS =====
    
    private void updateRequirementLabel(Label label, boolean met, String checkedText, String uncheckedText) {
        if (met) {
            label.setText(checkedText);
            label.setStyle("-fx-text-fill: #10b981; -fx-font-weight: bold;");
        } else {
            label.setText(uncheckedText);
            label.setStyle("-fx-text-fill: #dc2626;");
        }
    }
    
    private boolean isValidName(String name) {
        if (name.matches(".*\\d.*")) return false;
        return !name.isEmpty() && name.matches("^[a-zA-Z\\s]+$");
    }

    @FXML
    private void handleSort() {
        if (!isSorted) {
            // Sort alphabetically by name A-Z
            userList.sort((u1, u2) -> u1.getNom().compareToIgnoreCase(u2.getNom()));
            isSorted = true;
        } else {
            // Return to original order
            userList.clear();
            userList.addAll(originalUserList);
            isSorted = false;
        }
        userTable.refresh();
    }

    @FXML
    private void handleDownloadPDF() {
        showSuccess("📥 PDF en développement");
    }

    private void handleBlockUserDirect(User user) {
        user.setBlocked(!user.isBlocked());
        // Save blocked state to database
        userService.updateUserBlocked(user.getId(), user.isBlocked());
        
        // Refresh table immediately
        userTable.refresh();
        loadStatistics();
        
        String status = user.isBlocked() ? "🔒 bloqué" : "✅ débloqué";
        showSuccess("✅ Client " + status + "!");
    }

    private void showDeleteConfirm(User user) {
        deleteConfirmMessage.setText("Supprimer " + user.getNom() + " ?");
        userToDelete = user;
        showModal(deleteConfirmModal);
    }

    @FXML
    private void handleDeleteConfirm() {
        if (userToDelete == null) return;

        if (userService.deleteUser(userToDelete.getId())) {
            loadUsers();
            loadStatistics();
            showSuccess("✅ Client supprimé!");
        } else {
            showSuccess("❌ Erreur suppression");
        }
        closeModal();
    }

    private void showSuccess(String message) {
        successMessage.setText(message);
        showModal(successModal);
    }

    private void showModal(VBox modal) {
        overlayPane.setVisible(true);
        overlayPane.setManaged(true);
        modal.setVisible(true);
        modal.setManaged(true);

        FadeTransition fade = new FadeTransition(Duration.millis(300), overlayPane);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    @FXML
    private void closeModal() {
        FadeTransition fade = new FadeTransition(Duration.millis(300), overlayPane);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            overlayPane.setVisible(false);
            overlayPane.setManaged(false);
            addClientModal.setVisible(false);
            addClientModal.setManaged(false);
            editClientModal.setVisible(false);
            editClientModal.setManaged(false);
            deleteConfirmModal.setVisible(false);
            deleteConfirmModal.setManaged(false);
            successModal.setVisible(false);
            successModal.setManaged(false);
        });
        fade.play();
    }

    @FXML
    private void onStatCardEnter(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof VBox) {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), (VBox) e.getSource());
            scale.setToX(1.05);
            scale.setToY(1.05);
            scale.play();
        }
    }

    @FXML
    private void onStatCardExit(javafx.scene.input.MouseEvent e) {
        if (e.getSource() instanceof VBox) {
            ScaleTransition scale = new ScaleTransition(Duration.millis(200), (VBox) e.getSource());
            scale.setToX(1);
            scale.setToY(1);
            scale.play();
        }
    }

    @FXML
    private void goBackToDashboard() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) userTable.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }
}
