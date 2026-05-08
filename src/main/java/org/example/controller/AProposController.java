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
 * Page « À propos » en JavaFX natif (sans WebView) pour éviter
 * l'IllegalAccessError liée au module javafx.web non exporté.
 */
public class AProposController {

    // Navbar
    @FXML private HBox profileContainer;
    @FXML private VBox profileDropdown;
    @FXML private Button dashboardMenuItem;
    @FXML private Circle navbarAvatarCircle;
    @FXML private Label navbarUsername;
    @FXML private Label navbarAvatarLabel;
    @FXML private Label messagesBadge;

    // Contenu
    @FXML private VBox contentBox;
    @FXML private ScrollPane contentScrollPane;

    private final UserService userService = UserService.getInstance();
    private final ResponseQuestionService responseQuestionService = new ResponseQuestionService();

    @FXML
    public void initialize() {
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

        if (contentBox != null) {
            buildContent();
        }
    }

    // -------------------------------------------------------------------------
    // Construction du contenu JavaFX natif
    // -------------------------------------------------------------------------

    private void buildContent() {
        contentBox.getChildren().addAll(
                buildHero(),
                buildStats(),
                buildMission(),
                buildTimeline(),
                buildTeam(),
                buildValues(),
                buildCta()
        );
    }

    /** Bannière hero */
    private Node buildHero() {
        VBox hero = new VBox(16);
        hero.setAlignment(Pos.CENTER);
        hero.setPadding(new Insets(56, 32, 56, 32));
        hero.setStyle("-fx-background-color: linear-gradient(to bottom right, #0f4c35, #1f6f54, #28a745);");

        Label badge = new Label("🇹🇳 Fièrement Tunisien");
        badge.setStyle("-fx-background-color: rgba(255,255,255,0.15); -fx-text-fill: white; "
                + "-fx-padding: 6 16 6 16; -fx-background-radius: 20; -fx-font-size: 13px;");

        Label title = new Label("Votre Santé, Notre Passion");
        title.setStyle("-fx-font-size: 32px; -fx-font-weight: bold; -fx-text-fill: white; -fx-text-alignment: center;");
        title.setWrapText(true);

        Label subtitle = new Label(
                "Depuis 2018, CuraVita révolutionne la pharmacie en ligne en Tunisie\n"
                + "avec une approche centrée sur l'humain, la technologie et l'excellence.");
        subtitle.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.85); -fx-text-alignment: center;");
        subtitle.setWrapText(true);
        subtitle.setMaxWidth(620);

        hero.getChildren().addAll(badge, title, subtitle);
        return hero;
    }

    /** Bande de statistiques */
    private Node buildStats() {
        HBox row = new HBox(0);
        row.setAlignment(Pos.CENTER);
        row.setStyle("-fx-background-color: white; -fx-border-color: #e5e7eb; -fx-border-width: 0 0 1 0;");

        String[][] stats = {
            {"💊", "15K+", "Produits"},
            {"🎯", "24", "Gouvernorats couverts"},
            {"👥", "500K+", "Clients satisfaits"},
            {"⭐", "4.9/5", "Note de satisfaction"}
        };

        for (String[] s : stats) {
            VBox box = new VBox(6);
            box.setAlignment(Pos.CENTER);
            box.setPadding(new Insets(28, 40, 28, 40));
            box.setStyle("-fx-border-color: #e5e7eb; -fx-border-width: 0 1 0 0;");
            HBox.setHgrow(box, Priority.ALWAYS);

            Label icon = new Label(s[0]);
            icon.setStyle("-fx-font-size: 24px;");
            Label number = new Label(s[1]);
            number.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #16563f;");
            Label label = new Label(s[2]);
            label.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");

            box.getChildren().addAll(icon, number, label);
            row.getChildren().add(box);
        }
        return row;
    }

    /** Section mission — 3 cartes */
    private Node buildMission() {
        VBox section = new VBox(28);
        section.setPadding(new Insets(48, 40, 48, 40));
        section.setStyle("-fx-background-color: #f8fafc;");

        section.getChildren().addAll(
                buildSectionHeader("Notre Mission", "Ce qui nous définit",
                        "Nous croyons que chaque personne mérite un accès facile à des soins de qualité"),
                buildCardRow(new String[][]{
                    {"🔬", "Innovation constante",
                     "Notre plateforme utilise les dernières technologies pour vous offrir une expérience fluide et sécurisée."},
                    {"💚", "Qualité garantie",
                     "Produits issus de circuits agréés et conseils dispensés par des pharmaciens diplômés."},
                    {"🤝", "Service humain",
                     "Derrière chaque commande, une équipe de professionnels disponibles pour vous orienter."}
                })
        );
        return section;
    }

    /** Timeline */
    private Node buildTimeline() {
        VBox section = new VBox(28);
        section.setPadding(new Insets(48, 40, 48, 40));
        section.setStyle("-fx-background-color: white;");

        section.getChildren().add(buildSectionHeader("Notre histoire",
                "Une success story tunisienne",
                "De Tunis au monde entier, une aventure entrepreneuriale au service de votre bien-être"));

        String[][] events = {
            {"2018", "🌱 Naissance à Tunis",
             "Création de CuraVita au cœur de la capitale tunisienne par une équipe passionnée."},
            {"2020", "🚀 Expansion vers Sfax et Sousse",
             "Ouverture de centres de distribution. Des milliers de familles tunisiennes nous font confiance."},
            {"2023", "💡 Innovation digitale",
             "Lancement d'une application moderne avec livraison express dans le Grand Tunis."},
            {"2025", "🌍 Leader national",
             "CuraVita renforce sa présence et ses services sur tout le territoire tunisien."}
        };

        VBox timeline = new VBox(0);
        for (String[] e : events) {
            HBox item = new HBox(20);
            item.setPadding(new Insets(16, 0, 16, 0));
            item.setStyle("-fx-border-color: transparent transparent #e5e7eb transparent; -fx-border-width: 0 0 1 0;");

            Label year = new Label(e[0]);
            year.setMinWidth(60);
            year.setStyle("-fx-font-size: 16px; -fx-font-weight: bold; -fx-text-fill: #16563f;");

            VBox content = new VBox(4);
            Label title = new Label(e[1]);
            title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");
            Label desc = new Label(e[2]);
            desc.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280;");
            desc.setWrapText(true);
            content.getChildren().addAll(title, desc);
            HBox.setHgrow(content, Priority.ALWAYS);

            item.getChildren().addAll(year, content);
            timeline.getChildren().add(item);
        }

        section.getChildren().add(timeline);
        return section;
    }

    /** Équipe */
    private Node buildTeam() {
        VBox section = new VBox(28);
        section.setPadding(new Insets(48, 40, 48, 40));
        section.setStyle("-fx-background-color: #f0fdf4;");

        section.getChildren().add(buildSectionHeader("Notre équipe",
                "Des experts à votre service",
                "Une équipe pluridisciplinaire passionnée par votre bien-être"));

        String[][] members = {
            {"👨‍💻", "Mohamed Yassin Essalah", "Fondateur & développeur full stack"},
            {"👩‍💼", "Emna Ben Aissa", "Directrice marketing & UX"},
            {"👩‍⚕️", "Emna Ben Badr", "Responsable qualité pharma"},
            {"👩‍🔬", "Farah Hannachi", "Pharmacienne conseil"},
            {"👨‍💼", "Iheb Ben Jbir", "Directeur des opérations"}
        };

        FlowPane flow = new FlowPane(16, 16);
        flow.setPrefWrapLength(Double.MAX_VALUE);
        for (String[] m : members) {
            VBox card = new VBox(8);
            card.setAlignment(Pos.CENTER);
            card.setPadding(new Insets(24, 20, 24, 20));
            card.setPrefWidth(180);
            card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                    + "-fx-border-color: #d1fae5; -fx-border-radius: 12; -fx-border-width: 1; "
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);");

            Label avatar = new Label(m[0]);
            avatar.setStyle("-fx-font-size: 36px;");
            Label name = new Label(m[1]);
            name.setStyle("-fx-font-size: 13px; -fx-font-weight: bold; -fx-text-fill: #111827; -fx-text-alignment: center;");
            name.setWrapText(true);
            name.setMaxWidth(160);
            Label role = new Label(m[2]);
            role.setStyle("-fx-font-size: 11px; -fx-text-fill: #16a34a; -fx-text-alignment: center;");
            role.setWrapText(true);
            role.setMaxWidth(160);

            card.getChildren().addAll(avatar, name, role);
            flow.getChildren().add(card);
        }

        section.getChildren().add(flow);
        return section;
    }

    /** Valeurs */
    private Node buildValues() {
        VBox section = new VBox(28);
        section.setPadding(new Insets(48, 40, 48, 40));
        section.setStyle("-fx-background-color: white;");

        section.getChildren().add(buildSectionHeader("Nos valeurs",
                "Ce en quoi nous croyons",
                "Des principes fondamentaux qui guident chacune de nos actions"));

        String[][] values = {
            {"01", "Intégrité", "Transparence et honnêteté au cœur de notre relation avec vous."},
            {"02", "Excellence", "Nous visons la qualité dans chaque détail, du service à la préparation de vos commandes."},
            {"03", "Accessibilité", "Des soins et une plateforme pensés pour être simples et accessibles au plus grand nombre."},
            {"04", "Durabilité", "Nous encourageons des gestes responsables : logistique optimisée et partenariats locaux."}
        };

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        for (int i = 0; i < values.length; i++) {
            String[] v = values[i];
            HBox card = new HBox(16);
            card.setPadding(new Insets(20));
            card.setStyle("-fx-background-color: #f8fafc; -fx-background-radius: 10; "
                    + "-fx-border-color: #e5e7eb; -fx-border-radius: 10; -fx-border-width: 1;");

            Label num = new Label(v[0]);
            num.setStyle("-fx-font-size: 28px; -fx-font-weight: bold; -fx-text-fill: #d1fae5;");
            num.setMinWidth(48);

            VBox text = new VBox(4);
            Label title = new Label(v[1]);
            title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");
            Label desc = new Label(v[2]);
            desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
            desc.setWrapText(true);
            text.getChildren().addAll(title, desc);
            HBox.setHgrow(text, Priority.ALWAYS);

            card.getChildren().addAll(num, text);

            ColumnConstraints cc = new ColumnConstraints();
            cc.setPercentWidth(50);
            cc.setHgrow(Priority.ALWAYS);
            if (grid.getColumnConstraints().size() <= i % 2) {
                grid.getColumnConstraints().add(cc);
            }
            grid.add(card, i % 2, i / 2);
        }

        section.getChildren().add(grid);
        return section;
    }

    /** CTA final */
    private Node buildCta() {
        VBox section = new VBox(16);
        section.setAlignment(Pos.CENTER);
        section.setPadding(new Insets(48, 32, 48, 32));
        section.setStyle("-fx-background-color: linear-gradient(to right, #16563f, #28a745);");

        Label title = new Label("Prêt à prendre soin de vous ?");
        title.setStyle("-fx-font-size: 24px; -fx-font-weight: bold; -fx-text-fill: white;");

        Label text = new Label("Utilisez le menu de l'application pour découvrir les produits ou nous contacter.");
        text.setStyle("-fx-font-size: 14px; -fx-text-fill: rgba(255,255,255,0.85);");
        text.setWrapText(true);

        HBox buttons = new HBox(16);
        buttons.setAlignment(Pos.CENTER);

        Button produits = new Button("Découvrir nos produits →");
        produits.setStyle("-fx-background-color: white; -fx-text-fill: #16563f; -fx-font-weight: bold; "
                + "-fx-background-radius: 8; -fx-padding: 10 24;");
        produits.setOnAction(e -> handleNavProduits());

        Button contact = new Button("Nous contacter");
        contact.setStyle("-fx-background-color: transparent; -fx-text-fill: white; -fx-font-weight: bold; "
                + "-fx-border-color: white; -fx-border-radius: 8; -fx-border-width: 2; -fx-padding: 10 24;");
        contact.setOnAction(e -> handleNavContact());

        buttons.getChildren().addAll(produits, contact);
        section.getChildren().addAll(title, text, buttons);
        return section;
    }

    // -------------------------------------------------------------------------
    // Helpers UI
    // -------------------------------------------------------------------------

    private VBox buildSectionHeader(String tag, String title, String subtitle) {
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER);

        Label tagLbl = new Label(tag);
        tagLbl.setStyle("-fx-background-color: #dcfce7; -fx-text-fill: #16a34a; "
                + "-fx-padding: 4 14 4 14; -fx-background-radius: 20; -fx-font-size: 12px;");

        Label titleLbl = new Label(title);
        titleLbl.setStyle("-fx-font-size: 22px; -fx-font-weight: bold; -fx-text-fill: #111827;");

        Label subtitleLbl = new Label(subtitle);
        subtitleLbl.setStyle("-fx-font-size: 13px; -fx-text-fill: #6b7280; -fx-text-alignment: center;");
        subtitleLbl.setWrapText(true);
        subtitleLbl.setMaxWidth(560);

        header.getChildren().addAll(tagLbl, titleLbl, subtitleLbl);
        return header;
    }

    private HBox buildCardRow(String[][] cards) {
        HBox row = new HBox(16);
        row.setAlignment(Pos.CENTER);
        for (String[] c : cards) {
            VBox card = new VBox(12);
            card.setPadding(new Insets(24));
            card.setStyle("-fx-background-color: white; -fx-background-radius: 12; "
                    + "-fx-border-color: #e5e7eb; -fx-border-radius: 12; -fx-border-width: 1; "
                    + "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.06), 6, 0, 0, 2);");
            HBox.setHgrow(card, Priority.ALWAYS);

            Label icon = new Label(c[0]);
            icon.setStyle("-fx-font-size: 28px;");
            Label title = new Label(c[1]);
            title.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; -fx-text-fill: #111827;");
            Label desc = new Label(c[2]);
            desc.setStyle("-fx-font-size: 12px; -fx-text-fill: #6b7280;");
            desc.setWrapText(true);

            card.getChildren().addAll(icon, title, desc);
            row.getChildren().add(card);
        }
        return row;
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

    private Node navAnchor() { return profileContainer; }

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

    @FXML private void handleNavAbout() { /* déjà sur cette page */ }
    @FXML private void handleNavGuide() { SceneNavigation.replaceScene(navAnchor(), "/fxml/GuideSante.fxml"); }
    @FXML private void handleNavProduits() { SceneNavigation.replaceScene(navAnchor(), "/fxml/FrontProduits.fxml"); }
    @FXML private void handleNavCommandes() { SceneNavigation.replaceScene(navAnchor(), "/fxml/FrontMesCommandes.fxml"); }
    @FXML private void handleNavServices() { SceneNavigation.replaceScene(navAnchor(), "/fxml/FrontServices.fxml"); }
    @FXML private void handleNavPanier() { SceneNavigation.replaceScene(navAnchor(), "/fxml/FrontCommande.fxml"); }
    @FXML private void handleNavAdresses() { SceneNavigation.replaceScene(navAnchor(), "/fxml/FrontMesAdresses.fxml"); }
    @FXML private void handleNavContact() { SceneNavigation.replaceScene(navAnchor(), "/fxml/ContactPage.fxml"); }
    @FXML private void handleNavbarSearch() { if (contentScrollPane != null) contentScrollPane.requestFocus(); }
    @FXML private void goToAccueil() { SceneNavigation.replaceScene(navAnchor(), "/fxml/Accueil.fxml"); }
    @FXML private void goToTraitement() { SceneNavigation.replaceScene(navAnchor(), "/fxml/Traitement.fxml"); }
    @FXML private void goToMesOrdonnances() { SceneNavigation.replaceScene(navAnchor(), "/fxml/MesOrdonnances.fxml"); }
    @FXML private void goToCreerOrdonnance() { SceneNavigation.replaceScene(navAnchor(), "/fxml/Ordonnance.fxml"); }
    @FXML private void goToProfil() { closeProfileDropdown(); SceneNavigation.replaceScene(navAnchor(), "/fxml/Profil.fxml"); }
    @FXML private void goToMessagesPage() { closeProfileDropdown(); SceneNavigation.replaceScene(navAnchor(), "/fxml/MessagesPage.fxml"); }
    @FXML private void goToDashboard() { closeProfileDropdown(); SceneNavigation.replaceScene(navAnchor(), "/fxml/Dashboard.fxml"); }
    @FXML private void logout() { closeProfileDropdown(); userService.logout(); SceneNavigation.replaceScene(navAnchor(), "/fxml/Login.fxml"); }
}
