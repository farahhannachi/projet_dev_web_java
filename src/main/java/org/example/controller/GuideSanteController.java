package org.example.controller;

import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.shape.Circle;
import org.example.model.User;
import org.example.service.ResponseQuestionService;
import org.example.service.UserService;
import org.example.util.NavbarOrdonnanceMenu;
import org.example.util.SceneNavigation;

/**
 * Affiche le Guide Santé en JavaFX natif (sans WebView) pour éviter
 * l'IllegalAccessError liée au module javafx.web non exporté.
 */
public class GuideSanteController {

    // Navbar
    @FXML private HBox profileContainer;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;
    @FXML private Circle navbarAvatarCircle;
    @FXML private Label navbarUsername;
    @FXML private Label navbarAvatarLabel;
    @FXML private Label messagesBadge;

    // Contenu
    @FXML private VBox guideContentBox;
    @FXML private ScrollPane guideScrollPane;

    private final UserService userService = UserService.getInstance();
    private final ResponseQuestionService responseQuestionService = new ResponseQuestionService();

    @FXML
    public void initialize() {
        // Navbar
        User currentUser = userService.getCurrentUser();
        if (navbarUsername != null && currentUser != null) {
            String nom = currentUser.getNom() != null ? currentUser.getNom() : currentUser.getEmail();
            navbarUsername.setText(nom.split(" ")[0]);
        }
        if (dashboardMenuItem != null) {
            dashboardMenuItem.setVisible(userService.isAdmin());
            dashboardMenuItem.setManaged(userService.isAdmin());
        }
        if (navbarAvatarCircle != null) {
            navbarAvatarCircle.setStyle("-fx-fill: #1f6f54; -fx-stroke: white; -fx-stroke-width: 2;");
        }
        NavbarOrdonnanceMenu.wirePopupStyle(profileContainer);
        updateMessagesBadge();

        // Contenu guide
        if (guideContentBox != null) {
            buildGuideContent();
        }
    }

    // -------------------------------------------------------------------------
    // Construction du contenu JavaFX natif
    // -------------------------------------------------------------------------

    private void buildGuideContent() {
        guideContentBox.getChildren().clear();
        guideContentBox.getChildren().addAll(
                buildHeroSection(),
                buildCategoriesSection(),
                buildTipsSection(),
                buildBmiSection(),
                buildEmergencySection()
        );
    }

    /** Bannière hero verte */
    private Node buildHeroSection() {
        VBox hero = new VBox(16);
        hero.setAlignment(Pos.CENTER);
        hero.setPadding(new Insets(48, 32, 48, 32));
        hero.setStyle("-fx-background-color: linear-gradient(to bottom right, #16563f, #28a745);");

        Label icon = new Label("❤️");
        icon.setStyle("-fx-font-size: 48px;");

        Label title = new Label("Votre Guide Santé Complet");
        title.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: white; -fx-text-alignment: center;");
        title.setWrapText(true);

        Label subtitle = new Label("Conseils d'experts, informations fiables et outils interactifs pour votre santé au quotidien — CuraVita.");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.88); -fx-text-alignment: center;");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(600);

        HBox stats = new HBox(40);
        stats.setAlignment(Pos.CENTER);
        stats.getChildren().addAll(
                buildStat("150+", "Articles"),
                buildStat("6", "Thématiques"),
                buildStat("24/7", "Accompagnement")
        );

        hero.getChildren().addAll(icon, title, subtitle, stats);
        return hero;
    }

    private VBox buildStat(String number, String label) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        Label num = new Label(number);
        num.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 12px; -fx-text-fill: rgba(255,255,255,0.75);");
        box.getChildren().addAll(num, lbl);
        return box;
    }

    /** Grille des 6 catégories */
    private Node buildCategoriesSection() {
        VBox section = new VBox(24);
        section.setPadding(new Insets(40, 32, 40, 32));
        section.setStyle("-fx-background-color: #f8fafc;");

        Label tag = new Label("📚 Explorez");
        tag.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-padding: 4 12 4 12; -fx-background-radius: 20; -fx-font-size: 12px;");

        Label title = new Label("Nos Thématiques Santé");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label subtitle = new Label("Informations rédigées pour vous aider à prendre soin de vous (information générale, non médicale personnalisée).");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");
        subtitle.setWrapText(true);

        // Grille 3×2
        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setMaxWidth(Double.MAX_VALUE);

        String[][] categories = {
            {"🛡️", "Immunité", "Vitamines, alimentation et réflexes pour soutenir vos défenses naturelles.", "#16a34a"},
            {"🥗", "Nutrition", "Équilibre alimentaire, assiette idéale et exemples de menus.", "#0891b2"},
            {"🧠", "Santé mentale", "Stress, sommeil émotionnel et techniques de relaxation.", "#7c3aed"},
            {"🏃", "Activité physique", "Programmes légers, exercices sans matériel et précautions.", "#ea580c"},
            {"😴", "Sommeil", "Cycles du sommeil, routine du coucher et hygiène de vie.", "#0284c7"},
            {"💉", "Prévention", "Dépistages, vaccinations à discuter avec votre médecin, trousse à pharmacie.", "#dc2626"}
        };

        for (int i = 0; i < categories.length; i++) {
            String[] cat = categories[i];
            VBox card = buildCategoryCard(cat[0], cat[1], cat[2], cat[3]);
            grid.add(card, i % 3, i / 3);
            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(33.33);
            cc.setHgrow(Priority.ALWAYS);
            if (grid.getColumnConstraints().size() <= i % 3) {
                grid.getColumnConstraints().add(cc);
            }
        }

        section.getChildren().addAll(tag, title, subtitle, grid);
        return section;
    }

    private VBox buildCategoryCard(String emoji, String title, String desc, String color) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(20));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                + "-fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-border-width: 1; "
                + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);");
        card.setMaxWidth(Double.MAX_VALUE);

        Label iconLbl = new Label(emoji);
        iconLbl.setStyle("-fx-font-size: 28px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label descLbl = new Label(desc);
        descLbl.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        descLbl.setWrapText(true);

        Label link = new Label("Découvrir →");
        link.setStyle("-fx-font-size: 12px; -fx-text-fill: " + color + "; -fx-font-weight: bold;");

        card.getChildren().addAll(iconLbl, titleLbl, descLbl, link);
        return card;
    }

    /** Section conseils */
    private Node buildTipsSection() {
        VBox section = new VBox(24);
        section.setPadding(new Insets(40, 32, 40, 32));
        section.setStyle("-fx-background-color: white;");

        Label tag = new Label("💡 Conseils");
        tag.setStyle("-fx-background-color: #fef9c3; -fx-text-fill: #ca8a04; -fx-padding: 4 12 4 12; -fx-background-radius: 20; -fx-font-size: 12px;");

        Label title = new Label("Habitudes pour une vie plus saine");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label subtitle = new Label("Des gestes simples au quotidien — adaptez-les à votre situation.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        String[][] tips = {
            {"1", "Hydratation", "Buvez régulièrement dans la journée ; adaptez la quantité à votre activité et aux avis médicaux."},
            {"2", "Sommeil", "Viser des horaires réguliers et limiter les écrans avant le coucher."},
            {"3", "Mouvement", "Marche, vélo, étirements : privilégiez la régularité plutôt que l'intensité au début."},
            {"4", "Alimentation variée", "Fruits, légumes, protéines, féculents complets ; limitez les excès de sucre et produits ultra-transformés."},
            {"5", "Gestion du stress", "Respiration, pauses courtes, activités qui vous ressourcent."},
            {"6", "Alcool & tabac", "Réduire progressivement ; demandez de l'aide aux professionnels de santé si besoin."},
            {"7", "Consultations", "Les bilans et dépistages se décident avec un médecin selon votre âge et vos antécédents."},
            {"8", "Observation", "En cas de symptômes persistants ou inquiétants, consultez sans attendre."}
        };

        FlowPane flow = new FlowPane(12, 12);
        flow.setPrefWrapLength(Double.MAX_VALUE);
        for (String[] tip : tips) {
            flow.getChildren().add(buildTipCard(tip[0], tip[1], tip[2]));
        }

        section.getChildren().addAll(tag, title, subtitle, flow);
        return section;
    }

    private HBox buildTipCard(String number, String title, String content) {
        HBox card = new HBox(12);
        card.setPadding(new Insets(16));
        card.setPrefWidth(340);
        card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; "
                + "-fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-border-width: 1;");

        Label num = new Label(number);
        num.setMinWidth(32);
        num.setMinHeight(32);
        num.setAlignment(Pos.CENTER);
        num.setStyle("-fx-background-color: #16563f; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-font-size: 13px; -fx-background-radius: 50%;");

        VBox text = new VBox(4);
        Label t = new Label(title);
        t.setStyle("-fx-font-weight: bold; -fx-font-size: 13px; -fx-text-fill: #111827;");
        Label c = new Label(content);
        c.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
        c.setWrapText(true);
        text.getChildren().addAll(t, c);
        HBox.setHgrow(text, Priority.ALWAYS);

        card.getChildren().addAll(num, text);
        return card;
    }

    /** Calculateur IMC */
    private Node buildBmiSection() {
        VBox section = new VBox(24);
        section.setPadding(new Insets(40, 32, 40, 32));
        section.setAlignment(Pos.CENTER);
        section.setStyle("-fx-background-color: #f0fdf4;");

        Label tag = new Label("🧮 Outil");
        tag.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; -fx-padding: 4 12 4 12; -fx-background-radius: 20; -fx-font-size: 12px;");

        Label title = new Label("Calculez votre IMC");
        title.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label subtitle = new Label("Indicateur simple — il ne remplace pas un avis médical.");
        subtitle.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");

        HBox form = new HBox(16);
        form.setAlignment(Pos.CENTER);
        form.setMaxWidth(500);

        TextField heightField = new TextField();
        heightField.setPromptText("Taille (cm)");
        heightField.setPrefWidth(140);
        heightField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d1d5db; -fx-padding: 8 12;");

        TextField weightField = new TextField();
        weightField.setPromptText("Poids (kg)");
        weightField.setPrefWidth(140);
        weightField.setStyle("-fx-background-radius: 8; -fx-border-radius: 8; -fx-border-color: #d1d5db; -fx-padding: 8 12;");

        Button calcBtn = new Button("Calculer");
        calcBtn.setStyle("-fx-background-color: #16563f; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-background-radius: 8; -fx-padding: 8 20;");

        Label resultLabel = new Label();
        resultLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold;");
        resultLabel.setVisible(false);

        calcBtn.setOnAction(e -> {
            try {
                double h = Double.parseDouble(heightField.getText().trim()) / 100.0;
                double w = Double.parseDouble(weightField.getText().trim());
                if (h <= 0 || w <= 0) throw new NumberFormatException();
                double bmi = w / (h * h);
                String status;
                String color;
                if (bmi < 18.5) { status = "Insuffisance pondérale"; color = "#0891b2"; }
                else if (bmi < 25) { status = "Poids normal ✓"; color = "#16a34a"; }
                else if (bmi < 30) { status = "Surpoids"; color = "#ca8a04"; }
                else { status = "Obésité"; color = "#dc2626"; }
                resultLabel.setText(String.format("IMC : %.1f — %s", bmi, status));
                resultLabel.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: " + color + ";");
                resultLabel.setVisible(true);
            } catch (NumberFormatException ex) {
                resultLabel.setText("Veuillez entrer des valeurs numériques valides.");
                resultLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: #dc2626;");
                resultLabel.setVisible(true);
            }
        });

        form.getChildren().addAll(heightField, weightField, calcBtn);
        section.getChildren().addAll(tag, title, subtitle, form, resultLabel);
        return section;
    }

    /** Section urgences */
    private Node buildEmergencySection() {
        HBox section = new HBox(32);
        section.setPadding(new Insets(32, 40, 32, 40));
        section.setAlignment(Pos.CENTER);
        section.setStyle("-fx-background-color: #1e293b;");

        VBox left = new VBox(8);
        left.setAlignment(Pos.CENTER_LEFT);
        Label icon = new Label("🚨");
        icon.setStyle("-fx-font-size: 32px;");
        Label title = new Label("Urgences & numéros utiles");
        title.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label desc = new Label("En cas de détresse vitale, contactez immédiatement les services d'urgence.");
        desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #94a3b8;");
        desc.setWrapText(true);
        left.getChildren().addAll(icon, title, desc);
        HBox.setHgrow(left, Priority.ALWAYS);

        HBox numbers = new HBox(16);
        numbers.setAlignment(Pos.CENTER);
        numbers.getChildren().addAll(
                buildEmergencyNumber("190", "SAMU"),
                buildEmergencyNumber("198", "Pompiers"),
                buildEmergencyNumber("197", "Police")
        );

        section.getChildren().addAll(left, numbers);
        return section;
    }

    private VBox buildEmergencyNumber(String number, String label) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(12, 20, 12, 20));
        box.setStyle("-fx-background-color: #dc2626; -fx-background-radius: 10;");

        Label num = new Label(number);
        num.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: white;");
        Label lbl = new Label(label);
        lbl.setStyle("-fx-font-size: 11px; -fx-text-fill: rgba(255,255,255,0.85);");

        box.getChildren().addAll(num, lbl);
        return box;
    }

    // -------------------------------------------------------------------------
    // Navbar helpers
    // -------------------------------------------------------------------------

    private void updateMessagesBadge() {
        if (messagesBadge == null) return;
        User u = userService.getCurrentUser();
        if (u == null) { messagesBadge.setVisible(false); messagesBadge.setManaged(false); return; }
        int count = responseQuestionService.countUnreadResponsesForClient(u.getId());
        messagesBadge.setText(String.valueOf(count));
        messagesBadge.setVisible(count > 0);
        messagesBadge.setManaged(count > 0);
    }

    private Node navAnchor() {
        return profileContainer;
    }

    private void closeProfileDropdown() {
        if (profileDropdown != null) {
            profileDropdown.setVisible(false);
            profileDropdown.setManaged(false);
        }
    }

    @FXML
    private void toggleProfileDropdown() {
        if (profileDropdown != null) {
            boolean next = !profileDropdown.isVisible();
            profileDropdown.setVisible(next);
            profileDropdown.setManaged(next);
            if (next) profileDropdown.toFront();
        }
    }

    @FXML private void handleNavGuide() { /* déjà sur cette page */ }
    @FXML private void handleNavProduits() { SceneNavigation.replaceScene(navAnchor(), "/fxml/FrontProduits.fxml"); }
    @FXML private void handleNavCommandes() { SceneNavigation.replaceScene(navAnchor(), "/fxml/FrontMesCommandes.fxml"); }
    @FXML private void handleNavServices() { SceneNavigation.replaceScene(navAnchor(), "/fxml/FrontServices.fxml"); }
    @FXML private void handleNavPanier() { SceneNavigation.replaceScene(navAnchor(), "/fxml/FrontCommande.fxml"); }
    @FXML private void handleNavAdresses() { SceneNavigation.replaceScene(navAnchor(), "/fxml/FrontMesAdresses.fxml"); }
    @FXML private void handleNavContact() { SceneNavigation.replaceScene(navAnchor(), "/fxml/ContactPage.fxml"); }
    @FXML private void handleNavAbout() { SceneNavigation.replaceScene(navAnchor(), "/fxml/APropos.fxml"); }
    @FXML private void handleNavbarSearch() { if (guideScrollPane != null) guideScrollPane.requestFocus(); }
    @FXML private void goToAccueil() { SceneNavigation.replaceScene(navAnchor(), "/fxml/Accueil.fxml"); }
    @FXML private void goToTraitement() { SceneNavigation.replaceScene(navAnchor(), "/fxml/Traitement.fxml"); }
    @FXML private void goToMesOrdonnances() { SceneNavigation.replaceScene(navAnchor(), "/fxml/MesOrdonnances.fxml"); }
    @FXML private void goToCreerOrdonnance() { SceneNavigation.replaceScene(navAnchor(), "/fxml/Ordonnance.fxml"); }
    @FXML private void goToProfil() { closeProfileDropdown(); SceneNavigation.replaceScene(navAnchor(), "/fxml/Profil.fxml"); }
    @FXML private void goToMessagesPage() { closeProfileDropdown(); SceneNavigation.replaceScene(navAnchor(), "/fxml/MessagesPage.fxml"); }
    @FXML private void goToDashboard() { closeProfileDropdown(); SceneNavigation.replaceScene(navAnchor(), "/fxml/Dashboard.fxml"); }
    @FXML private void logout() { closeProfileDropdown(); userService.logout(); SceneNavigation.replaceScene(navAnchor(), "/fxml/Login.fxml"); }
}
