package controllers;

import javafx.animation.ParallelTransition;
import javafx.animation.RotateTransition;
import javafx.animation.ScaleTransition;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.ToggleButton;
import javafx.scene.layout.BorderPane;
import javafx.util.Duration;

/**
 * Utilitaire centralisé pour la gestion du Dark/Light Mode.
 * Gère à la fois les classes CSS et les overrides inline
 * pour les éléments avec des styles FXML inline.
 */
public class ThemeHelper {

    private static final String DARK_MODE_CLASS = "dark-mode";
    private static final String ICON_DARK = "☀️";
    private static final String ICON_LIGHT = "🌙";

    // ── Dark mode color palette (GitHub-style) ──
    private static final String DK_BG_DEEP = "#0d1117";
    private static final String DK_BG_CARD = "#161b22";
    private static final String DK_TEXT = "#e6edf3";
    private static final String DK_TEXT_MUTED = "#8b949e";
    private static final String DK_BORDER = "#30363d";

    /**
     * Applique le thème actuel à la scène et met à jour le toggle.
     */
    public static void applyTheme(Scene scene, ToggleButton themeToggle) {
        if (scene == null) return;

        boolean dark = SessionManager.isDarkMode();

        if (dark) {
            if (!scene.getRoot().getStyleClass().contains(DARK_MODE_CLASS)) {
                scene.getRoot().getStyleClass().add(DARK_MODE_CLASS);
            }
        } else {
            scene.getRoot().getStyleClass().remove(DARK_MODE_CLASS);
        }

        // Apply inline style overrides for nodes with hardcoded FXML styles
        applyInlineOverrides(scene.getRoot(), dark);

        if (themeToggle != null) {
            themeToggle.setSelected(dark);
            themeToggle.setText(dark ? ICON_DARK : ICON_LIGHT);
        }
    }

    /**
     * Inverse le thème, persiste l'état, et applique avec animation.
     */
    public static void toggleTheme(ToggleButton themeToggle) {
        if (themeToggle == null) return;

        boolean nowDark = themeToggle.isSelected();
        SessionManager.setDarkMode(nowDark);

        Scene scene = themeToggle.getScene();
        if (scene != null) {
            applyTheme(scene, themeToggle);
        }

        animateToggle(themeToggle);
    }

    /**
     * Applique le thème sur un root directement (utilisé par NavigationHelper).
     */
    public static void applyThemeToRoot(Parent root) {
        if (root == null) return;
        boolean dark = SessionManager.isDarkMode();
        if (dark) {
            if (!root.getStyleClass().contains(DARK_MODE_CLASS)) {
                root.getStyleClass().add(DARK_MODE_CLASS);
            }
        } else {
            root.getStyleClass().remove(DARK_MODE_CLASS);
        }
        applyInlineOverrides(root, dark);
    }

    /**
     * Recursively override inline styles on nodes that have hardcoded
     * light-mode backgrounds from FXML.
     */
    private static void applyInlineOverrides(Parent root, boolean dark) {
        if (root == null) return;

        // Handle the root BorderPane
        if (root instanceof BorderPane bp) {
            String style = bp.getStyle();
            if (style != null) {
                if (dark) {
                    if (style.contains("#f0f2f5")) {
                        bp.setStyle(style.replace("#f0f2f5", DK_BG_DEEP));
                    }
                } else {
                    if (style.contains(DK_BG_DEEP)) {
                        bp.setStyle(style.replace(DK_BG_DEEP, "#f0f2f5"));
                    }
                }
            }
            if (bp.getCenter() != null) {
                overrideNodeStyles(bp.getCenter(), dark);
            }
        }

        for (Node child : root.getChildrenUnmodifiable()) {
            overrideNodeStyles(child, dark);
        }
    }

    /**
     * Override inline styles on a single node and its children recursively.
     */
    private static void overrideNodeStyles(Node node, boolean dark) {
        if (node == null) return;

        String style = node.getStyle();
        if (style != null && !style.isEmpty()) {
            // Skip the sidebar — it already has a dark gradient
            if (style.contains("linear-gradient(to bottom right, #1b2a4a")) {
                return;
            }

            if (dark) {
                style = applyDarkReplacements(style);
            } else {
                style = applyLightReplacements(style);
            }
            node.setStyle(style);
        }

        // Recurse into children
        if (node instanceof ScrollPane sp) {
            String spStyle = sp.getStyle();
            if (spStyle != null) {
                if (dark) {
                    spStyle = spStyle.replace("-fx-background: white", "-fx-background: " + DK_BG_DEEP);
                    spStyle = spStyle.replace("-fx-background-color: white", "-fx-background-color: " + DK_BG_DEEP);
                    spStyle = spStyle.replace("-fx-background: #f0f2f5", "-fx-background: " + DK_BG_DEEP);
                } else {
                    spStyle = spStyle.replace("-fx-background: " + DK_BG_DEEP, "-fx-background: #f0f2f5");
                    spStyle = spStyle.replace("-fx-background-color: " + DK_BG_DEEP, "-fx-background-color: transparent");
                }
                sp.setStyle(spStyle);
            }
            if (sp.getContent() instanceof Parent p) {
                for (Node child : p.getChildrenUnmodifiable()) {
                    overrideNodeStyles(child, dark);
                }
            }
        } else if (node instanceof Parent p) {
            for (Node child : p.getChildrenUnmodifiable()) {
                overrideNodeStyles(child, dark);
            }
        }
    }

    private static String applyDarkReplacements(String style) {
        // Backgrounds
        style = style.replace("-fx-background-color: white", "-fx-background-color: " + DK_BG_CARD);
        style = style.replace("-fx-background-color:white", "-fx-background-color: " + DK_BG_CARD);
        style = style.replace("-fx-background-color: #f0f2f5", "-fx-background-color: " + DK_BG_DEEP);
        style = style.replace("-fx-background: #f0f2f5", "-fx-background: " + DK_BG_DEEP);
        style = style.replace("-fx-background-color: #e9ecef", "-fx-background-color: #21262d");
        style = style.replace("-fx-background-color: #f8f9fa", "-fx-background-color: #161b22");

        // Text fills
        style = style.replace("-fx-text-fill: #1a1a2e", "-fx-text-fill: " + DK_TEXT);
        style = style.replace("-fx-text-fill: #1b2a4a", "-fx-text-fill: " + DK_TEXT);
        style = style.replace("-fx-text-fill: #344054", "-fx-text-fill: #c9d1d9");
        style = style.replace("-fx-text-fill: #495057", "-fx-text-fill: #c9d1d9");
        style = style.replace("-fx-text-fill: #6c757d", "-fx-text-fill: " + DK_TEXT_MUTED);
        style = style.replace("-fx-text-fill: #adb5bd", "-fx-text-fill: #6e7681");
        style = style.replace("-fx-text-fill: #8e99a4", "-fx-text-fill: #6e7681");

        // Borders
        style = style.replace("-fx-border-color: #f0f2f5", "-fx-border-color: " + DK_BORDER);
        style = style.replace("-fx-border-color: #f8f9fa", "-fx-border-color: #21262d");
        style = style.replace("-fx-border-color: #dee2e6", "-fx-border-color: " + DK_BORDER);

        // Shadows
        style = style.replace("rgba(0,0,0,0.06)", "rgba(0,0,0,0.4)");
        style = style.replace("rgba(0,0,0,0.08)", "rgba(0,0,0,0.45)");
        style = style.replace("rgba(0,0,0,0.04)", "rgba(0,0,0,0.35)");

        // Icon backgrounds
        style = style.replace("rgba(27,42,74,0.1)", "rgba(88,166,255,0.12)");
        style = style.replace("rgba(230,57,86,0.1)", "rgba(248,81,73,0.12)");
        style = style.replace("rgba(45,27,78,0.1)", "rgba(188,140,255,0.12)");
        style = style.replace("rgba(230,57,86,0.08)", "rgba(248,81,73,0.08)");

        return style;
    }

    private static String applyLightReplacements(String style) {
        // Reverse: dark → light
        style = style.replace("-fx-background-color: " + DK_BG_CARD, "-fx-background-color: white");
        style = style.replace("-fx-background-color: " + DK_BG_DEEP, "-fx-background-color: #f0f2f5");
        style = style.replace("-fx-background: " + DK_BG_DEEP, "-fx-background: #f0f2f5");
        style = style.replace("-fx-background-color: #21262d", "-fx-background-color: #e9ecef");
        style = style.replace("-fx-background-color: #161b22", "-fx-background-color: #f8f9fa");

        style = style.replace("-fx-text-fill: " + DK_TEXT, "-fx-text-fill: #1a1a2e");
        style = style.replace("-fx-text-fill: #c9d1d9", "-fx-text-fill: #344054");
        style = style.replace("-fx-text-fill: " + DK_TEXT_MUTED, "-fx-text-fill: #6c757d");
        style = style.replace("-fx-text-fill: #6e7681", "-fx-text-fill: #adb5bd");

        style = style.replace("-fx-border-color: " + DK_BORDER, "-fx-border-color: #f0f2f5");
        style = style.replace("-fx-border-color: #21262d", "-fx-border-color: #f8f9fa");

        style = style.replace("rgba(0,0,0,0.4)", "rgba(0,0,0,0.06)");
        style = style.replace("rgba(0,0,0,0.45)", "rgba(0,0,0,0.08)");
        style = style.replace("rgba(0,0,0,0.35)", "rgba(0,0,0,0.04)");

        style = style.replace("rgba(88,166,255,0.12)", "rgba(27,42,74,0.1)");
        style = style.replace("rgba(248,81,73,0.12)", "rgba(230,57,86,0.1)");
        style = style.replace("rgba(188,140,255,0.12)", "rgba(45,27,78,0.1)");
        style = style.replace("rgba(248,81,73,0.08)", "rgba(230,57,86,0.08)");

        return style;
    }

    /**
     * Smooth rotation + scale animation on the toggle button.
     */
    private static void animateToggle(ToggleButton toggle) {
        RotateTransition rotate = new RotateTransition(Duration.millis(400), toggle);
        rotate.setByAngle(360);

        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(200), toggle);
        scaleUp.setToX(1.3);
        scaleUp.setToY(1.3);

        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), toggle);
        scaleDown.setToX(1.0);
        scaleDown.setToY(1.0);
        scaleDown.setDelay(Duration.millis(200));

        ParallelTransition parallel = new ParallelTransition(rotate, scaleUp);
        parallel.setOnFinished(e -> scaleDown.play());
        parallel.play();
    }
}
