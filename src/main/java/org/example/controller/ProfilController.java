package org.example.controller;

import javafx.animation.FadeTransition;
import javafx.animation.ScaleTransition;
import javafx.animation.Interpolator;
import javafx.animation.PauseTransition;
import javafx.animation.TranslateTransition;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.Node;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import org.example.model.Avatar;
import org.example.model.User;
import org.example.config.AIConfig;
import org.example.service.OpenRouterService;
import org.example.service.TwoFactorAuthService;
import org.example.service.UserService;
import org.example.util.NavbarOrdonnanceMenu;
import org.example.util.SceneNavigation;
import org.example.service.ResponseQuestionService;

import java.io.File;
import java.util.regex.Pattern;
import javafx.concurrent.Task;

public class ProfilController {

    @FXML private StackPane mainStack;
    @FXML private StackPane heroSection;
    @FXML private ImageView heroBackgroundImage;
    @FXML private ImageView heroAvatarImage;
    @FXML private StackPane heroAvatarFrame;
    @FXML private Label heroNameLabel;
    @FXML private Label heroEmailLabel;
    @FXML private Label heroRoleLabel;
    @FXML private Label heroStatusBadge;

    @FXML private StackPane overlayPane;
    @FXML private VBox editModal;
    @FXML private VBox deleteModal;
    @FXML private VBox avatarPanel;
    @FXML private VBox successModal;
    @FXML private StackPane previewAvatarFrame;
    @FXML private ImageView previewImageView;

    @FXML private Label infoNameLabel;
    @FXML private Label infoEmailLabel;
    @FXML private Label infoRoleLabel;
    @FXML private Label emailInfoCardLabel;
    @FXML private Label roleInfoCardLabel;
    @FXML private Label infoStatusLabel;
    @FXML private Label twoFactorStatusLabel;
    @FXML private Label twoFactorDescriptionLabel;
    @FXML private TextField twoFactorSecretField;
    @FXML private TextField twoFactorCodeField;
    @FXML private Label twoFactorMessageLabel;
    @FXML private VBox twoFactorSetupBox;
    @FXML private Button prepareTwoFactorButton;
    @FXML private Button disableTwoFactorButton;
    @FXML private VBox assistantPanel;
    @FXML private Button assistantFloatingButton;
    @FXML private ScrollPane assistantScrollPane;
    @FXML private VBox assistantMessagesBox;
    @FXML private TextField assistantInputField;
    @FXML private Button assistantSendButton;
    @FXML private Label assistantHintLabel;
    @FXML private Label assistantTitleLabel;
    @FXML private Label messagesBadge;

    @FXML private TextField editNameField;
    @FXML private TextField editEmailField;
    @FXML private PasswordField editPasswordField;
    @FXML private Label editErrorLabel;
    @FXML private Label successMessage;

    @FXML private HBox profileContainer;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;
    @FXML private Circle navbarAvatarCircle;
    @FXML private Label navbarUsername;
    @FXML private Button styleCartoon;
    @FXML private Button styleNeutral;
    @FXML private Button stylePixel;
    @FXML private Button styleAdventure;
    @FXML private Button styleEmoji;
    @FXML private Button styleLorelei;
    @FXML private Button styleRobot;

    private final UserService userService = UserService.getInstance();
    private final TwoFactorAuthService twoFactorAuthService = new TwoFactorAuthService();
    private final OpenRouterService aiAssistantService = new OpenRouterService();
    private final ResponseQuestionService responseQuestionService = new ResponseQuestionService();
    private User currentUser;
    private Avatar currentAvatar;
    private Avatar previewAvatar;
    private String selectedStyle = "avataaars";
    private String pendingTwoFactorSecret;
    private boolean assistantRequestRunning;
    private long nextAssistantRequestAllowedAt;

    private static final long ASSISTANT_COOLDOWN_MS = 1500;

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[a-zA-Z][a-zA-Z0-9._-]*@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$");

    @FXML
    public void initialize() {
        currentUser = userService.getCurrentUser();
        configureAvatarViews();

        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }

        if (currentUser != null) {
            loadProfileData();
            loadNavbarUserData();
            loadHeroSection();
            loadUserAvatar();
            refreshTwoFactorSection();
            initializeAssistant();
        }

        if (heroSection != null) {
            FadeTransition fade = new FadeTransition(Duration.millis(500), heroSection);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        }

        NavbarOrdonnanceMenu.wirePopupStyle(mainStack);
        updateMessagesBadge();
    }

    private void updateMessagesBadge() {
        if (messagesBadge == null) return;
        User u = userService.getCurrentUser();
        if (u == null) { messagesBadge.setVisible(false); messagesBadge.setManaged(false); return; }
        int count = responseQuestionService.countUnreadResponsesForClient(u.getId());
        messagesBadge.setText(String.valueOf(count));
        messagesBadge.setVisible(count > 0);
        messagesBadge.setManaged(count > 0);
    }

    private void configureAvatarViews() {
        applyAvatarClip(heroAvatarImage, 74);
        applyAvatarClip(previewImageView, 72);

        DropShadow shadow = new DropShadow();
        shadow.setColor(Color.rgb(10, 54, 39, 0.22));
        shadow.setRadius(24);
        shadow.setSpread(0.16);
        shadow.setOffsetY(10);

        if (heroAvatarFrame != null) {
            heroAvatarFrame.setEffect(shadow);
        }
        if (previewAvatarFrame != null) {
            previewAvatarFrame.setEffect(shadow);
        }
    }

    private void applyAvatarClip(ImageView imageView, double radius) {
        if (imageView == null) {
            return;
        }

        Circle clip = new Circle(radius, radius, radius);
        imageView.setClip(clip);
        imageView.setPreserveRatio(true);
        imageView.setSmooth(true);
    }

    private void loadHeroSection() {
        if (currentUser == null) {
            return;
        }

        String fullName = safeText(currentUser.getNom(), "Utilisateur CuraVita");
        String firstName = fullName.split(" ")[0];
        heroNameLabel.setText("My name is " + firstName + ".");
        heroEmailLabel.setText(safeText(currentUser.getEmail(), "email@curavita.com"));
        String role = currentUser.getType();
        heroRoleLabel.setText(role != null && role.equalsIgnoreCase("admin") ? "Pharmacy Administrator" : "Customer");

        heroStatusBadge.getStyleClass().removeAll("active", "blocked");
        if (currentUser.isBlocked()) {
            heroStatusBadge.setText("BLOQUE");
            heroStatusBadge.getStyleClass().add("blocked");
        } else {
            heroStatusBadge.setText("ACTIF");
            heroStatusBadge.getStyleClass().add("active");
        }

        loadHeroBackground();
    }

    private void loadHeroBackground() {
        try {
            if (heroBackgroundImage == null) {
                return;
            }
            Image bgImage = new Image(
                    "https://images.unsplash.com/photo-1576091160550-2173dba999f3?w=1600&h=420&fit=crop",
                    1600,
                    420,
                    false,
                    true,
                    true
            );
            heroBackgroundImage.setImage(bgImage);
        } catch (Exception exception) {
            if (heroSection != null) {
                heroSection.setStyle("-fx-background-color: linear-gradient(to right, #1f6f54, #2f8f67);");
            }
        }
    }

    private void loadNavbarUserData() {
        if (currentUser == null) {
            return;
        }

        if (navbarUsername != null) {
            String username = safeText(currentUser.getNom(), "Utilisateur");
            navbarUsername.setText(username.split(" ")[0]);
        }

        if (navbarAvatarCircle != null) {
            navbarAvatarCircle.setStyle("-fx-fill: #1f6f54; -fx-stroke: white; -fx-stroke-width: 2;");
        }
    }

    private void loadProfileData() {
        String name = safeText(currentUser.getNom(), "Utilisateur");
        String email = safeText(currentUser.getEmail(), "email@curavita.com");
        String role = safeText(currentUser.getType(), "client").toUpperCase();

        if (infoNameLabel != null) infoNameLabel.setText(name);
        if (infoEmailLabel != null) infoEmailLabel.setText(email);
        if (emailInfoCardLabel != null) emailInfoCardLabel.setText(email);
        if (infoRoleLabel != null) infoRoleLabel.setText(role);
        if (roleInfoCardLabel != null) roleInfoCardLabel.setText(role);

        if (infoStatusLabel != null) {
            if (currentUser.isBlocked()) {
                infoStatusLabel.setText("BLOQUE");
                infoStatusLabel.setStyle("-fx-text-fill: #dc2626;");
            } else {
                infoStatusLabel.setText("ACTIF");
                infoStatusLabel.setStyle("-fx-text-fill: #059669;");
            }
        }
    }

    private void refreshTwoFactorSection() {
        if (currentUser == null) {
            return;
        }

        boolean enabled = currentUser.isTotpEnabled();
        twoFactorStatusLabel.setText(enabled ? "Active" : "Inactive");
        twoFactorStatusLabel.getStyleClass().removeAll("on", "off");
        twoFactorStatusLabel.getStyleClass().add(enabled ? "on" : "off");

        twoFactorDescriptionLabel.setText(enabled
                ? "2FA is enabled. After sign-in, this user must confirm access with a 6-digit authenticator code."
                : "Enable 2FA with an authenticator app to secure sign-in with a second verification step.");

        disableTwoFactorButton.setVisible(enabled);
        disableTwoFactorButton.setManaged(enabled);
        prepareTwoFactorButton.setText(enabled ? "Regenerer la cle" : "Configurer la 2FA");

        if (!enabled && (pendingTwoFactorSecret == null || pendingTwoFactorSecret.isBlank())) {
            twoFactorSetupBox.setVisible(false);
            twoFactorSetupBox.setManaged(false);
        }
        if (enabled) {
            twoFactorSetupBox.setVisible(false);
            twoFactorSetupBox.setManaged(false);
            twoFactorSecretField.clear();
            twoFactorCodeField.clear();
        }
    }

    private void initializeAssistant() {
        if (assistantTitleLabel != null) {
            assistantTitleLabel.setText(aiAssistantService.getAssistantName());
        }
        if (assistantHintLabel != null) {
            assistantHintLabel.setText(buildAssistantReadyMessage());
        }
        setAssistantInteractionEnabled(true);
        if (assistantMessagesBox != null && assistantMessagesBox.getChildren().isEmpty()) {
            addAssistantMessage(aiAssistantService.getAssistantName(),
                    "Hi. I can help you navigate CuraVita, understand products, manage your profile, or explain admin actions.",
                    false);
        }
    }

    private void loadUserAvatar() {
        String avatarConfig = currentUser.getAvatarConfig();
        if (avatarConfig != null && !avatarConfig.isEmpty()) {
            currentAvatar = Avatar.fromJson(avatarConfig);
        } else {
            currentAvatar = Avatar.generateRandom("avataaars");
        }

        updateAvatarDisplay(currentAvatar, true, false);
    }

    private void updateAvatarDisplay(Avatar avatar, boolean updateHero, boolean updatePreview) {
        Avatar safeAvatar = avatar != null ? avatar : Avatar.generateRandom("avataaars");
        Image avatarImage = createAvatarImage(safeAvatar.getAvatarUrl());
        if (avatarImage == null) {
            avatarImage = createAvatarImage(getDefaultAvatarUrl());
        }

        if (updateHero && heroAvatarImage != null) {
            heroAvatarImage.setImage(avatarImage);
        }
        if (updatePreview && previewImageView != null) {
            previewImageView.setImage(avatarImage);
        }
    }

    private Image createAvatarImage(String avatarUrl) {
        try {
            /* Résolution élevée pour éviter la pixélisation sur le hero (~148px) et HiDPI */
            Image image = new Image(avatarUrl, 384, 384, true, true, true);
            return image.isError() ? null : image;
        } catch (Exception exception) {
            return null;
        }
    }

    private String getDefaultAvatarUrl() {
        String seed = currentUser != null && currentUser.getNom() != null && !currentUser.getNom().isBlank()
                ? currentUser.getNom().replace(" ", "+")
                : "CuraVita";
        return "https://api.dicebear.com/7.x/initials/png?seed=" + seed + "&backgroundColor=1f6f54&textColor=ffffff&size=256";
    }

    private String safeText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    @FXML
    private void toggleAssistantPanel() {
        boolean shouldShow = !assistantPanel.isVisible();
        assistantPanel.setVisible(shouldShow);
        assistantPanel.setManaged(shouldShow);

        if (shouldShow) {
            assistantPanel.setOpacity(0);
            assistantPanel.setTranslateY(18);

            FadeTransition fade = new FadeTransition(Duration.millis(220), assistantPanel);
            fade.setFromValue(0);
            fade.setToValue(1);

            TranslateTransition slide = new TranslateTransition(Duration.millis(220), assistantPanel);
            slide.setFromY(18);
            slide.setToY(0);

            fade.play();
            slide.play();
            if (assistantInputField != null) {
                assistantInputField.requestFocus();
            }
        }
    }

    @FXML
    private void handleAssistantSend() {
        long now = System.currentTimeMillis();
        if (assistantRequestRunning) {
            assistantHintLabel.setText("Please wait. The assistant is still replying.");
            return;
        }

        if (now < nextAssistantRequestAllowedAt) {
            long remainingMs = nextAssistantRequestAllowedAt - now;
            double seconds = Math.max(1.0, Math.ceil(remainingMs / 1000.0));
            assistantHintLabel.setText("Please wait " + (int) seconds + " second(s) before sending another message.");
            return;
        }

        String userMessage = assistantInputField.getText().trim();
        if (userMessage.isEmpty()) {
            return;
        }

        assistantRequestRunning = true;
        setAssistantInteractionEnabled(false);
        addAssistantMessage("You", userMessage, true);
        assistantInputField.clear();
        assistantHintLabel.setText("Thinking...");

        Task<String> task = new Task<>() {
            @Override
            protected String call() throws Exception {
                return aiAssistantService.ask(userMessage, currentUser);
            }
        };

        task.setOnSucceeded(event -> {
            addAssistantMessage(aiAssistantService.getAssistantName(), task.getValue(), false);
            assistantHintLabel.setText(buildAssistantReadyMessage());
            beginAssistantCooldown();
        });

        task.setOnFailed(event -> {
            addAssistantMessage(aiAssistantService.getAssistantName(),
                    OpenRouterService.TEMPORARY_UNAVAILABLE_MESSAGE,
                    false);
            assistantHintLabel.setText("The assistant is available, but the remote AI request failed.");
            beginAssistantCooldown();
        });

        Thread worker = new Thread(task, "assistant-request");
        worker.setDaemon(true);
        worker.start();
    }

    private void setAssistantInteractionEnabled(boolean enabled) {
        if (assistantInputField != null) {
            assistantInputField.setDisable(!enabled);
        }
        if (assistantSendButton != null) {
            assistantSendButton.setDisable(!enabled);
        }
    }

    private void beginAssistantCooldown() {
        assistantRequestRunning = false;
        nextAssistantRequestAllowedAt = System.currentTimeMillis() + ASSISTANT_COOLDOWN_MS;

        PauseTransition cooldown = new PauseTransition(Duration.millis(ASSISTANT_COOLDOWN_MS));
        cooldown.setOnFinished(event -> {
            setAssistantInteractionEnabled(true);
            assistantHintLabel.setText(buildAssistantReadyMessage());
            if (assistantInputField != null && assistantPanel != null && assistantPanel.isVisible()) {
                assistantInputField.requestFocus();
            }
        });
        cooldown.play();
    }

    private String buildAssistantReadyMessage() {
        return AIConfig.isConfigured()
                ? "Connected to " + AIConfig.PROVIDER + ". Ask anything about CuraVita."
                : "Set ASSISTANT_NAME, PROVIDER, MODEL, and API_KEY in AIConfig.java to enable live AI replies.";
    }

    private void addAssistantMessage(String sender, String message, boolean userMessage) {
        VBox bubble = new VBox(4);
        bubble.getStyleClass().add(userMessage ? "assistant-bubble-user" : "assistant-bubble-ai");

        Label senderLabel = new Label(sender);
        senderLabel.getStyleClass().add("assistant-message-author");

        Label bodyLabel = new Label(message);
        bodyLabel.setWrapText(true);
        bodyLabel.getStyleClass().add("assistant-message-text");

        bubble.getChildren().addAll(senderLabel, bodyLabel);

        HBox row = new HBox();
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        if (userMessage) {
            row.getChildren().addAll(spacer, bubble);
        } else {
            row.getChildren().addAll(bubble, spacer);
        }

        assistantMessagesBox.getChildren().add(row);
        if (assistantScrollPane != null) {
            assistantScrollPane.layout();
            assistantScrollPane.setVvalue(1.0);
        }
    }

    @FXML
    private void handlePrepareTwoFactor() {
        pendingTwoFactorSecret = twoFactorAuthService.generateSecret();
        twoFactorSecretField.setText(twoFactorAuthService.buildManualEntryKey(pendingTwoFactorSecret));
        twoFactorCodeField.clear();
        twoFactorMessageLabel.setText("Use this secret in your authenticator app, then enter the generated code.");
        twoFactorSetupBox.setVisible(true);
        twoFactorSetupBox.setManaged(true);
    }

    @FXML
    private void handleEnableTwoFactor() {
        if (pendingTwoFactorSecret == null || pendingTwoFactorSecret.isBlank()) {
            twoFactorMessageLabel.setText("Generate a secret first.");
            return;
        }

        String code = twoFactorCodeField.getText().trim();
        if (!code.matches("\\d{6}")) {
            twoFactorMessageLabel.setText("Enter a valid 6-digit code.");
            return;
        }

        if (!twoFactorAuthService.verifyCode(pendingTwoFactorSecret, code)) {
            twoFactorMessageLabel.setText("Invalid code. Check your authenticator app and try again.");
            return;
        }

        boolean updated = userService.updateUserTwoFactor(currentUser.getId(), pendingTwoFactorSecret, true);
        if (!updated) {
            twoFactorMessageLabel.setText("Unable to enable 2FA right now.");
            return;
        }

        currentUser.setTotpSecret(pendingTwoFactorSecret);
        currentUser.setTotpEnabled(true);
        pendingTwoFactorSecret = null;
        twoFactorMessageLabel.setText("2FA enabled successfully.");
        refreshTwoFactorSection();
    }

    @FXML
    private void handleDisableTwoFactor() {
        boolean updated = userService.disableUserTwoFactor(currentUser.getId());
        if (!updated) {
            twoFactorMessageLabel.setText("Unable to disable 2FA right now.");
            return;
        }

        currentUser.setTotpSecret(null);
        currentUser.setTotpEnabled(false);
        pendingTwoFactorSecret = null;
        twoFactorMessageLabel.setText("2FA disabled.");
        refreshTwoFactorSection();
    }

    @FXML
    private void selectStyle(ActionEvent event) {
        Button clicked = (Button) event.getSource();
        selectedStyle = mapStyleName(clicked.getText());
        clearSelectedStyles();
        clicked.getStyleClass().add("selected");
        previewAvatar = Avatar.generateRandom(selectedStyle);
        updatePreviewAvatar();
    }

    private void clearSelectedStyles() {
        if (styleCartoon != null) styleCartoon.getStyleClass().remove("selected");
        if (styleNeutral != null) styleNeutral.getStyleClass().remove("selected");
        if (stylePixel != null) stylePixel.getStyleClass().remove("selected");
        if (styleAdventure != null) styleAdventure.getStyleClass().remove("selected");
        if (styleEmoji != null) styleEmoji.getStyleClass().remove("selected");
        if (styleLorelei != null) styleLorelei.getStyleClass().remove("selected");
        if (styleRobot != null) styleRobot.getStyleClass().remove("selected");
    }

    private String mapStyleName(String name) {
        switch (name.toLowerCase()) {
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

    @FXML
    private void generateRandomAvatar() {
        previewAvatar = Avatar.generateRandom(selectedStyle);
        updatePreviewAvatar();
    }

    private void updatePreviewAvatar() {
        if (previewAvatar == null) {
            previewAvatar = Avatar.generateRandom(selectedStyle);
        }

        FadeTransition fadeOut = new FadeTransition(Duration.millis(140), previewImageView);
        fadeOut.setFromValue(1);
        fadeOut.setToValue(0);
        fadeOut.setOnFinished(event -> {
            updateAvatarDisplay(previewAvatar, false, true);
            FadeTransition fadeIn = new FadeTransition(Duration.millis(140), previewImageView);
            fadeIn.setFromValue(0);
            fadeIn.setToValue(1);
            fadeIn.play();
        });
        fadeOut.play();
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
            editErrorLabel.setText("Nom et email obligatoires");
            return;
        }

        if (!EMAIL_PATTERN.matcher(email).matches()) {
            editErrorLabel.setText("Format d'email invalide");
            return;
        }

        if (!password.isEmpty() && password.length() < 6) {
            editErrorLabel.setText("Le mot de passe doit contenir au moins 6 caracteres");
            return;
        }

        String passwordToUse = password.isEmpty() ? currentUser.getPassword() : password;
        if (userService.updateUser(currentUser.getId(), name, email, passwordToUse, currentUser.getType())) {
            currentUser.setNom(name);
            currentUser.setEmail(email);
            loadProfileData();
            loadNavbarUserData();
            loadHeroSection();
            showSuccess("Profil mis a jour avec succes!");
            closeModal();
        } else {
            editErrorLabel.setText("Email deja utilise par un autre compte");
        }
    }

    @FXML
    private void handleChangeAvatar() {
        showAvatarGenerator();
    }

    @FXML
    private void handleChangeBackground() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Choisir une image de fond");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.webp")
        );

        File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            heroBackgroundImage.setImage(new Image(selectedFile.toURI().toString(), 1600, 420, false, true, true));
        }
    }

    private void showAvatarGenerator() {
        previewAvatar = currentAvatar != null ? currentAvatar : Avatar.generateRandom(selectedStyle);
        selectedStyle = previewAvatar.getStyle();
        clearSelectedStyles();
        markSelectedStyle(selectedStyle);
        updateAvatarDisplay(previewAvatar, false, true);
        showAvatarPanel();
    }

    private void markSelectedStyle(String style) {
        if ("avataaars".equals(style) && styleCartoon != null) styleCartoon.getStyleClass().add("selected");
        if ("avataaars-neutral".equals(style) && styleNeutral != null) styleNeutral.getStyleClass().add("selected");
        if ("pixel-art".equals(style) && stylePixel != null) stylePixel.getStyleClass().add("selected");
        if ("adventurer".equals(style) && styleAdventure != null) styleAdventure.getStyleClass().add("selected");
        if ("fun-emoji".equals(style) && styleEmoji != null) styleEmoji.getStyleClass().add("selected");
        if ("lorelei".equals(style) && styleLorelei != null) styleLorelei.getStyleClass().add("selected");
        if ("bottts".equals(style) && styleRobot != null) styleRobot.getStyleClass().add("selected");
    }

    @FXML
    private void handleAcceptAvatar() {
        if (previewAvatar != null) {
            currentAvatar = previewAvatar;
            FadeTransition fadeOut = new FadeTransition(Duration.millis(150), heroAvatarImage);
            fadeOut.setFromValue(1);
            fadeOut.setToValue(0);
            fadeOut.setOnFinished(event -> {
                updateAvatarDisplay(currentAvatar, true, false);
                FadeTransition fadeIn = new FadeTransition(Duration.millis(150), heroAvatarImage);
                fadeIn.setFromValue(0);
                fadeIn.setToValue(1);
                fadeIn.play();
            });
            fadeOut.play();

            String avatarJson = currentAvatar.toJson();
            if (userService.updateUserAvatar(currentUser.getId(), avatarJson)) {
                currentUser.setAvatarConfig(avatarJson);
            }
        }

        showSuccess("Avatar enregistre avec succes!");
        closeAvatarPanel();
    }

    @FXML
    private void closeAvatarPanel() {
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), avatarPanel);
        scaleDown.setFromX(1);
        scaleDown.setToX(0.92);
        scaleDown.setFromY(1);
        scaleDown.setToY(0.92);
        scaleDown.setInterpolator(Interpolator.EASE_IN);

        FadeTransition fade = new FadeTransition(Duration.millis(240), overlayPane);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(event -> {
            overlayPane.setVisible(false);
            overlayPane.setManaged(false);
            avatarPanel.setVisible(false);
            avatarPanel.setManaged(false);
            avatarPanel.setScaleX(1);
            avatarPanel.setScaleY(1);
        });

        scaleDown.setOnFinished(event -> fade.play());
        scaleDown.play();
    }

    @FXML
    private void handleDeleteAccount() {
        showModal(deleteModal);
    }

    @FXML
    private void handleConfirmDelete() {
        if (userService.deleteUser(currentUser.getId())) {
            showSuccess("Compte supprime avec succes!");
            closeModal();
            PauseTransition pause = new PauseTransition(Duration.seconds(2));
            pause.setOnFinished(event -> goBack());
            pause.play();
        } else {
            showSuccess("Erreur lors de la suppression");
        }
    }

    private void showSuccess(String message) {
        successMessage.setText(message);
        showModal(successModal);
    }

    private void showModal(VBox modal) {
        overlayPane.setVisible(true);
        overlayPane.setManaged(true);

        avatarPanel.setVisible(false);
        avatarPanel.setManaged(false);
        editModal.setVisible(false);
        editModal.setManaged(false);
        deleteModal.setVisible(false);
        deleteModal.setManaged(false);
        successModal.setVisible(false);
        successModal.setManaged(false);

        modal.setVisible(true);
        modal.setManaged(true);

        FadeTransition fade = new FadeTransition(Duration.millis(250), overlayPane);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
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
        avatarPanel.setScaleX(0.88);
        avatarPanel.setScaleY(0.88);
        avatarPanel.setTranslateY(-24);
        avatarPanel.setOpacity(0);

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(320), avatarPanel);
        scaleUp.setFromX(0.88);
        scaleUp.setToX(1);
        scaleUp.setFromY(0.88);
        scaleUp.setToY(1);
        scaleUp.setInterpolator(Interpolator.EASE_OUT);

        TranslateTransition slide = new TranslateTransition(Duration.millis(320), avatarPanel);
        slide.setFromY(-24);
        slide.setToY(0);

        FadeTransition panelFade = new FadeTransition(Duration.millis(320), avatarPanel);
        panelFade.setFromValue(0);
        panelFade.setToValue(1);

        FadeTransition overlayFade = new FadeTransition(Duration.millis(240), overlayPane);
        overlayFade.setFromValue(0);
        overlayFade.setToValue(1);

        scaleUp.play();
        slide.play();
        panelFade.play();
        overlayFade.play();
    }

    @FXML
    private void closeModal() {
        FadeTransition fade = new FadeTransition(Duration.millis(240), overlayPane);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(event -> {
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

    private Node navAnchor() {
        return profileContainer != null ? profileContainer : mainStack;
    }

    private void navigateToFxml(String resourcePath) {
        SceneNavigation.replaceScene(navAnchor(), resourcePath);
    }

    @FXML
    private void handleNavbarSearch() {
        if (assistantPanel == null || assistantInputField == null) {
            return;
        }
        if (!assistantPanel.isVisible()) {
            assistantPanel.setVisible(true);
            assistantPanel.setManaged(true);
            assistantPanel.setOpacity(1);
            assistantPanel.setTranslateY(0);
        }
        javafx.application.Platform.runLater(() -> assistantInputField.requestFocus());
    }

    private void dismissProfileDropdownNow() {
        if (profileDropdown != null) {
            profileDropdown.setOpacity(1);
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }
    }

    @FXML
    private void goToMessagesPage() {
        dismissProfileDropdownNow();
        navigateToFxml("/fxml/MessagesPage.fxml");
    }

    @FXML
    private void handleNavProduits() {
        navigateToFxml("/fxml/FrontProduits.fxml");
    }

    @FXML
    private void handleNavCommandes() {
        navigateToFxml("/fxml/FrontMesCommandes.fxml");
    }

    @FXML
    private void handleNavServices() {
        navigateToFxml("/fxml/FrontServices.fxml");
    }

    @FXML
    private void handleNavPanier() {
        navigateToFxml("/fxml/FrontCommande.fxml");
    }

    @FXML
    private void handleNavAdresses() {
        navigateToFxml("/fxml/FrontMesAdresses.fxml");
    }

    @FXML
    private void handleNavGuide() {
        navigateToFxml("/fxml/GuideSante.fxml");
    }

    @FXML
    private void handleNavContact() {
        navigateToFxml("/fxml/ContactPage.fxml");
    }

    @FXML
    private void handleNavAbout() {
        navigateToFxml("/fxml/APropos.fxml");
    }

    @FXML
    private void goBack() {
        navigateToFxml("/fxml/Accueil.fxml");
    }

    @FXML
    private void goToAccueil() {
        try {
            goBack();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void goToDashboard() {
        dismissProfileDropdownNow();
        navigateToFxml("/fxml/Dashboard.fxml");
    }

    @FXML
    private void toggleProfileDropdown() {
        if (profileDropdown == null) {
            return;
        }

        boolean isVisible = profileDropdown.isVisible();
        if (isVisible) {
            FadeTransition fade = new FadeTransition(Duration.millis(180), profileDropdown);
            fade.setFromValue(1);
            fade.setToValue(0);
            fade.setOnFinished(event -> {
                profileDropdown.setVisible(false);
                profileDropdown.setManaged(false);
            });
            fade.play();
        } else {
            profileDropdown.setVisible(true);
            profileDropdown.setManaged(true);
            profileDropdown.setOpacity(0);
            profileDropdown.toFront();
            Node parent = profileDropdown.getParent();
            if (parent != null) {
                parent.toFront();
            }

            FadeTransition fade = new FadeTransition(Duration.millis(180), profileDropdown);
            fade.setFromValue(0);
            fade.setToValue(1);
            fade.play();
        }
    }

    @FXML
    private void closeDropdownIfOpen() {
        if (profileDropdown != null && profileDropdown.isVisible()) {
            FadeTransition fade = new FadeTransition(Duration.millis(160), profileDropdown);
            fade.setFromValue(1);
            fade.setToValue(0);
            fade.setOnFinished(event -> {
                profileDropdown.setVisible(false);
                profileDropdown.setManaged(false);
            });
            fade.play();
        }
    }

    @FXML
    private void logout() {
        dismissProfileDropdownNow();
        userService.logout();
        SceneNavigation.replaceScene(navAnchor(), "/fxml/Login.fxml");
    }

    @FXML
    private void goToTraitement() {
        navigateToFxml("/fxml/Traitement.fxml");
    }

    @FXML
    private void goToCreerOrdonnance() {
        navigateToFxml("/fxml/Ordonnance.fxml");
    }

    @FXML
    private void goToMesOrdonnances() {
        navigateToFxml("/fxml/MesOrdonnances.fxml");
    }
}
