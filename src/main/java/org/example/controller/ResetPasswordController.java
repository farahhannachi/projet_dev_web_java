package org.example.controller;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;
import org.example.service.PasswordResetService;
import org.example.util.EmailUtil;

import java.io.IOException;

/**
 * Controller for Reset Password page
 * Handles email verification and reset token generation
 */
public class ResetPasswordController {
    
    @FXML private TextField emailInput;
    @FXML private TextField tokenInput;
    @FXML private Label errorLabel;
    @FXML private VBox successBox;
    @FXML private Button sendButton;
    @FXML private VBox emailSection;
    @FXML private VBox tokenSection;
    @FXML private Hyperlink backToSignInLink;
    @FXML private Text headerSubtitle;
    
    /**
     * Initialize controller
     */
    @FXML
    private void initialize() {
        errorLabel.setText("");
        successBox.setVisible(false);
    }
    
    /**
     * Handle Send Reset Link button click
     */
    @FXML
    private void handleSendReset() {
        String email = emailInput.getText().trim();
        
        // Clear previous messages
        errorLabel.setText("");
        successBox.setVisible(false);
        
        // Validation
        if (email.isEmpty()) {
            showError("Please enter your email address");
            return;
        }
        
        if (!isValidEmail(email)) {
            showError("Please enter a valid email address");
            return;
        }
        
        // Disable button during processing
        sendButton.setDisable(true);
        
        // Generate reset token
        String resetToken = PasswordResetService.generateResetToken(email);
        
        if (resetToken != null) {
            // Send email with reset link
            boolean emailSent = EmailUtil.sendPasswordResetEmail(email, resetToken);
            
            if (emailSent) {
                showSuccess();
                // Clear input
                emailInput.clear();
                
                // Switch to token entry mode
                switchToTokenEntry();
            } else {
                showError("Failed to send email. Please try again later.");
                sendButton.setDisable(false);
            }
        } else {
            showError("Email not found in our system");
            sendButton.setDisable(false);
        }
    }
    
    /**
     * Show success message with animation
     */
    private void showSuccess() {
        successBox.setOpacity(0);
        successBox.setVisible(true);
        
        FadeTransition fadeIn = new FadeTransition(Duration.millis(500), successBox);
        fadeIn.setFromValue(0);
        fadeIn.setToValue(1);
        fadeIn.play();
    }
    
    /**
     * Show error message
     */
    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-size: 14;");
    }
    
    /**
     * Validate email format
     */
    private boolean isValidEmail(String email) {
        return email.matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");
    }
    
    /**
     * Switch UI to token entry mode after email is sent
     */
    private void switchToTokenEntry() {
        // Hide email section and success box
        emailSection.setVisible(false);
        emailSection.setManaged(false);
        successBox.setVisible(false);
        successBox.setManaged(false);
        
        // Show token section
        tokenSection.setVisible(true);
        tokenSection.setManaged(true);
        
        // Update UI text
        headerSubtitle.setText("Enter the reset token from your email");
        sendButton.setText("CHECK TOKEN");
        sendButton.setOnAction(e -> handleTokenReset());
        sendButton.setDisable(false);
        
        // Focus token input
        tokenInput.requestFocus();
        
        System.out.println("[RESET] Switched to token entry mode");
    }
    
    /**
     * Handle token validation and navigate to NewPassword page
     */
    @FXML
    private void handleTokenReset() {
        String token = tokenInput.getText().trim();
        
        // Clear previous error
        errorLabel.setText("");
        
        // Validate token input
        if (token.isEmpty()) {
            showError("Please enter the reset token");
            return;
        }
        
        // Verify token with database
        String email = PasswordResetService.verifyResetToken(token);
        
        if (email != null) {
            // Token is valid - navigate to NewPassword page
            System.out.println("[RESET] Token verified for: " + email);
            navigateToNewPassword(token);
        } else {
            // Token is invalid or expired
            showError("Invalid or expired token. Please request a new reset link.");
            tokenInput.clear();
            tokenInput.requestFocus();
        }
    }
    
    /**
     * Navigate to New Password page with token
     */
    public static void navigateToNewPassword(String token) {
        try {
            FXMLLoader loader = new FXMLLoader(ResetPasswordController.class.getResource("/fxml/NewPassword.fxml"));
            Parent root = loader.load();
            
            NewPasswordController controller = loader.getController();
            controller.initializeWithToken(token);
            
            Scene scene = new Scene(root);
            java.net.URL cssUrl = org.example.util.SceneNavigation.class.getResource(org.example.util.SceneNavigation.STYLESHEET_PATH);
            if (cssUrl != null) {
                scene.getStylesheets().add(cssUrl.toExternalForm());
            }
            
            Stage stage = (Stage) Stage.getWindows().get(0);
            stage.setScene(scene);
            
            System.out.println("[RESET] Navigated to New Password page with token");
        } catch (IOException e) {
            System.err.println("[ERROR] Failed to load New Password page: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Navigate back to Sign In page
     */
    @FXML
    private void handleBackToSignIn() {
        org.example.util.SceneNavigation.replaceScene(emailInput, "/fxml/Login.fxml");
        System.out.println("[RESET] Navigated back to Sign In");
    }
}

