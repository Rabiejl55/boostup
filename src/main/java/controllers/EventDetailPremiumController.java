package controllers;

import entities.GEvenement.EvenementFX;
import javafx.animation.*;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.util.Duration;
import services.maps.GeocodingService;
import utils.QrCodeUtils;
import utils.maps.LocationParser;
import utils.maps.MapHtmlBuilder;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur de la modal premium (détails événement).
 * Objectif: WOW UI, sans modifier la logique métier.
 */
public class EventDetailPremiumController implements Initializable {

    @FXML private ImageView imgEvent;
    @FXML private StackPane ledGlow;

    @FXML private Label lblTitre;
    @FXML private Label lblDate;
    @FXML private Label lblLieu;
    @FXML private Label lblCapacite;
    @FXML private Label lblCategorie;
    @FXML private Label lblPrix;
    @FXML private Label lblType;
    @FXML private Label lblDescription;

    @FXML private Label badgeStatut;
    @FXML private Label badgeNote;

    private Runnable onClose;
    private EvenementFX current;

    // animations
    private Timeline glowTimeline;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupLedGlowAnimation();
        // petite apparition interne
        if (lblTitre != null) {
            FadeTransition ft = new FadeTransition(Duration.millis(250), lblTitre);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.play();
        }
    }

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void setEvenement(EvenementFX e, Integer bestNote) {
        this.current = e;

        if (e == null) return;

        if (lblTitre != null) lblTitre.setText(nvl(e.getTitre(), "(Sans titre)"));
        if (lblType != null) lblType.setText(nvl(e.getType(), "-"));
        if (lblLieu != null) lblLieu.setText(nvl(e.getLieu(), "-"));
        if (lblCapacite != null) lblCapacite.setText(String.valueOf(e.getCapaciteMax()));
        if (lblDescription != null) lblDescription.setText(nvl(e.getDescription(), ""));

        if (lblDate != null) {
            String d = e.getDateEvenement() == null ? "-" : e.getDateEvenement().toLocalDate().toString();
            lblDate.setText(d);
        }

        // Champs non existants en DB pour l’instant -> on met des placeholders premium
        if (lblCategorie != null) lblCategorie.setText("Générale");
        if (lblPrix != null) lblPrix.setText("0 DT");

        // Badge statut (simple, cohérent)
        if (badgeStatut != null) {
            badgeStatut.setText("OUVERT");
            badgeStatut.getStyleClass().removeAll("status-open", "status-closed");
            badgeStatut.getStyleClass().add("status-open");
        }

        // badge note
        if (badgeNote != null) {
            if (bestNote == null) {
                badgeNote.setText("⭐ —");
                setBadgeNoteStyle("note-na");
            } else {
                int n = bestNote;
                if (n >= 5) {
                    badgeNote.setText("⭐ 5");
                    setBadgeNoteStyle("note-5");
                } else if (n == 4) {
                    badgeNote.setText("⭐ 4");
                    setBadgeNoteStyle("note-4");
                } else if (n == 3) {
                    badgeNote.setText("👍 3");
                    setBadgeNoteStyle("note-3");
                } else if (n == 2) {
                    badgeNote.setText("👍 2");
                    setBadgeNoteStyle("note-2");
                } else {
                    badgeNote.setText("👎 1");
                    setBadgeNoteStyle("note-1");
                }
            }
        }

        // image
        if (imgEvent != null) {
            Image img = loadImage(e.getImage());
            imgEvent.setImage(img);
        }
    }

    private void setBadgeNoteStyle(String style) {
        badgeNote.getStyleClass().removeAll("note-5", "note-4", "note-3", "note-2", "note-1", "note-na");
        badgeNote.getStyleClass().add(style);
    }

    private Image loadImage(String value) {
        String v = value == null ? "" : value.trim();

        if (v.startsWith("http://") || v.startsWith("https://")) {
            try {
                return new Image(v, true);
            } catch (Exception ignored) {
            }
        }

        if (v.startsWith("/")) {
            URL url = getClass().getResource(v);
            if (url != null) return new Image(url.toExternalForm(), true);
        }

        URL fb = getClass().getResource("/images/logo.png");
        return fb == null ? null : new Image(fb.toExternalForm(), true);
    }

    private void setupLedGlowAnimation() {
        if (ledGlow == null) return;

        DropShadow ds = new DropShadow(40, Color.color(0.35, 0.25, 0.95, 0.75));
        ds.setSpread(0.25);
        ds.setOffsetX(0);
        ds.setOffsetY(0);
        ledGlow.setEffect(ds);

        glowTimeline = new Timeline(
                new KeyFrame(Duration.ZERO,
                        new KeyValue(ds.radiusProperty(), 28, Interpolator.EASE_BOTH),
                        new KeyValue(ds.spreadProperty(), 0.18, Interpolator.EASE_BOTH),
                        new KeyValue(ds.colorProperty(), Color.web("#3d8bfd", 0.62), Interpolator.EASE_BOTH)
                ),
                new KeyFrame(Duration.millis(1200),
                        new KeyValue(ds.radiusProperty(), 48, Interpolator.EASE_BOTH),
                        new KeyValue(ds.spreadProperty(), 0.32, Interpolator.EASE_BOTH),
                        new KeyValue(ds.colorProperty(), Color.web("#6f42c1", 0.68), Interpolator.EASE_BOTH)
                ),
                new KeyFrame(Duration.millis(2400),
                        new KeyValue(ds.radiusProperty(), 28, Interpolator.EASE_BOTH),
                        new KeyValue(ds.spreadProperty(), 0.18, Interpolator.EASE_BOTH),
                        new KeyValue(ds.colorProperty(), Color.web("#3d8bfd", 0.62), Interpolator.EASE_BOTH)
                )
        );
        glowTimeline.setCycleCount(Animation.INDEFINITE);
        glowTimeline.play();
    }

    private static String nvl(String s, String fallback) {
        return (s == null || s.trim().isEmpty()) ? fallback : s;
    }

    @FXML
    private void handleClose() {
        if (glowTimeline != null) glowTimeline.stop();
        if (onClose != null) onClose.run();
    }

    @FXML
    private void handleInscription() {
        if (current == null) {
            showInfo("Inscription", "Événement introuvable.");
            return;
        }

        try {
            // Charger la modal FXML
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ParticipationFrontModal.fxml"));
            Node modal = loader.load();

            ParticipationFrontModalController controller = loader.getController();
            controller.setEvenement(current);

            // On ajoute par-dessus la modal premium (même scène)
            StackPane root = (StackPane) lblTitre.getScene().getRoot();
            StackPane overlay = buildSimpleOverlay(root, modal);

            controller.setOnClose(() -> {
                root.getChildren().remove(overlay);
            });

            root.getChildren().add(overlay);
            playOpenOverlayAnimation(overlay, modal);

        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Inscription", "Impossible d'ouvrir le formulaire: " + e.getMessage());
        }
    }

    private StackPane buildSimpleOverlay(StackPane root, Node content) {
        StackPane backdrop = new StackPane();
        backdrop.setStyle("-fx-background-color: rgba(0,0,0,0.55);");
        backdrop.setOnMouseClicked(ev -> {
            // click dehors: ferme (équivalent annuler)
            root.getChildren().remove(backdrop.getParent());
        });

        StackPane container = new StackPane();
        container.setPickOnBounds(false);
        container.getChildren().addAll(backdrop, content);

        StackPane.setAlignment(content, javafx.geometry.Pos.CENTER);
        return container;
    }

    private void playOpenOverlayAnimation(Node overlay, Node modal) {
        overlay.setOpacity(0);
        modal.setOpacity(0);
        modal.setTranslateY(18);
        modal.setScaleX(0.985);
        modal.setScaleY(0.985);

        FadeTransition ft = new FadeTransition(Duration.millis(220), overlay);
        ft.setToValue(1);

        FadeTransition mft = new FadeTransition(Duration.millis(260), modal);
        mft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(320), modal);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition st = new ScaleTransition(Duration.millis(320), modal);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_BOTH);

        new ParallelTransition(ft, mft, tt, st).play();
    }

    @FXML
    private void handleQr() {
        if (current == null) {
            showInfo("QR Code", "Événement introuvable.");
            return;
        }

        try {
            String payload = buildEventTextPayload(current);
            var qrImage = QrCodeUtils.toQrImage(payload, 320);

            FXMLLoader loader = new FXMLLoader(getClass().getResource("/QrCodeModal.fxml"));
            Node modal = loader.load();
            QrCodeModalController controller = loader.getController();
            controller.setQr(qrImage);

            StackPane root = (StackPane) lblTitre.getScene().getRoot();
            StackPane overlay = buildSimpleOverlay(root, modal);
            controller.setOnClose(() -> root.getChildren().remove(overlay));

            root.getChildren().add(overlay);
            playOpenOverlayAnimation(overlay, modal);

        } catch (Exception e) {
            e.printStackTrace();
            showInfo("QR Code", "Impossible de générer le QR: " + e.getMessage());
        }
    }

    private String buildEventTextPayload(EvenementFX e) {
        StringBuilder sb = new StringBuilder();
        sb.append("BOOSTUP - FICHE ÉVÉNEMENT\n");
        sb.append("========================\n");
        sb.append("Titre: ").append(nvl(e.getTitre(), "-")).append("\n");
        sb.append("Type: ").append(nvl(e.getType(), "-")).append("\n");
        sb.append("Date: ").append(e.getDateEvenement() == null ? "-" : e.getDateEvenement().toString()).append("\n");
        sb.append("Lieu: ").append(nvl(e.getLieu(), "-")).append("\n");
        sb.append("Capacité max: ").append(e.getCapaciteMax()).append("\n\n");
        sb.append("Description:\n").append(nvl(e.getDescription(), "")).append("\n");
        return sb.toString();
    }

    @FXML
    private void handleGenererAfficher() {
        showInfo("Générer & Afficher", "Fonctionnalité à connecter selon ton besoin (PDF, QR, etc.)." );
    }

    @FXML
    private void handleMaps() {
        if (current == null) {
            showInfo("Maps", "Événement introuvable.");
            return;
        }

        String lieu = current.getLieu();
        var parsed = LocationParser.parse(lieu);

        switch (parsed.kind) {
            case EMPTY -> showInfo("Maps", "Aucun lieu disponible pour cet événement.");
            case INVALID -> showInfo("Maps", "Lieu invalide. Veuillez saisir une adresse, des coordonnées (lat,lon) ou une URL Maps.");
            case URL -> {
                // URL directe -> on affiche sans géocodage
                openMapModal(parsed.url, parsed.url, buildSubtitle(lieu));
            }
            case LAT_LON -> openLatLon(parsed.lat, parsed.lon, buildSubtitle(lieu));
            case ADDRESS -> {
                // non bloquant
                showInfo("Maps", "Recherche de l’adresse... (cela peut prendre 1–2s)");
                GeocodingService geo = new GeocodingService();
                geo.geocodeAsync(parsed.address).thenAccept(opt -> Platform.runLater(() -> {
                    if (opt.isEmpty()) {
                        showInfo("Maps", "Localisation introuvable pour: " + parsed.address);
                        return;
                    }
                    var r = opt.get();
                    String sub = (r.displayName == null || r.displayName.isBlank()) ? parsed.address : r.displayName;
                    openLatLon(r.lat, r.lon, sub);
                }));
            }
        }
    }

    private String buildSubtitle(String lieu) {
        String titre = nvl(current == null ? null : current.getTitre(), "");
        String date = (current == null || current.getDateEvenement() == null) ? "-" : current.getDateEvenement().toString();
        String l = lieu == null ? "" : lieu.trim();
        return titre + " • " + date + (l.isEmpty() ? "" : (" • " + l));
    }

    private void openLatLon(double lat, double lon, String subtitle) {
        String html = MapHtmlBuilder.buildLeafletHtml(lat, lon,
                nvl(current == null ? null : current.getTitre(), "Événement"), subtitle);
        String dataUrl = MapHtmlBuilder.toDataUrl(html);
        String external = "https://www.openstreetmap.org/?mlat=" + lat + "&mlon=" + lon + "#map=16/" + lat + "/" + lon;
        openMapModal(dataUrl, external, subtitle);
    }

    private void openMapModal(String urlToLoad, String externalUrl, String subtitle) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/MapModal.fxml"));
            Node modal = loader.load();
            MapModalController controller = loader.getController();
            controller.setSubtitle(subtitle);
            controller.loadUrl(urlToLoad, externalUrl);

            StackPane root = (StackPane) lblTitre.getScene().getRoot();
            StackPane overlay = buildSimpleOverlay(root, modal);
            controller.setOnClose(() -> root.getChildren().remove(overlay));

            root.getChildren().add(overlay);
            playOpenOverlayAnimation(overlay, modal);

        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Maps", "Impossible d'ouvrir la carte: " + e.getMessage());
        }
    }

    private void showInfo(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }

    @FXML
    private void handleFeedback() {
        if (current == null) {
            showInfo("Feedback", "Événement introuvable.");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FeedbackFrontModal.fxml"));
            Node modal = loader.load();

            FeedbackFrontModalController controller = loader.getController();
            controller.setEvenement(current);

            StackPane root = (StackPane) lblTitre.getScene().getRoot();
            StackPane overlay = buildSimpleOverlay(root, modal);
            controller.setOnClose(() -> root.getChildren().remove(overlay));

            root.getChildren().add(overlay);
            playOpenOverlayAnimation(overlay, modal);

        } catch (Exception e) {
            e.printStackTrace();
            showInfo("Feedback", "Impossible d'ouvrir le formulaire: " + e.getMessage());
        }
    }
}
