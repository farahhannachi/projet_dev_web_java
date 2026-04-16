package org.example.controller;

import javafx.animation.*;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.paint.CycleMethod;
import javafx.scene.paint.LinearGradient;
import javafx.scene.paint.Stop;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.util.Duration;
import javafx.concurrent.Task;
import org.example.model.Avatar;
import org.example.model.User;
import org.example.service.UserService;

import java.io.File;
import java.io.IOException;
import java.util.regex.Pattern;

public class ProfilController {

    // Hero Section Fields
    @FXML private StackPane heroSection;
    @FXML private ImageView heroBackgroundImage;
    @FXML private Button editBackgroundBtn;
    @FXML private ImageView heroAvatarImage;
    @FXML private Button editAvatarOverlayBtn;
    @FXML private Label heroNameLabel;
    @FXML private Label heroEmailLabel;
    @FXML private Label heroRoleLabel;
    @FXML private Label heroStatusBadge;
    
    @FXML private StackPane mainStack;
    @FXML private StackPane overlayPane;
    
    @FXML private VBox editModal;
    @FXML private VBox deleteModal;
    @FXML private VBox avatarPanel;
    @FXML private VBox successModal;

    @FXML private Circle avatarCircle;
    @FXML private ImageView avatarImageView;
    @FXML private Label avatarLabel;
    @FXML private Label previewLabel;
    @FXML private ImageView previewImageView;
    @FXML private Label nameLabel;

    @FXML private Label infoNameLabel;
    @FXML private Label infoEmailLabel;
    @FXML private Label infoRoleLabel;
    @FXML private Label infoStatusLabel;
    @FXML private Label infoDateLabel;

    @FXML private TextField editNameField;
    @FXML private TextField editEmailField;
    @FXML private PasswordField editPasswordField;
    @FXML private Label editErrorLabel;

    @FXML private Label successMessage;
    
    // Navbar elements
    @FXML private HBox profileContainer;
    @FXML private VBox profileDropdown;
    @FXML private Circle navbarAvatarCircle;
    @FXML private Label navbarAvatarLabel;
    @FXML private Label navbarUsername;
    
    // Style buttons
    @FXML private Button styleCartoon;
    @FXML private Button styleNeutral;
    @FXML private Button stylePixel;
    @FXML private Button styleAdventure;
    @FXML private Button styleEmoji;
    @FXML private Button styleLorelei;
    @FXML private Button styleRobot;

    private UserService userService = new UserService();
    private User currentUser;
    private Avatar currentAvatar;
    private Avatar previewAvatar;
    private String selectedStyle = "cartoon";
    
    private static final String EMAIL_PATTERN = "^[a-zA-Z][a-zA-Z0-9._-]*@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$";
    private static final Pattern emailPattern = Pattern.compile(EMAIL_PATTERN);

    @FXML
    public void initialize() {
        currentUser = userService.getCurrentUser();
        if (currentUser != null) {
            loadProfileData();
            loadUserAvatar();
            loadNavbarUserData();
            loadHeroSection();
        }
        
        // Fade in animation for hero section
        if (heroSection != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(600), heroSection);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        }
        
        // Pulse animation for avatar
        if (avatarImageView != null) {
            ScaleTransition pulse = new ScaleTransition(Duration.seconds(1.5), avatarImageView);
            pulse.setFromX(1);
            pulse.setToX(1.05);
            pulse.setFromY(1);
            pulse.setToY(1.05);
            pulse.setAutoReverse(true);
            pulse.setCycleCount(Animation.INDEFINITE);
            pulse.play();
        }
    }
    
    /**
     * Load the hero section with user data and default/custom background
     */
    private void loadHeroSection() {
        if (currentUser == null) return;
        
       // Set hero labels - CALVINO STYLE
        if (heroNameLabel != null) {
            // Format: "My name is [Name]"
            String firstName = currentUser.getNom().split(" ")[0];
            heroNameLabel.setText("My name is " + firstName + ".");
        }
        if (heroEmailLabel != null) {
            heroEmailLabel.setText(currentUser.getEmail());
        }
        if (heroRoleLabel != null) {
            // Role based on user type
            String roleText = currentUser.getType().equalsIgnoreCase("admin") 
                ? "Pharmacy Administrator" 
                : "Customer";
            heroRoleLabel.setText(roleText);
        }
        if (heroStatusBadge != null) {
            if (currentUser.isBlocked()) {
                heroStatusBadge.setText("BLOQUÉ");
                heroStatusBadge.getStyleClass().add("blocked");
            } else {
                heroStatusBadge.setText("ACTIF");
                heroStatusBadge.getStyleClass().add("active");
            }
        }
        
        // Load hero avatar
        loadHeroAvatar();
        
        // Load background (image)
        loadHeroBackground();
    }
    
    /**
     * Load avatar in hero section
     */
    private void loadHeroAvatar() {
        if (heroAvatarImage == null) return;
        
        String avatarConfig = currentUser.getAvatarConfig();
        Avatar heroAvatar;
        if (avatarConfig != null && !avatarConfig.isEmpty()) {
            heroAvatar = Avatar.fromJson(avatarConfig);
        } else {
            heroAvatar = Avatar.generateRandom();
        }
        
        try {
            Image image = new Image(heroAvatar.getAvatarUrl(), true);
            heroAvatarImage.setImage(image);
            heroAvatarImage.setVisible(true);
        } catch (Exception e) {
            System.out.println("Failed to load hero avatar: " + e.getMessage());
        }
    }
    
    /**
     * Load background (image or video)
     */
    private void loadHeroBackground() {
        // Try loading default background image
        try {
            String defaultBgUrl = "https://images.unsplash.com/photo-1576091160550-2173dba999f3?w=1920&h=400&fit=crop";
            Image bgImage = new Image(defaultBgUrl, true);
            bgImage.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                if (newProgress.doubleValue() == 1.0 && bgImage.isError() == false) {
                    heroBackgroundImage.setImage(bgImage);
                    heroBackgroundImage.setVisible(true);
                }
            });
            
            // Fallback: if image fails or takes too long, use gradient
            Task<Void> fallbackTask = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    Thread.sleep(3000);
                    return null;
                }
            };
            fallbackTask.setOnSucceeded(e -> {
                if (heroBackgroundImage.getImage() == null) {
                    applyGradientBackground();
                }
            });
            fallbackTask.run();
            
        } catch (Exception e) {
            System.out.println("Failed to load default background: " + e.getMessage());
            applyGradientBackground();
        }
    }
    
    /**
     * Apply gradient background as fallback
     */
    private void applyGradientBackground() {
        // Create a simple gradient using Rectangle
        Rectangle gradient = new Rectangle();
        gradient.setWidth(1920);
        gradient.setHeight(350);
        
        // Create green gradient
        LinearGradient linearGradient = new LinearGradient(
            0, 0, 0, 1, 
            true, 
            CycleMethod.NO_CYCLE,
            new Stop(0, Color.web("#1F6F54")),
            new Stop(0.5, Color.web("#2E8B57")),
            new Stop(1, Color.web("#3CB371"))
        );
        gradient.setFill(linearGradient);
        
        // Add to hero section as background
        if (heroSection != null) {
            heroSection.setStyle("-fx-background-color: #1F6F54;");
        }
    }
    
    /**
     * Handle background change (image upload only)
     */
    @FXML
    private void handleChangeBackground() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image de fond");
        fileChooser.getExtensionFilters().add(
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );
        
        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            String filePath = selectedFile.toURI().toString();
            setImageBackground(filePath);
        }
    }
    
    /**
     * Set image as background
     */
    private void setImageBackground(String imagePath) {
        try {
            Image image = new Image(imagePath, true);
            image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                if (newProgress.doubleValue() == 1.0) {
                    heroBackgroundImage.setImage(image);
                }
            });
        } catch (Exception e) {
            System.out.println("Failed to load background image: " + e.getMessage());
        }
    }
    
    private void loadNavbarUserData() {
        if (currentUser != null) {
            // Set username in navbar
            if (navbarUsername != null) {
                String username = currentUser.getNom();
                if (username != null && !username.isEmpty()) {
                    // Get first name only for cleaner look
                    String firstName = username.split(" ")[0];
                    navbarUsername.setText(firstName);
                } else {
                    navbarUsername.setText("Utilisateur");
                }
            }
            
            // Set avatar circle color
            if (navbarAvatarCircle != null) {
                navbarAvatarCircle.setStyle("-fx-fill: #1f6f54; -fx-stroke: white; -fx-stroke-width: 2;");
            }
        }
    }

    private void loadProfileData() {
        if (nameLabel != null) nameLabel.setText(currentUser.getNom());

        if (infoNameLabel != null) infoNameLabel.setText(currentUser.getNom());
        if (infoEmailLabel != null) infoEmailLabel.setText(currentUser.getEmail());
        if (infoRoleLabel != null) infoRoleLabel.setText(currentUser.getType().toUpperCase());

        boolean isBlocked = currentUser.isBlocked();
        if (infoStatusLabel != null) {
            if (isBlocked) {
                infoStatusLabel.setText("BLOQUÉ");
                infoStatusLabel.setStyle("-fx-text-fill: #dc2626;");
            } else {
                infoStatusLabel.setText("ACTIF");
                infoStatusLabel.setStyle("-fx-text-fill: #059669;");
            }
        }

        if (infoDateLabel != null) infoDateLabel.setText(currentUser.getCreatedAt());
    }
    
    private void loadUserAvatar() {
        String avatarConfig = currentUser.getAvatarConfig();
        if (avatarConfig != null && !avatarConfig.isEmpty()) {
            currentAvatar = Avatar.fromJson(avatarConfig);
        } else {
            currentAvatar = Avatar.generateRandom();
        }
        
        // Display the avatar
        updateAvatarDisplay(avatarLabel, avatarCircle, currentAvatar);
    }
    
    private void updateAvatarDisplay(Label label, Circle circle, Avatar avatar) {
        if (avatar == null) {
            avatar = Avatar.generateRandom();
        }
        
        // For DiceBear avatars, we use the URL to load image
        String avatarUrl = avatar.getAvatarUrl();
        
        // Try to load the image
        try {
            Image image = new Image(avatarUrl, true);
            image.progressProperty().addListener((obs, oldProgress, newProgress) -> {
                if (newProgress.doubleValue() == 1.0) {
                    // Image loaded successfully
                }
            });
            
            // Set the image to the ImageView and ensure it's visible
            if (avatarImageView != null) {
                avatarImageView.setImage(image);
                avatarImageView.setVisible(true);
                avatarImageView.setManaged(true);
            }
            if (previewImageView != null) {
                previewImageView.setImage(image);
                previewImageView.setVisible(true);
                previewImageView.setManaged(true);
            }
            
            // Hide the emoji label, show ImageView
            if (label != null) {
                label.setVisible(false);
                label.setManaged(false);
            }
            if (circle != null) {
                circle.setVisible(false);
                circle.setManaged(false);
            }
        } catch (Exception e) {
            // Fallback to emoji display
            System.out.println("Failed to load avatar image: " + e.getMessage());
            if (label != null) {
                label.setVisible(true);
                label.setManaged(true);
                label.setText(avatar.getSeed() != null ? "👤" : "👤");
            }
            // Ensure ImageView is hidden
            if (avatarImageView != null) {
                avatarImageView.setVisible(false);
                avatarImageView.setManaged(false);
            }
        }
    }
    
    private void updatePreviewAvatar() {
        if (previewAvatar == null) {
            previewAvatar = Avatar.generateRandom(selectedStyle);
        }
        
        // Apply animation for preview update
        if (previewImageView != null) {
            // Fade out
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), previewImageView);
            fadeOut.setFromValue(1.0);
            fadeOut.setToValue(0.0);
            fadeOut.setOnFinished(e -> {
                updateAvatarDisplay(null, null, previewAvatar);
                // Fade in
                FadeTransition fadeIn = new FadeTransition(Duration.millis(150), previewImageView);
                fadeIn.setFromValue(0.0);
                fadeIn.setToValue(1.0);
                fadeIn.play();
            });
            fadeOut.play();
        }
    }

    // Style selection handlers
    @FXML
    private void selectStyle(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        String style = clicked.getText().toLowerCase();
        
        // Map display name to style
        selectedStyle = mapStyleName(style);
        
        // Update button styles
        styleCartoon.getStyleClass().remove("selected");
        styleNeutral.getStyleClass().remove("selected");
        stylePixel.getStyleClass().remove("selected");
        styleAdventure.getStyleClass().remove("selected");
        styleEmoji.getStyleClass().remove("selected");
        styleLorelei.getStyleClass().remove("selected");
        styleRobot.getStyleClass().remove("selected");
        
        clicked.getStyleClass().add("selected");
        
        // Generate new avatar with selected style
        previewAvatar = Avatar.generateRandom(selectedStyle);
        updatePreviewAvatar();
    }
    
    private String mapStyleName(String name) {
        switch(name.toLowerCase()) {
            case "cartoon": return "avataaars";
            case "neutral": return "avataaars-neutral";
            case "pixel": return "pixel-art";
            case "adventure": return "adventurer";
            case "emoji": return "fun-emoji";
            case "lorelei": return "lorelei";
            case "robot": return "bottts";
            default: return "avataaars";
        }
    }
    
    // Generate random avatar
    @FXML
    private void generateRandomAvatar() {
        previewAvatar = Avatar.generateRandom(selectedStyle);
        updatePreviewAvatar();
    }

    @FXML
    private void handleEditProfile() {
        editErrorLabel.setText("");
        editNameField.setText(currentUser.getNom());
        editEmailField.setText(currentUser.getEmail());
        editPasswordField.clear();
        showModal(editModal);
    }

    @FXML
    private void handleSaveProfile() {
        String name = editNameField.getText().trim();
        String email = editEmailField.getText().trim();
        String password = editPasswordField.getText();

        if (name.isEmpty() || email.isEmpty()) {
            editErrorLabel.setText("❌ Nom et email obligatoires");
            return;
        }

        if (!emailPattern.matcher(email).matches()) {
            editErrorLabel.setText("❌ Format d'email invalide");
            return;
        }

        if (!password.isEmpty() && password.length() < 6) {
            editErrorLabel.setText("❌ Le mot de passe doit contenir au moins 6 caractères");
            return;
        }

        String passwordToUse = password.isEmpty() ? currentUser.getPassword() : password;

        if (userService.updateUser(currentUser.getId(), name, email, passwordToUse, currentUser.getType())) {
            currentUser.setNom(name);
            currentUser.setEmail(email);
            loadProfileData();
            showSuccess("✅ Profil mis à jour avec succès!");
            closeModal();
        } else {
            editErrorLabel.setText("❌ Email déjà utilisé par un autre compte");
        }
    }

    @FXML
    private void handleChangeAvatar() {
        // Open avatar generator panel (DiceBear only)
        showAvatarGenerator();
    }
    
    /**
     * Show avatar generator panel (DiceBear)
     */
    private void showAvatarGenerator() {
        // Initialize preview with current avatar or generate new
        if (previewAvatar == null) {
            previewAvatar = currentAvatar != null ? currentAvatar : Avatar.generateRandom();
        }
        
        // Update preview display
        updatePreviewAvatar();
        
        // Select current style
        selectedStyle = previewAvatar.getStyle();
        
        // Update button styles
        if (styleCartoon != null) styleCartoon.getStyleClass().remove("selected");
        if (styleNeutral != null) styleNeutral.getStyleClass().remove("selected");
        if (stylePixel != null) stylePixel.getStyleClass().remove("selected");
        if (styleAdventure != null) styleAdventure.getStyleClass().remove("selected");
        if (styleEmoji != null) styleEmoji.getStyleClass().remove("selected");
        if (styleLorelei != null) styleLorelei.getStyleClass().remove("selected");
        if (styleRobot != null) styleRobot.getStyleClass().remove("selected");
        
        switch(selectedStyle) {
            case "avataaars": if (styleCartoon != null) styleCartoon.getStyleClass().add("selected"); break;
            case "avataaars-neutral": if (styleNeutral != null) styleNeutral.getStyleClass().add("selected"); break;
            case "pixel-art": if (stylePixel != null) stylePixel.getStyleClass().add("selected"); break;
            case "adventurer": if (styleAdventure != null) styleAdventure.getStyleClass().add("selected"); break;
            case "fun-emoji": if (styleEmoji != null) styleEmoji.getStyleClass().add("selected"); break;
            case "lorelei": if (styleLorelei != null) styleLorelei.getStyleClass().add("selected"); break;
            case "bottts": if (styleRobot != null) styleRobot.getStyleClass().add("selected"); break;
            default: if (styleCartoon != null) styleCartoon.getStyleClass().add("selected");
        }
        
        showAvatarPanel();
    }
    


    @FXML
    private void handleAcceptAvatar() {
        if (previewAvatar != null) {
            currentAvatar = previewAvatar;
            
            // Update the main avatar display with animation
            if (avatarImageView != null) {
                FadeTransition fadeOut = new FadeTransition(Duration.millis(150), avatarImageView);
                fadeOut.setFromValue(1.0);
                fadeOut.setToValue(0.0);
                fadeOut.setOnFinished(e -> {
                    updateAvatarDisplay(null, null, currentAvatar);
                    FadeTransition fadeIn = new FadeTransition(Duration.millis(150), avatarImageView);
                    fadeIn.setFromValue(0.0);
                    fadeIn.setToValue(1.0);
                    fadeIn.play();
                });
                fadeOut.play();
            }
            
            // Save to database
            String avatarJson = currentAvatar.toJson();
            if (userService.updateUserAvatar(currentUser.getId(), avatarJson)) {
                currentUser.setAvatarConfig(avatarJson);
            }
        }
        
        showSuccess("✅ Avatar enregistré avec succès!");
        closeAvatarPanel();
    }

    @FXML
    private void closeAvatarPanel() {
        // Scale down animation for panel
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), avatarPanel);
        scaleDown.setFromX(1);
        scaleDown.setToX(0.9);
        scaleDown.setFromY(1);
        scaleDown.setToY(0.9);
        scaleDown.setInterpolator(Interpolator.EASE_IN);
        
        FadeTransition fade = new FadeTransition(Duration.millis(300), overlayPane);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            overlayPane.setVisible(false);
            overlayPane.setManaged(false);
            avatarPanel.setVisible(false);
            avatarPanel.setManaged(false);
            avatarPanel.setScaleX(1);
            avatarPanel.setScaleY(1);
        });
        
        // Run scale first, then fade
        scaleDown.setOnFinished(e -> fade.play());
        scaleDown.play();
    }

    @FXML
    private void handleDeleteAccount() {
        showModal(deleteModal);
    }

    @FXML
    private void handleConfirmDelete() {
        if (userService.deleteUser(currentUser.getId())) {
            showSuccess("✅ Compte supprimé avec succès!");
            closeModal();
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(e -> {
                try {
                    goBack();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            });
            pause.play();
        } else {
            showSuccess("❌ Erreur lors de la suppression");
        }
    }

    private void showSuccess(String message) {
        successMessage.setText(message.replace("✅ ", "").replace("❌ ", ""));
        showModal(successModal);
    }

    private void showModal(VBox modal) {
        overlayPane.setVisible(true);
        overlayPane.setManaged(true);
        
        avatarPanel.setVisible(false);
        avatarPanel.setManaged(false);
        
        modal.setVisible(true);
        modal.setManaged(true);

        FadeTransition fade = new FadeTransition(Duration.millis(300), overlayPane);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
        
        modal.setScaleX(0.8);
        modal.setScaleY(0.8);
        ScaleTransition scale = new ScaleTransition(Duration.millis(300), modal);
        scale.setFromX(0.8);
        scale.setFromY(0.8);
        scale.setToX(1);
        scale.setToY(1);
        scale.play();
    }

    private void showAvatarPanel() {
        overlayPane.setVisible(true);
        overlayPane.setManaged(true);
        
        editModal.setVisible(false);
        editModal.setManaged(false);
        deleteModal.setVisible(false);
        deleteModal.setManaged(false);
        successModal.setVisible(false);
        successModal.setManaged(false);
        
        avatarPanel.setVisible(true);
        avatarPanel.setManaged(true);
        
        // Start small and scale up (bounce effect)
        avatarPanel.setScaleX(0.8);
        avatarPanel.setScaleY(0.8);

        // Scale up animation
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(400), avatarPanel);
        scaleUp.setFromX(0.8);
        scaleUp.setToX(1);
        scaleUp.setFromY(0.8);
        scaleUp.setToY(1);
        scaleUp.setInterpolator(Interpolator.EASE_OUT);
        
        // Fade in
        FadeTransition fade = new FadeTransition(Duration.millis(300), overlayPane);
        fade.setFromValue(0);
        fade.setToValue(1);
        
        // Play both animations
        scaleUp.play();
        fade.play();
        
        avatarPanel.setTranslateY(-50);
        avatarPanel.setOpacity(0);
        TranslateTransition slide = new TranslateTransition(Duration.millis(400), avatarPanel);
        slide.setFromY(-50);
        slide.setToY(0);
        slide.play();
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(400), avatarPanel);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }

    @FXML
    private void closeModal() {
        FadeTransition fade = new FadeTransition(Duration.millis(300), overlayPane);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            overlayPane.setVisible(false);
            overlayPane.setManaged(false);
            editModal.setVisible(false);
            editModal.setManaged(false);
            deleteModal.setVisible(false);
            deleteModal.setManaged(false);
            avatarPanel.setVisible(false);
            avatarPanel.setManaged(false);
            successModal.setVisible(false);
            successModal.setManaged(false);
        });
        fade.play();
    }

    @FXML
    private void goBack() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Accueil.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) mainStack.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }
    
    @FXML
    private void goToAccueil() throws IOException {
        goBack();
    }
    
    @FXML
    private void goToDashboard() throws IOException {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Dashboard.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) profileContainer.getScene().getWindow();
        stage.setScene(scene);
        stage.setFullScreen(true);
    }
    
    @FXML
    private void toggleProfileDropdown() {
        if (profileDropdown != null) {
            boolean isVisible = profileDropdown.isVisible();
            
            if (isVisible) {
                // Close with fade + scale out
                FadeTransition fade = new FadeTransition(Duration.millis(200), profileDropdown);
                fade.setFromValue(1.0);
                fade.setToValue(0.0);
                
                ScaleTransition scale = new ScaleTransition(Duration.millis(200), profileDropdown);
                scale.setFromX(1.0);
                scale.setFromY(1.0);
                scale.setToX(0.95);
                scale.setToY(0.95);
                
                scale.setOnFinished(e -> {
                    profileDropdown.setVisible(false);
                    profileDropdown.setManaged(false);
                });
                
                fade.play();
                scale.play();
            } else {
                // Open with fade + scale in
                profileDropdown.setVisible(true);
                profileDropdown.setManaged(true);
                profileDropdown.setOpacity(0);
                profileDropdown.setScaleX(0.95);
                profileDropdown.setScaleY(0.95);
                
                FadeTransition fade = new FadeTransition(Duration.millis(200), profileDropdown);
                fade.setFromValue(0.0);
                fade.setToValue(1.0);
                
                ScaleTransition scale = new ScaleTransition(Duration.millis(200), profileDropdown);
                scale.setFromX(0.95);
                scale.setFromY(0.95);
                scale.setToX(1.0);
                scale.setToY(1.0);
                
                fade.play();
                scale.play();
            }
        }
    }
    
    @FXML
    private void closeDropdownIfOpen() {
        if (profileDropdown != null && profileDropdown.isVisible()) {
            FadeTransition fade = new FadeTransition(Duration.millis(200), profileDropdown);
            fade.setFromValue(1.0);
            fade.setToValue(0.0);
            fade.setOnFinished(e -> {
                profileDropdown.setVisible(false);
                profileDropdown.setManaged(false);
            });
            fade.play();
        }
    }
    
    @FXML
    private void logout() throws IOException {
        userService.logout();
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/Login.fxml"));
        Parent root = loader.load();
        Scene scene = new Scene(root);
        scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
        Stage stage = (Stage) profileContainer.getScene().getWindow();
        stage.setScene(scene);
    }
}
