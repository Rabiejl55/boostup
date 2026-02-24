package controllers;

import javafx.application.Platform;
import javafx.embed.swing.SwingFXUtils;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import services.WeatherService;
import services.WeatherService.WeatherData;
import utils.maps.TileMapBuilder;

import java.awt.*;
import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ResourceBundle;

public class MapModalController implements Initializable {

    @FXML private ImageView mapImage;
    @FXML private ProgressIndicator progress;
    @FXML private Label lblSubtitle;
    @FXML private Button btnOpenExternal;
    @FXML private Button btnMeteo;

    private Runnable onClose;
    private String externalUrl;
    private String currentLieu; // Pour la météo

    private final HttpClient http = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void setSubtitle(String subtitle) {
        if (lblSubtitle != null) {
            lblSubtitle.setText(subtitle == null ? "" : subtitle);
        }
    }

    public void loadUrl(String url, String externalUrl) {
        this.externalUrl = externalUrl;

        if (btnOpenExternal != null) {
            btnOpenExternal.setDisable(externalUrl == null || externalUrl.isBlank());
        }

        if (mapImage == null) return;

        // 1) Si url est une image http(s) -> on essaye directement
        if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
            fetchAndShow(url);
            return;
        }

        // 2) Sinon on tente via externalUrl (format OSM mlat/mlon)
        double[] latLon = tryExtractLatLonFromExternal(externalUrl);
        if (latLon == null) {
            setError("⚠️ Coordonnées introuvables pour afficher la carte. Utilise 'Ouvrir dans Maps'.");
            return;
        }

        String imgUrl = buildStaticMapUrl(latLon[0], latLon[1]);
        fetchAndShow(imgUrl);
    }

    private void fetchAndShow(String imgUrl) {
        if (progress != null) progress.setVisible(true);

        HttpRequest req = HttpRequest.newBuilder(URI.create(imgUrl))
                .timeout(Duration.ofSeconds(12))
                .header("User-Agent", "BoostUp/1.0 (JavaFX)")
                .GET()
                .build();

        http.sendAsync(req, HttpResponse.BodyHandlers.ofByteArray())
                .thenAccept(resp -> Platform.runLater(() -> {
                    if (progress != null) progress.setVisible(false);
                    if (resp.statusCode() < 200 || resp.statusCode() >= 300) {
                        setError("⚠️ staticmap bloqué (HTTP " + resp.statusCode() + "). Chargement via tuiles OSM...");
                        applyTileFallback();
                        return;
                    }
                    byte[] bytes = resp.body();
                    if (bytes == null || bytes.length == 0) {
                        setError("⚠️ Réponse carte vide. Chargement via tuiles OSM...");
                        applyTileFallback();
                        return;
                    }
                    Image img = new Image(new ByteArrayInputStream(bytes));
                    if (img.isError() || img.getWidth() <= 1) {
                        setError("⚠️ Image carte invalide. Chargement via tuiles OSM...");
                        applyTileFallback();
                        return;
                    }
                    mapImage.setImage(img);
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (progress != null) progress.setVisible(false);
                        String msg = ex.getCause() == null ? ex.getMessage() : ex.getCause().getClass().getSimpleName();
                        setError("⚠️ staticmap bloqué (" + msg + "). Chargement via tuiles OSM...");
                        applyTileFallback();
                    });
                    return null;
                });
    }

    /**
     * Fallback utilisant les tuiles OSM (tile.openstreetmap.org) qui sont accessibles
     * même quand staticmap.openstreetmap.de est bloqué. Assemble plusieurs tuiles
     * pour créer une vraie carte intégrée.
     */
    private void applyTileFallback() {
        if (mapImage == null) return;

        double[] latLon = tryExtractLatLonFromExternal(externalUrl);
        if (latLon == null) {
            applyOfflineFallback();
            return;
        }

        if (progress != null) progress.setVisible(true);

        TileMapBuilder.buildMapImage(latLon[0], latLon[1], 15)
                .thenAccept(img -> Platform.runLater(() -> {
                    if (progress != null) progress.setVisible(false);
                    if (img != null && !img.isError()) {
                        mapImage.setImage(img);
                    } else {
                        setError("⚠️ Tuiles OSM échouées. Fallback 100% offline appliqué.");
                        applyOfflineFallback();
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (progress != null) progress.setVisible(false);
                        setError("⚠️ Tuiles OSM échouées: " + ex.getMessage() + "\n→ Fallback 100% offline appliqué.");
                        applyOfflineFallback();
                    });
                    return null;
                });
    }

    /**
     * Fallback 100% offline (aucune HTTP): génère une "mini-carte" stylisée
     * avec une grille + point central. Ça garantit une carte intégrée même si
     * le réseau bloque OpenStreetMap/staticmap.
     */
    private void applyOfflineFallback() {
        if (mapImage == null) return;

        int w = 980;
        int h = 520;
        Canvas canvas = new Canvas(w, h);
        GraphicsContext g = canvas.getGraphicsContext2D();

        // fond
        g.setFill(Color.web("#0b1020"));
        g.fillRect(0, 0, w, h);

        // dégradé léger
        for (int i = 0; i < h; i += 2) {
            double a = 0.20 * (1.0 - (double) i / h);
            g.setStroke(Color.color(0.43, 0.26, 0.76, a));
            g.strokeLine(0, i, w, i);
        }

        // grille
        g.setStroke(Color.color(1, 1, 1, 0.07));
        for (int x = 0; x < w; x += 50) g.strokeLine(x, 0, x, h);
        for (int y = 0; y < h; y += 50) g.strokeLine(0, y, w, y);

        // "route" stylisée
        g.setStroke(Color.color(0.24, 0.55, 0.99, 0.35));
        g.setLineWidth(6);
        g.beginPath();
        g.moveTo(w * 0.10, h * 0.70);
        g.bezierCurveTo(w * 0.35, h * 0.40, w * 0.55, h * 0.80, w * 0.90, h * 0.35);
        g.stroke();

        // ping
        double cx = w / 2.0;
        double cy = h / 2.0;
        g.setFill(Color.web("#e63956"));
        g.fillOval(cx - 10, cy - 10, 20, 20);
        g.setStroke(Color.color(1, 1, 1, 0.85));
        g.setLineWidth(2);
        g.strokeOval(cx - 15, cy - 15, 30, 30);

        // texte
        g.setFill(Color.color(1, 1, 1, 0.85));
        g.fillText("Carte hors-ligne (réseau bloqué)", 18, 28);
        g.setFill(Color.color(1, 1, 1, 0.6));
        g.fillText("Astuce: clique sur 'Ouvrir dans Maps' pour voir la carte réelle.", 18, 48);

        WritableImage snapshot = new WritableImage(w, h);
        SnapshotParameters sp = new SnapshotParameters();
        sp.setFill(Color.TRANSPARENT);
        canvas.snapshot(sp, snapshot);
        mapImage.setImage(snapshot);
    }

    private void setError(String msg) {
        if (lblSubtitle != null) {
            lblSubtitle.setText((lblSubtitle.getText() == null ? "" : lblSubtitle.getText()) + "\n\n" + msg);
        }
    }

    private String buildStaticMapUrl(double lat, double lon) {
        int zoom = 15;
        int w = 980;
        int h = 520;
        return "https://staticmap.openstreetmap.de/staticmap.php?center=" + lat + "," + lon
                + "&zoom=" + zoom
                + "&size=" + w + "x" + h
                + "&markers=" + lat + "," + lon + ",red-pushpin";
    }

    private double[] tryExtractLatLonFromExternal(String externalUrl) {
        if (externalUrl == null) return null;
        try {
            URI uri = URI.create(externalUrl);
            String q = uri.getQuery();
            if (q == null) return null;
            Double lat = null;
            Double lon = null;
            for (String part : q.split("&")) {
                String[] kv = part.split("=", 2);
                if (kv.length != 2) continue;
                if (kv[0].equalsIgnoreCase("mlat")) lat = Double.parseDouble(kv[1]);
                if (kv[0].equalsIgnoreCase("mlon")) lon = Double.parseDouble(kv[1]);
            }
            if (lat == null || lon == null) return null;
            return new double[]{lat, lon};
        } catch (Exception e) {
            return null;
        }
    }

    @FXML
    private void handleClose() {
        if (onClose != null) onClose.run();
    }

    @FXML
    private void handleOpenExternal() {
        if (externalUrl == null || externalUrl.isBlank()) return;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(externalUrl));
            }
        } catch (Exception ignored) {
        }
    }

    /**
     * 🌤️ NOUVELLE MÉTHODE : Affiche la météo du lieu
     */
    @FXML
    private void handleMeteo() {
        if (currentLieu == null || currentLieu.trim().isEmpty()) {
            showMeteoInfo("Météo", "⚠️ Lieu non défini");
            return;
        }

        // Récupération de la météo
        WeatherData meteo = WeatherService.getMeteo(currentLieu);

        if (meteo.isSuccess()) {
            String emoji = WeatherService.getEmojiMeteo(meteo.getIcon());
            String message = String.format(
                "%s MÉTÉO DE %s\n\n" +
                "%s Température : %s\n" +
                "☁️ Conditions  : %s\n\n" +
                "💡 Conseil : %s",
                emoji,
                currentLieu.toUpperCase(),
                emoji,
                meteo.getTemperature(),
                meteo.getDescription(),
                getMeteoAdvice(meteo.getIcon())
            );
            showMeteoInfo("Météo - " + currentLieu, message);
        } else {
            showMeteoInfo("Météo", "❌ " + meteo.getDescription() + "\n\nVille recherchée : " + currentLieu);
        }
    }

    /**
     * 💡 Donne un conseil selon la météo
     */
    private String getMeteoAdvice(String icon) {
        if (icon == null) return "Profitez de votre événement !";

        String code = icon.substring(0, 2);
        switch (code) {
            case "01": return "Parfait pour un événement en extérieur ! ☀️";
            case "02": return "Beau temps prévu, idéal ! 🌤️";
            case "03":
            case "04": return "Nuageux mais aucun souci ! ⛅";
            case "09":
            case "10": return "N'oubliez pas votre parapluie ! ☂️";
            case "11": return "Attention aux orages, prévoyez un plan B ! ⛈️";
            case "13": return "Il va neiger, habillez-vous chaudement ! ❄️";
            case "50": return "Brouillard possible, soyez prudents ! 🌫️";
            default: return "Profitez de votre événement !";
        }
    }

    /**
     * Définit le lieu pour la météo
     */
    public void setLieu(String lieu) {
        this.currentLieu = lieu;
    }

    private void showMeteoInfo(String title, String message) {
        Platform.runLater(() -> {
            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.INFORMATION);
            alert.setTitle(title);
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.showAndWait();
        });
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // nothing
    }
}
