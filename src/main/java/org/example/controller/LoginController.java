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

    @FXML private javafx.scene.layout.VBox signInPanel;
    @FXML private javafx.scene.layout.VBox signUpPanel;
    @FXML private StackPane backgroundPane;

    private UserService userService = UserService.getInstance();
    private boolean isSignInMode = true;
    private boolean isAnimating = false;

    @FXML
    private void initialize() {
        if (!UserService.isDatabaseConnected()) {
            loginError.setText("⚠️ Database not connected. Please start MySQL.");
            loginError.setStyle("-fx-text-fill: #F39C12;");
        }
        // Sign Up panel is fully hidden at startup
        if (signUpPanel != null) {
            signUpPanel.setVisible(false);
            signUpPanel.setManaged(false);
            signUpPanel.setOpacity(0);
            signUpPanel.setTranslateX(1200);
        }
        // Sign In panel centered
        if (signInPanel != null) {
            signInPanel.setTranslateX(0);
            signInPanel.setOpacity(1);
        }
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

        // Make Sign Up panel visible before animating
        signUpPanel.setVisible(true);
        signUpPanel.setManaged(true);
        signUpPanel.setTranslateX(1200);
        signUpPanel.setOpacity(0);

        // Sign In slides left and fades out
        TranslateTransition signInOut = new TranslateTransition(Duration.millis(400), signInPanel);
        signInOut.setToX(-500);
        signInOut.setInterpolator(Interpolator.EASE_BOTH);
        FadeTransition signInFade = new FadeTransition(Duration.millis(350), signInPanel);
        signInFade.setToValue(0);

        // Sign Up slides in from right and fades in
        TranslateTransition signUpIn = new TranslateTransition(Duration.millis(400), signUpPanel);
        signUpIn.setFromX(1200);
        signUpIn.setToX(0);
        signUpIn.setInterpolator(Interpolator.EASE_BOTH);
        FadeTransition signUpFade = new FadeTransition(Duration.millis(400), signUpPanel);
        signUpFade.setFromValue(0);
        signUpFade.setToValue(1);

        ParallelTransition out = new ParallelTransition(signInOut, signInFade);
        ParallelTransition in = new ParallelTransition(signUpIn, signUpFade);
        SequentialTransition seq = new SequentialTransition(out, in);
        seq.setOnFinished(e -> {
            signInPanel.setVisible(false);
            signInPanel.setManaged(false);
            isAnimating = false;
        });
        seq.play();
    }

    @FXML
    private void showSignIn() {
        if (isSignInMode || isAnimating) return;
        isAnimating = true;
        isSignInMode = true;

        // Make Sign In panel visible before animating
        signInPanel.setVisible(true);
        signInPanel.setManaged(true);
        signInPanel.setTranslateX(-500);
        signInPanel.setOpacity(0);

        // Sign Up slides right and fades out
        TranslateTransition signUpOut = new TranslateTransition(Duration.millis(400), signUpPanel);
        signUpOut.setToX(1200);
        signUpOut.setInterpolator(Interpolator.EASE_BOTH);
        FadeTransition signUpFade = new FadeTransition(Duration.millis(350), signUpPanel);
        signUpFade.setToValue(0);

        // Sign In slides in from left and fades in
        TranslateTransition signInIn = new TranslateTransition(Duration.millis(400), signInPanel);
        signInIn.setFromX(-500);
        signInIn.setToX(0);
        signInIn.setInterpolator(Interpolator.EASE_BOTH);
        FadeTransition signInFade = new FadeTransition(Duration.millis(400), signInPanel);
        signInFade.setFromValue(0);
        signInFade.setToValue(1);

        ParallelTransition out = new ParallelTransition(signUpOut, signUpFade);
        ParallelTransition in = new ParallelTransition(signInIn, signInFade);
        SequentialTransition seq = new SequentialTransition(out, in);
        seq.setOnFinished(e -> {
            signUpPanel.setVisible(false);
            signUpPanel.setManaged(false);
            isAnimating = false;
        });
        seq.play();
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

    @FXML
    private void handleForgotPassword() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/fxml/ResetPassword.fxml"));
            Parent root = loader.load();
            Scene scene = new Scene(root);
            scene.getStylesheets().add(getClass().getResource("/css/styles.css").toExternalForm());
            Stage stage = (Stage) loginEmail.getScene().getWindow();
            stage.setScene(scene);
        } catch (Exception e) {
            e.printStackTrace();
            loginError.setText("❌ Erreur navigation : " + e.getMessage());
        }
    }
}
