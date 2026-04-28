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

    @FXML private StackPane signInPanel;
    @FXML private StackPane signUpPanel;
    @FXML private StackPane backgroundPane;

    private UserService userService = new UserService();
    private boolean isSignInMode = true;
    private boolean isAnimating = false;

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

    @FXML
    private void handleSignup() {
        if (isAnimating) return;
        
        String name = signupName.getText().trim();
        String email = signupEmail.getText().trim();
        String password = signupPassword.getText();

        signupError.setText("");

        if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
            signupError.setText("❌ All fields required");
            signupError.setStyle("-fx-text-fill: #E74C3C;");
            return;
        }

        // Allow emails with text and numbers before @gmail.com (e.g., mundo36@gmail.com)
        if (!email.matches("^[a-zA-Z0-9]+@gmail\\.com$")) {
            signupError.setText("❌ Invalid email format (use: name@gmail.com or mundo36@gmail.com)");
            signupError.setStyle("-fx-text-fill: #E74C3C;");
            return;
        }

        if (userService.signup(email, password, name)) {
            try {
                User newUser = userService.getCurrentUser();
                goToNextPage(newUser);
            } catch (IOException e) {
                signupError.setText("❌ Error loading page");
                signupError.setStyle("-fx-text-fill: #E74C3C;");
            }
        } else {
            signupError.setText("❌ Email already exists or invalid format");
            signupError.setStyle("-fx-text-fill: #E74C3C;");
        }
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
