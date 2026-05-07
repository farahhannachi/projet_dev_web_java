package org.example.controller;

import javafx.animation.FadeTransition;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.service.PasswordResetService;
import org.mindrot.jbcrypt.BCrypt;

/**
 * Controller for New Password page
 * Handles password reset with token verification
 */
public class NewPasswordController {
    
    @FXML private PasswordField newPasswordInput;
    @FXML private PasswordField confirmPasswordInput;
    @FXML private Label passwordError;
    @FXML private Label confirmError;
    @FXML private Label errorLabel;
    @FXML private VBox successBox;
    @FXML private Button resetButton;
    
    // Password requirement labels
    @FXML private Label reqLength;
    @FXML private Label reqUppercase;
    @FXML private Label reqNumber;
    
    private String resetToken;
    private boolean passwordValid = false;
    private boolean passwordsMatch = false;
    
    /**
     * Initialize controller with reset token
     */
    public void initializeWithToken(String token) {
        this.resetToken = token;
        
        // Verify token
        String email = PasswordResetService.verifyResetToken(token);
        if (email == null) {
            showError("❌ Invalid or expired reset link. Please request a new password reset.");
            resetButton.setDisable(true);
        } else {
            System.out.println("[RESET] Token verified for email: " + email);
        }
    }
    
    /**
     * Initialize controller
     */
    @FXML
    private void initialize() {
        errorLabel.setText("");
        successBox.setVisible(false);
        passwordError.setText("");
        confirmError.setText("");
    }
    
    /**
     * Validate password strength
     */
    @FXML
    private void validatePasswordStrength() {
        String password = newPasswordInput.getText();
        passwordError.setText("");
        
        // Reset all requirements
        boolean hasMinLength = password.length() >= 8;
        boolean hasUppercase = password.matches(".*[A-Z].*");
        boolean hasNumber = password.matches(".*\\d.*");
        
        // Update UI
        updateRequirement(reqLength, hasMinLength, "✓ At least 8 characters", "✗ At least 8 characters");
        updateRequirement(reqUppercase, hasUppercase, "✓ Uppercase letter (A-Z)", "✗ Uppercase letter (A-Z)");
        updateRequirement(reqNumber, hasNumber, "✓ Number (0-9)", "✗ Number (0-9)");
        
        // Check if all requirements met
        passwordValid = hasMinLength && hasUppercase && hasNumber;
        
        // Validate confirm password if it has content
        if (!confirmPasswordInput.getText().isEmpty()) {
            validateConfirmPassword();
        }
    }
    
    /**
     * Validate confirm password
     */
    @FXML
    private void validateConfirmPassword() {
        String password = newPasswordInput.getText();
        String confirmPassword = confirmPasswordInput.getText();
        
        confirmError.setText("");
        
        if (password.isEmpty()) {
            confirmError.setText("Enter password first");
            passwordsMatch = false;
            return;
        }
        
        if (confirmPassword.isEmpty()) {
            passwordsMatch = false;
            return;
        }
        
        if (password.equals(confirmPassword)) {
            confirmError.setText("");
            passwordsMatch = true;
        } else {
            confirmError.setText("Passwords do not match");
            passwordsMatch = false;
        }
    }
    
    /**
     * Update requirement label styling
     */
    private void updateRequirement(Label label, boolean isMet, String checkedText, String uncheckedText) {
        if (isMet) {
            label.setText(checkedText);
            label.setStyle("-fx-text-fill: #27ae60;");
        } else {
            label.setText(uncheckedText);
            label.setStyle("-fx-text-fill: #e74c3c;");
        }
    }
    
    /**
     * Handle Reset Password button click
     */
    @FXML
    private void handleResetPassword() {
        // Clear previous messages
        errorLabel.setText("");
        successBox.setVisible(false);
        
        // Validation
        if (!passwordValid) {
            showError("❌ Password does not meet requirements");
            return;
        }
        
        if (!passwordsMatch) {
            showError("❌ Passwords do not match");
            return;
        }
        
        if (resetToken == null || resetToken.isEmpty()) {
            showError("❌ Invalid reset session");
            return;
        }
        
        // Disable button during processing
        resetButton.setDisable(true);
        
        // Reset password
        String newPassword = newPasswordInput.getText();
        boolean success = PasswordResetService.resetPassword(resetToken, newPassword);
        
        if (success) {
            showSuccess();
            
            // Auto-redirect to login after 3 seconds
            new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    javafx.application.Platform.runLater(this::handleBackToSignIn);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }).start();
        } else {
            showError("❌ Failed to reset password. Please try again.");
            resetButton.setDisable(false);
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
     * Navigate back to Sign In page
     */
    @FXML
    private void handleBackToSignIn() {
        org.example.util.SceneNavigation.replaceScene(newPasswordInput, "/fxml/Login.fxml");
        System.out.println("[RESET] Navigated back to Sign In");
    }
}

