package org.example.controller;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.scene.shape.Circle;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.model.User;
import org.example.service.UserService;

import java.io.IOException;

public class LoginController {
    @FXML private TextField loginEmail;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginError;

    @FXML private TextField signupName;
    @FXML private TextField signupEmail;
    @FXML private PasswordField signupPassword;
    @FXML private Label signupError;
    
    // Validation Labels
    @FXML private Label signupNameError;
    @FXML private Label signupEmailError;
    @FXML private Label signupPasswordError;
    
    // Password Strength Indicators
    @FXML private Circle strengthCircle1;
    @FXML private Circle strengthCircle2;
    @FXML private Circle strengthCircle3;
    @FXML private Label passwordStrengthLabel;
    
    // Password Requirements
    @FXML private Label reqLength;
    @FXML private Label reqUppercase;
    @FXML private Label reqNumber;
    @FXML private Label reqSpecial;

    @FXML private StackPane signInPanel;
    @FXML private StackPane signUpPanel;
    @FXML private StackPane backgroundPane;
    @FXML private StackPane bannedOverlayPane;
    @FXML private VBox bannedModal;

    private UserService userService = new UserService();
    private boolean isSignInMode = true;
    private boolean isAnimating = false;
    
    // Password strength tracking
    private boolean hasMinLength = false;
    private boolean hasUppercase = false;
    private boolean hasNumber = false;
    private boolean hasSpecial = false;

    @FXML
    private void initialize() {
        // Check database connection on startup
        if (!UserService.isDatabaseConnected()) {
            loginError.setText("⚠️ Database not connected. Please start MySQL.");
            loginError.setStyle("-fx-text-fill: #F39C12;");
        }
        
        // Initialize: Sign In is visible, Sign Up is to the right (off-screen)
        // Sign In panel is at translateX = 0
        // Sign Up panel is at translateX = 428 (to the right)
        if (signUpPanel != null) {
            signUpPanel.setTranslateX(428);
        }
        if (signInPanel != null) {
            signInPanel.setTranslateX(0);
            signInPanel.setOpacity(1);
        }
        
        // Start background animation
        startBackgroundAnimation();
    }
    
    private void startBackgroundAnimation() {
        if (backgroundPane == null) return;
        
        // Create two colored rectangles for the gradient effect
        javafx.scene.layout.StackPane whiteLayer = new StackPane();
        whiteLayer.setStyle("-fx-background-color: #ffffff;");
        whiteLayer.setOpacity(1.0);
        
        javafx.scene.layout.StackPane greenLayer = new StackPane();
        greenLayer.setStyle("-fx-background-color: #1f6f5c;");
        greenLayer.setOpacity(0.0);
        
        backgroundPane.getChildren().addAll(whiteLayer, greenLayer);
        backgroundPane.setPickOnBounds(false);
        
        // Animate green layer opacity: 0 -> 0.7 -> 0 -> loop
        Timeline timeline = new Timeline();
        timeline.setCycleCount(Timeline.INDEFINITE);
        
        // Phase 1: Stay white (0-3s)
        // Phase 2: Fade to green (3-6s)
        KeyValue kv1 = new KeyValue(greenLayer.opacityProperty(), 0.0, Interpolator.EASE_IN);
        KeyFrame kf1 = new KeyFrame(Duration.ZERO, kv1);
        
        KeyValue kv2 = new KeyValue(greenLayer.opacityProperty(), 0.7, Interpolator.EASE_BOTH);
        KeyFrame kf2 = new KeyFrame(Duration.seconds(5), kv2);
        
        KeyValue kv3 = new KeyValue(greenLayer.opacityProperty(), 0.0, Interpolator.EASE_BOTH);
        KeyFrame kf3 = new KeyFrame(Duration.seconds(10), kv3);
        
        timeline.getKeyFrames().addAll(kf1, kf2, kf3);
        timeline.play();
        
        // Add subtle floating animation to the floating circles
        startFloatingCirclesAnimation();
    }
    
    private void startFloatingCirclesAnimation() {
        // Get all floating circle elements
        for (javafx.scene.Node node : backgroundPane.getChildren()) {
            if (node instanceof StackPane) {
                String style = node.getStyle();
                if (style != null && style.contains("floating-circle")) {
                    // Create subtle floating/bobbing animation
                    TranslateTransition floatAnim = new TranslateTransition(Duration.seconds(4), node);
                    floatAnim.setFromY(node.getTranslateY());
                    floatAnim.setToY(node.getTranslateY() + 15);
                    floatAnim.setInterpolator(Interpolator.EASE_BOTH);
                    floatAnim.setAutoReverse(true);
                    floatAnim.setCycleCount(Animation.INDEFINITE);
                    floatAnim.play();
                    
                    // Subtle scale breathing
                    ScaleTransition scaleAnim = new ScaleTransition(Duration.seconds(3), node);
                    scaleAnim.setFromX(1.0);
                    scaleAnim.setToX(1.1);
                    scaleAnim.setInterpolator(Interpolator.EASE_BOTH);
                    scaleAnim.setAutoReverse(true);
                    scaleAnim.setCycleCount(Animation.INDEFINITE);
                    scaleAnim.play();
                }
            }
        }
    }

    @FXML
    private void handleLogin() {
        if (isAnimating) return;
        
        String email = loginEmail.getText().trim();
        String password = loginPassword.getText();

        if (email.isEmpty() || password.isEmpty()) {
            loginError.setText("❌ All fields required");
            loginError.setStyle("-fx-text-fill: #E74C3C;");
            return;
        }

        User user = userService.login(email, password);
        if (user != null) {
            // Check if user is blocked
            if (user.isBlocked()) {
                showBannedModal();
                return;
            }
            
            try {
                goToNextPage(user);
            } catch (IOException e) {
                loginError.setText("❌ Error loading page");
            }
        } else {
            loginError.setText("❌ Invalid email or password");
            loginError.setStyle("-fx-text-fill: #E74C3C;");
        }
    }

    private void showBannedModal() {
        bannedOverlayPane.setVisible(true);
        bannedOverlayPane.setManaged(true);
        bannedModal.setVisible(true);
        bannedModal.setManaged(true);

        FadeTransition fade = new FadeTransition(Duration.millis(300), bannedOverlayPane);
        fade.setFromValue(0);
        fade.setToValue(1);
        fade.play();
    }

    @FXML
    private void closeBannedModal() {
        FadeTransition fade = new FadeTransition(Duration.millis(300), bannedOverlayPane);
        fade.setFromValue(1);
        fade.setToValue(0);
        fade.setOnFinished(e -> {
            bannedOverlayPane.setVisible(false);
            bannedOverlayPane.setManaged(false);
            bannedModal.setVisible(false);
            bannedModal.setManaged(false);
        });
        fade.play();
    }

    @FXML
    private void handleSignup() {
        if (isAnimating) return;

        String name = signupName.getText().trim();
        String email = signupEmail.getText().trim();
        String password = signupPassword.getText();

        signupError.setText("");

        // VALIDATION 1: Name
        if (!isValidName(name)) {
            signupError.setText("❌ Le nom ne doit pas contenir de chiffres");
            signupError.setStyle("-fx-text-fill: #E74C3C;");
            return;
        }

        // VALIDATION 2: Email
        if (email.isEmpty()) {
            signupError.setText("❌ Email obligatoire");
            signupError.setStyle("-fx-text-fill: #E74C3C;");
            return;
        }

        if (!email.matches("^[a-zA-Z0-9]+@gmail\\.com$")) {
            signupError.setText("❌ Format email invalide (use: name@gmail.com)");
            signupError.setStyle("-fx-text-fill: #E74C3C;");
            return;
        }

        // VALIDATION 3: Password Strength
        if (!isPasswordValid(password)) {
            // Show specific error
            if (!hasMinLength) {
                signupError.setText("❌ Mot de passe: minimum 6 caractères");
            } else if (!hasUppercase) {
                signupError.setText("❌ Mot de passe: doit contenir une majuscule (A-Z)");
            } else if (!hasNumber) {
                signupError.setText("❌ Mot de passe: doit contenir un chiffre (0-9)");
            }
            signupError.setStyle("-fx-text-fill: #E74C3C;");
            return;
        }

        // All validations passed
        if (userService.signup(email, password, name)) {
            try {
                User newUser = userService.getCurrentUser();
                goToNextPage(newUser);
            } catch (IOException e) {
                signupError.setText("❌ Erreur chargement page");
                signupError.setStyle("-fx-text-fill: #E74C3C;");
            }
        } else {
            signupError.setText("❌ Email existe déjà ou format invalide");
            signupError.setStyle("-fx-text-fill: #E74C3C;");
        }
    }
    
    // ===== VALIDATION METHODS =====
    
    @FXML
    private void validateSignupName() {
        String name = signupName.getText().trim();
        
        if (name.isEmpty()) {
            signupNameError.setText("");
            return;
        }
        
        if (isValidName(name)) {
            signupNameError.setText("✅ Nom valide");
            signupNameError.setStyle("-fx-text-fill: #10b981;");
        } else {
            signupNameError.setText("❌ Le nom ne doit pas contenir de chiffres");
            signupNameError.setStyle("-fx-text-fill: #dc2626;");
        }
    }
    
    @FXML
    private void validateSignupEmail() {
        String email = signupEmail.getText().trim();
        
        if (email.isEmpty()) {
            signupEmailError.setText("");
            return;
        }
        
        if (email.matches("^[a-zA-Z0-9]+@gmail\\.com$")) {
            signupEmailError.setText("✅ Email valide");
            signupEmailError.setStyle("-fx-text-fill: #10b981;");
        } else {
            signupEmailError.setText("❌ Format: name@gmail.com");
            signupEmailError.setStyle("-fx-text-fill: #dc2626;");
        }
    }
    
    @FXML
    private void validatePasswordStrength() {
        String password = signupPassword.getText();
        
        if (password.isEmpty()) {
            resetPasswordStrength();
            return;
        }
        
        // Check each requirement
        hasMinLength = password.length() >= 6;
        hasUppercase = password.matches(".*[A-Z].*");
        hasNumber = password.matches(".*[0-9].*");
        hasSpecial = password.matches(".*[*\\-+@#].*");
        
        // Update requirement labels
        updateRequirementLabel(reqLength, hasMinLength, "✓ Au moins 6 caractères", "✗ Au moins 6 caractères");
        updateRequirementLabel(reqUppercase, hasUppercase, "✓ Contient une majuscule (A-Z)", "✗ Contient une majuscule (A-Z)");
        updateRequirementLabel(reqNumber, hasNumber, "✓ Contient un chiffre (0-9)", "✗ Contient un chiffre (0-9)");
        updateRequirementLabel(reqSpecial, hasSpecial, "✓ Contient caractère spécial (* - + @ #)", "○ Contient caractère spécial (* - + @ #)");
        
        // Calculate password strength
        int strength = (hasMinLength ? 1 : 0) + (hasUppercase ? 1 : 0) + (hasNumber ? 1 : 0) + (hasSpecial ? 1 : 0);
        
        updatePasswordStrengthIndicator(strength);
    }
    
    private void updatePasswordStrengthIndicator(int strength) {
        // Reset all circles
        strengthCircle1.setStyle("-fx-fill: #e5e7eb;");
        strengthCircle2.setStyle("-fx-fill: #e5e7eb;");
        strengthCircle3.setStyle("-fx-fill: #e5e7eb;");
        
        if (strength == 0) {
            passwordStrengthLabel.setText("");
        } else if (strength <= 2) {
            // WEAK - Red
            strengthCircle1.setStyle("-fx-fill: #dc2626;");
            passwordStrengthLabel.setText("Faible");
            passwordStrengthLabel.setStyle("-fx-text-fill: #dc2626; -fx-font-size: 11; -fx-font-weight: bold;");
        } else if (strength == 3) {
            // MEDIUM - Orange
            strengthCircle1.setStyle("-fx-fill: #f97316;");
            strengthCircle2.setStyle("-fx-fill: #f97316;");
            passwordStrengthLabel.setText("Moyen");
            passwordStrengthLabel.setStyle("-fx-text-fill: #f97316; -fx-font-size: 11; -fx-font-weight: bold;");
        } else {
            // STRONG - Green
            strengthCircle1.setStyle("-fx-fill: #10b981;");
            strengthCircle2.setStyle("-fx-fill: #10b981;");
            strengthCircle3.setStyle("-fx-fill: #10b981;");
            passwordStrengthLabel.setText("Fort");
            passwordStrengthLabel.setStyle("-fx-text-fill: #10b981; -fx-font-size: 11; -fx-font-weight: bold;");
        }
    }
    
    private void resetPasswordStrength() {
        strengthCircle1.setStyle("-fx-fill: #e5e7eb;");
        strengthCircle2.setStyle("-fx-fill: #e5e7eb;");
        strengthCircle3.setStyle("-fx-fill: #e5e7eb;");
        passwordStrengthLabel.setText("");
        
        reqLength.setText("✗ Au moins 6 caractères");
        reqLength.setStyle("-fx-text-fill: #999;");
        reqUppercase.setText("✗ Contient une majuscule (A-Z)");
        reqUppercase.setStyle("-fx-text-fill: #999;");
        reqNumber.setText("✗ Contient un chiffre (0-9)");
        reqNumber.setStyle("-fx-text-fill: #999;");
        reqSpecial.setText("○ Contient un caractère spécial (* - + @ #)");
        reqSpecial.setStyle("-fx-text-fill: #999;");
    }
    
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
        // Name must not contain numbers
        if (name.matches(".*\\d.*")) {
            return false;
        }
        return !name.isEmpty() && name.matches("^[a-zA-Z\\s]+$");
    }
    
    private boolean isPasswordValid(String password) {
        // Requires: Min 6 chars, 1 uppercase, 1 number
        // Optional: special character
        boolean hasMin = password.length() >= 6;
        boolean hasUpper = password.matches(".*[A-Z].*");
        boolean hasNum = password.matches(".*[0-9].*");
        
        return hasMin && hasUpper && hasNum;
    }

    @FXML
    private void showSignUp() {
        if (!isSignInMode || isAnimating) return;
        
        isAnimating = true;
        isSignInMode = false;
        
        // Slide Sign In panel to the LEFT (fade out)
        TranslateTransition signInSlideOut = new TranslateTransition(Duration.millis(500), signInPanel);
        signInSlideOut.setToX(-428);
        signInSlideOut.setInterpolator(Interpolator.EASE_BOTH);
        
        FadeTransition signInFadeOut = new FadeTransition(Duration.millis(300), signInPanel);
        signInFadeOut.setToValue(0);
        
        // Slide Sign Up panel from RIGHT to CENTER (fade in)
        TranslateTransition signUpSlideIn = new TranslateTransition(Duration.millis(500), signUpPanel);
        signUpSlideIn.setFromX(428);
        signUpSlideIn.setToX(0);
        signUpSlideIn.setInterpolator(Interpolator.EASE_BOTH);
        
        FadeTransition signUpFadeIn = new FadeTransition(Duration.millis(300), signUpPanel);
        signUpFadeIn.setFromValue(0);
        signUpFadeIn.setToValue(1);
        
        // Run animations in parallel
        ParallelTransition parallelOut = new ParallelTransition(signInSlideOut, signInFadeOut);
        ParallelTransition parallelIn = new ParallelTransition(signUpSlideIn, signUpFadeIn);
        
        SequentialTransition sequential = new SequentialTransition();
        sequential.getChildren().add(parallelOut);
        sequential.getChildren().add(parallelIn);
        
        sequential.setOnFinished(e -> {
            isAnimating = false;
        });
        
        sequential.play();
    }

    @FXML
    private void showSignIn() {
        if (isSignInMode || isAnimating) return;
        
        isAnimating = true;
        isSignInMode = true;
        
        // Slide Sign Up panel to the LEFT (fade out)
        TranslateTransition signUpSlideOut = new TranslateTransition(Duration.millis(500), signUpPanel);
        signUpSlideOut.setToX(-428);
        signUpSlideOut.setInterpolator(Interpolator.EASE_BOTH);
        
        FadeTransition signUpFadeOut = new FadeTransition(Duration.millis(300), signUpPanel);
        signUpFadeOut.setToValue(0);
        
        // Slide Sign In panel from LEFT to CENTER (fade in)
        TranslateTransition signInSlideIn = new TranslateTransition(Duration.millis(500), signInPanel);
        signInSlideIn.setFromX(-428);
        signInSlideIn.setToX(0);
        signInSlideIn.setInterpolator(Interpolator.EASE_BOTH);
        
        FadeTransition signInFadeIn = new FadeTransition(Duration.millis(300), signInPanel);
        signInFadeIn.setFromValue(0);
        signInFadeIn.setToValue(1);
        
        // Run animations in parallel
        ParallelTransition parallelOut = new ParallelTransition(signUpSlideOut, signUpFadeOut);
        ParallelTransition parallelIn = new ParallelTransition(signInSlideIn, signInFadeIn);
        
        SequentialTransition sequential = new SequentialTransition();
        sequential.getChildren().add(parallelOut);
        sequential.getChildren().add(parallelIn);
        
        sequential.setOnFinished(e -> {
            isAnimating = false;
        });
        
        sequential.play();
    }

    private void goToNextPage(User user) throws IOException {
        String fxmlFile;
        
        if (user.getType().equals("admin")) {
            fxmlFile = "/fxml/Dashboard.fxml";
        } else {
            fxmlFile = "/fxml/Accueil.fxml";
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlFile));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());

            Stage stage = (Stage) loginEmail.getScene().getWindow();
            stage.setScene(scene);
            stage.setFullScreen(true);
        } catch (Exception e) {
            e.printStackTrace();
            loginError.setText("❌ Error: " + e.getMessage());
            loginError.setStyle("-fx-text-fill: #E74C3C;");
        }
    }
}
