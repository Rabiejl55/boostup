package services.CandidatureService;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import entities.GCandidature.Candidature;
import javafx.scene.image.Image;
import javafx.scene.image.PixelWriter;
import javafx.scene.image.WritableImage;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * QRCodeService — Version statique, sans serveur HTTP.
 *
 * Le QR Code contient une URL vers une page HTML hébergée sur Netlify.
 * Les données (statut, nom, score...) sont encodées dans les paramètres URL.
 *
 * ── ÉTAPE DE DÉPLOIEMENT (une seule fois) ──────────────────────
 * 1. Aller sur https://app.netlify.com/drop
 * 2. Glisser-déposer le fichier index.html fourni
 * 3. Copier l'URL donnée (ex: https://amazing-name-123.netlify.app)
 * 4. Remplacer BASE_URL ci-dessous par cette URL
 * ────────────────────────────────────────────────────────────────
 *
 * Résultat : QR Code fonctionne PARTOUT (WiFi, 4G, partout dans le monde)
 */
public class QRCodeService {

    private static final int QR_SIZE = 350;

    // ── ⚠️ REMPLACEZ CETTE URL APRÈS DÉPLOIEMENT SUR NETLIFY ─────
    private static final String BASE_URL = "https://gestion-candidatures-6763d0.netlify.app";
    // ──────────────────────────────────────────────────────────────

    // ─── GÉNÉRATION QR CODE ───────────────────────────────────────

    /**
     * Génère un QR Code JavaFX Image pointant vers la page Netlify.
     * L'URL contient toutes les données encodées — aucun serveur requis.
     */
    public Image genererQRCode(Candidature candidature) throws WriterException {
        String url = buildUrl(candidature);
        return genererQRCodeDepuisTexte(url);
    }

    /**
     * Construit l'URL complète avec les paramètres encodés.
     * Ex : https://mon-site.netlify.app/?nom=TechVision&statut=VALIDEE&score=8.5
     */
    public String buildUrl(Candidature candidature) {
        StringBuilder sb = new StringBuilder(BASE_URL).append("/?");

        sb.append("nom=")
                .append(encode(nvl(candidature.getNomCandidature())));

        sb.append("&startup=")
                .append(encode(nvl(candidature.getNomStartup())));

        sb.append("&statut=")
                .append(encode(nvl(candidature.getStatut())));

        sb.append("&score=")
                .append(encode(candidature.getScore() != null
                        ? String.format("%.2f", candidature.getScore())
                        : "null"));

        sb.append("&date=")
                .append(encode(candidature.getDateDepot() != null
                        ? candidature.getDateDepot().toString()
                        : ""));

        sb.append("&commentaire=")
                .append(encode(nvl(candidature.getCommentaire())));

        // Timestamp de génération
        sb.append("&ts=")
                .append(encode(java.time.LocalDateTime.now()
                        .format(java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))));

        return sb.toString();
    }

    // ─── GÉNÉRATION IMAGE QR ──────────────────────────────────────

    private Image genererQRCodeDepuisTexte(String texte) throws WriterException {
        Map<EncodeHintType, Object> hints = new HashMap<>();
        // Niveau M (moins dense que H) car l'URL peut être longue
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.M);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 2);

        QRCodeWriter writer = new QRCodeWriter();
        BitMatrix bitMatrix = writer.encode(texte, BarcodeFormat.QR_CODE,
                QR_SIZE, QR_SIZE, hints);

        WritableImage image = new WritableImage(QR_SIZE, QR_SIZE);
        PixelWriter pw = image.getPixelWriter();

        for (int y = 0; y < QR_SIZE; y++) {
            for (int x = 0; x < QR_SIZE; x++) {
                javafx.scene.paint.Color color = bitMatrix.get(x, y)
                        ? javafx.scene.paint.Color.web("#0a0f1e")
                        : javafx.scene.paint.Color.WHITE;
                pw.setColor(x, y, color);
            }
        }
        return image;
    }

    // ─── HELPERS ──────────────────────────────────────────────────

    private String encode(String val) {
        return URLEncoder.encode(val, StandardCharsets.UTF_8);
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }
}