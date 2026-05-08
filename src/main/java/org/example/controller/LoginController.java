package org.example.controller;

import javafx.animation.*;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.util.Duration;
import org.example.model.User;
import org.example.service.TwoFactorAuthService;
import org.example.service.TwilioVerifyService;
import org.example.service.UserService;
import org.example.util.SceneNavigation;

public class LoginController {
    @FXML private TextField loginEmail;
    @FXML private PasswordField loginPassword;
    @FXML private Label loginError;
    @FXML private VBox loginStepCredentials;
    @FXML private VBox loginStepTotp;
    @FXML private TextField loginTotpCode;

    @FXML private TextField signupName;
    @FXML private TextField signupEmail;
    @FXML private TextField signupPhone;
    @FXML private PasswordField signupPassword;
    @FXML private Button signupSendSmsButton;
    @FXML private VBox signupOtpBox;
    @FXML private TextField signupOtpCode;
    @FXML private Label signupError;

    @FXML private javafx.scene.layout.VBox signInPanel;
    @FXML private javafx.scene.layout.VBox signUpPanel;
    @FXML private StackPane backgroundPane;

    private UserService userService = UserService.getInstance();
    private final TwoFactorAuthService twoFactorAuthService = new TwoFactorAuthService();
    private final TwilioVerifyService twilioVerifyService = TwilioVerifyService.getInstance();
    /** Téléphone national normalisé (stocké DB) après envoi Verify réussi. */
    private String signupPendingNationalPhone;
    /** E.164 pour Twilio Verify (check). */
    private String signupPendingE164;
    /** Utilisateur authentifié par mot de passe en attente de validation TOTP ({@code currentUser} pas encore défini). */
    private User pendingTotpUser;
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
        cancelTotpFlowSilent();
        resetSignupVerificationUi();
    }

    private void resetSignupVerificationUi() {
        signupPendingNationalPhone = null;
        signupPendingE164 = null;
        if (signupOtpBox != null) {
            signupOtpBox.setVisible(false);
            signupOtpBox.setManaged(false);
        }
        if (signupOtpCode != null) {
            signupOtpCode.clear();
        }
        if (signupSendSmsButton != null) {
            signupSendSmsButton.setDisable(false);
        }
    }

    private boolean validateSignupFormBasics(String name, String email, String password, String phoneRaw) {
        signupError.setText("");
        if (name.isEmpty() || email.isEmpty() || password.isEmpty() || phoneRaw.isBlank()) {
            signupError.setText("❌ Tous les champs sont requis");
            signupError.setStyle("-fx-text-fill: #E74C3C;");
            return false;
        }
        String phone = UserService.normalizePhoneNumber(phoneRaw);
        if (!UserService.isValidPhoneNumber(phone)) {
            signupError.setText("❌ Numéro invalide : au moins 8 chiffres (ex: +216XXXXXXXX ou 98XXXXXX)");
            signupError.setStyle("-fx-text-fill: #E74C3C;");
            return false;
        }
        if (!email.matches("^[a-zA-Z0-9]+@gmail\\.com$")) {
            signupError.setText("❌ Invalid email format (use: name@gmail.com or mundo36@gmail.com)");
            signupError.setStyle("-fx-text-fill: #E74C3C;");
            return false;
        }
        return true;
    }

    @FXML
    private void handleSignupSendCode() {
        if (isAnimating) {
            return;
        }

        String name = signupName.getText().trim();
        String email = signupEmail.getText().trim();
        String password = signupPassword.getText();
        String phoneRaw = signupPhone != null ? signupPhone.getText() : "";

        if (!validateSignupFormBasics(name, email, password, phoneRaw)) {
            return;
        }

        // --- TWILIO DÉSACTIVÉ TEMPORAIREMENT ---
        // La vérification SMS est bypassée : le compte est créé directement.
        if (userService.isEmailTaken(email)) {
            signupError.setText("❌ Cet email est déjà utilisé");
            signupError.setStyle("-fx-text-fill: #E74C3C;");
            return;
        }

        String national = UserService.normalizePhoneNumber(phoneRaw);
        signupPendingNationalPhone = national;
        signupPendingE164 = "SKIP"; // marqueur bypass

        if (userService.signup(email, password, name, national)) {
            User newUser = userService.getCurrentUser();
            resetSignupVerificationUi();
            goToNextPage(newUser);
        } else {
            signupError.setText("❌ Erreur lors de la création du compte");
            signupError.setStyle("-fx-text-fill: #E74C3C;");
        }
    }

    @FXML
    private void handleSignupConfirm() {
        if (isAnimating) {
            return;
        }

        String name = signupName.getText().trim();
        String email = signupEmail.getText().trim();
        String password = signupPassword.getText();
        String phoneRaw = signupPhone != null ? signupPhone.getText() : "";

        if (!validateSignupFormBasics(name, email, password, phoneRaw)) {
            return;
        }

        // --- TWILIO DÉSACTIVÉ TEMPORAIREMENT ---
        // Le compte est déjà créé dans handleSignupSendCode, cette étape est bypassée.
        signupError.setText("ℹ️ Vérification SMS désactivée temporairement.");
        signupError.setStyle("-fx-text-fill: #1f6f5c;");
        /*
        if (signupPendingE164 == null || signupPendingNationalPhone == null) {
            signupError.setText("❌ Envoyez d’abord le code SMS avec « Envoyer le code SMS »");
            signupError.setStyle("-fx-text-fill: #E74C3C;");
            return;
        }

        String nationalCurrent = UserService.normalizePhoneNumber(phoneRaw);
        if (!nationalCurrent.equals(signupPendingNationalPhone)) {
            signupError.setText("❌ Le numéro a changé après l’envoi du SMS. Renvoyez le code.");
            signupError.setStyle("-fx-text-fill: #E74C3C;");
            resetSignupVerificationUi();
            return;
        }

        String otp = signupOtpCode != null ? signupOtpCode.getText().trim() : "";
        if (!otp.matches("\\d{6}")) {
            signupError.setText("❌ Entrez le code à 6 chiffres reçu par SMS");
            signupError.setStyle("-fx-text-fill: #E74C3C;");
            return;
        }

        signupError.setText("");
        Thread worker = new Thread(() -> {
            boolean ok = twilioVerifyService.checkVerificationCode(signupPendingE164, otp);
            javafx.application.Platform.runLater(() -> {
                if (!ok) {
                    signupError.setText("❌ Code incorrect ou expiré. Renvoyez un nouveau SMS si besoin.");
                    signupError.setStyle("-fx-text-fill: #E74C3C;");
                    return;
                }
                if (userService.isEmailTaken(email)) {
                    signupError.setText("❌ Cet email est déjà utilisé");
                    signupError.setStyle("-fx-text-fill: #E74C3C;");
                    resetSignupVerificationUi();
                    return;
                }
                if (userService.signup(email, password, name, signupPendingNationalPhone)) {
                    User newUser = userService.getCurrentUser();
                    resetSignupVerificationUi();
                    goToNextPage(newUser);
                } else {
                    signupError.setText("❌ Erreur lors de la création du compte");
                    signupError.setStyle("-fx-text-fill: #E74C3C;");
                }
            });
        }, "twilio-verify-check");
        worker.setDaemon(true);
        worker.start();
        */
    }

    private boolean requiresTotpChallenge(User user) {
        return user != null
                && user.isTotpEnabled()
                && user.getTotpSecret() != null
                && !user.getTotpSecret().isBlank();
    }

    private void showTotpChallenge() {
        if (loginStepCredentials != null) {
            loginStepCredentials.setVisible(false);
            loginStepCredentials.setManaged(false);
        }
        if (loginStepTotp != null) {
            loginStepTotp.setVisible(true);
            loginStepTotp.setManaged(true);
        }
        if (loginTotpCode != null) {
            loginTotpCode.clear();
        }
        loginError.setText("");
        javafx.application.Platform.runLater(() -> {
            if (loginTotpCode != null) {
                loginTotpCode.requestFocus();
            }
        });
    }

    private void cancelTotpFlowSilent() {
        pendingTotpUser = null;
        if (loginStepTotp != null) {
            loginStepTotp.setVisible(false);
            loginStepTotp.setManaged(false);
        }
        if (loginStepCredentials != null) {
            loginStepCredentials.setVisible(true);
            loginStepCredentials.setManaged(true);
        }
        if (loginTotpCode != null) {
            loginTotpCode.clear();
        }
    }

    @FXML
    private void handleTotpVerify() {
        if (pendingTotpUser == null) {
            cancelTotpFlowSilent();
            return;
        }
        String code = loginTotpCode != null ? loginTotpCode.getText().trim() : "";
        if (!code.matches("\\d{6}")) {
            loginError.setText("❌ Entrez un code à 6 chiffres");
            loginError.setStyle("-fx-text-fill: #E74C3C;");
            return;
        }
        if (!twoFactorAuthService.verifyCode(pendingTotpUser.getTotpSecret(), code)) {
            loginError.setText("❌ Code incorrect ou expiré. Réessayez.");
            loginError.setStyle("-fx-text-fill: #E74C3C;");
            return;
        }
        loginError.setText("");
        User user = pendingTotpUser;
        pendingTotpUser = null;
        userService.establishSessionAfterTotp(user);
        if (loginStepTotp != null) {
            loginStepTotp.setVisible(false);
            loginStepTotp.setManaged(false);
        }
        if (loginStepCredentials != null) {
            loginStepCredentials.setVisible(true);
            loginStepCredentials.setManaged(true);
        }
        if (loginTotpCode != null) {
            loginTotpCode.clear();
        }
        goToNextPage(user);
    }

    @FXML
    private void handleTotpCancel() {
        loginError.setText("");
        cancelTotpFlowSilent();
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
            if (requiresTotpChallenge(user)) {
                pendingTotpUser = user;
                showTotpChallenge();
            } else {
                goToNextPage(user);
            }
        } else {
            loginError.setText("❌ Invalid email or password");
            loginError.setStyle("-fx-text-fill: #E74C3C;");
        }
    }

    @FXML
    private void showSignUp() {
        if (!isSignInMode || isAnimating) return;
        cancelTotpFlowSilent();
        resetSignupVerificationUi();
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
        cancelTotpFlowSilent();
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
            resetSignupVerificationUi();
            isAnimating = false;
        });
        seq.play();
    }

    private void goToNextPage(User user) {
        if (user == null) {
            loginError.setText("❌ Session invalide");
            signupError.setText("❌ Session invalide");
            loginError.setStyle("-fx-text-fill: #E74C3C;");
            signupError.setStyle("-fx-text-fill: #E74C3C;");
            return;
        }
        String fxmlFile = user.getType() != null && user.getType().equals("admin")
                ? "/fxml/Dashboard.fxml"
                : "/fxml/Accueil.fxml";
        if (getClass().getResource(fxmlFile) == null) {
            loginError.setText("❌ Error loading page");
            signupError.setText("❌ Error loading page");
            loginError.setStyle("-fx-text-fill: #E74C3C;");
            signupError.setStyle("-fx-text-fill: #E74C3C;");
            return;
        }
        SceneNavigation.replaceScene(loginEmail, fxmlFile);
    }

    @FXML
    private void handleForgotPassword() {
        if (getClass().getResource("/fxml/ResetPassword.fxml") == null) {
            loginError.setText("❌ Erreur navigation : page introuvable");
            return;
        }
        SceneNavigation.replaceScene(loginEmail, "/fxml/ResetPassword.fxml");
    }
}
