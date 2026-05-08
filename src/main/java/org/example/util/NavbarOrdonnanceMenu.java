package org.example.util;

import javafx.application.Platform;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.MenuButton;
import javafx.scene.control.skin.MenuButtonSkin;
import javafx.scene.input.MouseEvent;

/**
 * Applique la classe CSS au popup du MenuButton Ordonnance et ouvre le menu au survol.
 * La fermeture est laissée au comportement natif (choix d'une entrée, clic ailleurs, Échap)
 * pour éviter les courses avec {@code hide()} qui empêchaient le déclenchement des {@code MenuItem}.
 */
public final class NavbarOrdonnanceMenu {

    private static final String POPUP_STYLE_CLASS = "nav-ordonnance-context";
    private static final String HOVER_WIRED = "navOrd.hoverWired";

    private NavbarOrdonnanceMenu() {}

    /** À appeler depuis {@code initialize()} ; fonctionne même si la scène n'est pas encore attachée. */
    public static void wirePopupStyle(Node sceneAnchor) {
        if (sceneAnchor == null) {
            return;
        }
        Runnable task = () -> {
            Scene scene = sceneAnchor.getScene();
            if (scene != null) {
                wireMenuButtonsIn(scene.getRoot());
            }
        };
        Platform.runLater(task);
        Platform.runLater(task);
    }

    private static void wireMenuButtonsIn(Node root) {
        if (root == null) {
            return;
        }
        for (Node n : root.lookupAll(".nav-ordonnance-menubutton")) {
            if (n instanceof MenuButton mb) {
                applyPopupStyleClass(mb);
                mb.skinProperty().addListener((obs, oldSkin, newSkin) -> applyPopupStyleClass(mb));
                wireHoverShowOnly(mb);
            }
        }
    }

    private static ContextMenu getPopupMenu(MenuButton mb) {
        if (!(mb.getSkin() instanceof MenuButtonSkin skin)) {
            return null;
        }
        try {
            var f = MenuButtonSkin.class.getDeclaredField("popup");
            f.setAccessible(true);
            Object o = f.get(skin);
            return o instanceof ContextMenu cm ? cm : null;
        } catch (ReflectiveOperationException e) {
            return null;
        }
    }

    private static void applyPopupStyleClass(MenuButton mb) {
        ContextMenu cm = getPopupMenu(mb);
        if (cm != null && !cm.getStyleClass().contains(POPUP_STYLE_CLASS)) {
            cm.getStyleClass().add(POPUP_STYLE_CLASS);
        }
    }

    /** Ouverture au survol ; aucun {@code hide()} programmé (évite les conflits avec les actions des lignes). */
    private static void wireHoverShowOnly(MenuButton mb) {
        if (Boolean.TRUE.equals(mb.getProperties().get(HOVER_WIRED))) {
            return;
        }
        mb.getProperties().put(HOVER_WIRED, true);

        mb.addEventFilter(MouseEvent.MOUSE_ENTERED, e -> {
            if (!mb.isShowing()) {
                mb.show();
            }
        });
    }
}
