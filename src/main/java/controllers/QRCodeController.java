package controllers;

import entities.GCandidature.Candidature;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.CandidatureService.QRCodeService;

import java.awt.*;
import java.net.URI;

/**
 * QRCodeController — Version Netlify (sans serveur local).
 *
 * Le QR Code pointe vers une URL Netlify statique avec les données
 * encodées en paramètres. Fonctionne sur n'importe quel réseau (WiFi, 4G…).
 *
 * ⚠️ Plus besoin de LocalHttpServer ici.
 */
public class QRCodeController {

    @FXML private ImageView imgQRCode;
    @FXML private Label     lblNomCandidature;
    @FXML private Label     lblStartup;
    @FXML private Label     lblStatut;
    @FXML private Label     lblStatutDesc;
    @FXML private Label     lblCheminFichier;   // peut être null si retiré du FXML
    @FXML private Button    btnOuvrirPage;
    @FXML private Button    btnCopierLien;
    @FXML private Button    btnFermer;
    @FXML private VBox      paneLoading;
    @FXML private VBox      paneContent;
    @FXML private VBox      paneErreurServeur;
    @FXML private Label     lblErreurServeur;

    private final QRCodeService qrService = new QRCodeService();
    private Candidature candidature;
    private String urlPublique;

    public void setCandidature(Candidature c) {
        this.candidature = c;
        afficherInfos();
        genererQR();
    }

    // ─── INFO CANDIDATURE ─────────────────────────────────────────

    private void afficherInfos() {
        lblNomCandidature.setText(nvl(candidature.getNomCandidature()));
        lblStartup.setText("🏢  " + nvl(candidature.getNomStartup()));
        String statut = candidature.getStatut() != null ? candidature.getStatut() : "EN_ATTENTE";
        switch (statut) {
            case "VALIDEE" -> {
                lblStatut.setText("✅  Validée");
                lblStatut.setStyle("-fx-text-fill:#0d7a57;-fx-font-weight:800;-fx-font-size:13px;");
                lblStatutDesc.setText("Candidature acceptée par le jury");
            }
            case "REFUSEE" -> {
                lblStatut.setText("❌  Refusée");
                lblStatut.setStyle("-fx-text-fill:#b91c1c;-fx-font-weight:800;-fx-font-size:13px;");
                lblStatutDesc.setText("Candidature non retenue");
            }
            default -> {
                lblStatut.setText("⏳  En attente");
                lblStatut.setStyle("-fx-text-fill:#f59e0b;-fx-font-weight:800;-fx-font-size:13px;");
                lblStatutDesc.setText("Dossier en cours d'examen");
            }
        }
    }

    // ─── GÉNÉRATION QR (Netlify — aucun serveur requis) ───────────

    private void genererQR() {
        show(paneLoading, true);
        show(paneContent, false);
        show(paneErreurServeur, false);

        Thread t = new Thread(() -> {
            try {
                // Construit l'URL Netlify avec les données encodées
                String url = qrService.buildUrl(candidature);
                this.urlPublique = url;

                // Génère le QR Code pointant vers cette URL
                javafx.scene.image.Image qrImage = qrService.genererQRCode(candidature);

                Platform.runLater(() -> {
                    imgQRCode.setImage(qrImage);
                    imgQRCode.setFitWidth(320);
                    imgQRCode.setFitHeight(320);
                    imgQRCode.setPreserveRatio(true);

                    if (lblCheminFichier != null) {
                        lblCheminFichier.setText(url);
                        lblCheminFichier.setTooltip(new Tooltip(url));
                    }

                    show(paneLoading, false);
                    show(paneContent, true);
                });

            } catch (Throwable e) {
                e.printStackTrace();
                String msg = e.getClass().getSimpleName() + " : "
                        + (e.getMessage() != null ? e.getMessage() : "erreur inconnue");

                Platform.runLater(() -> {
                    show(paneLoading, false);
                    if (paneErreurServeur != null) {
                        if (lblErreurServeur != null) lblErreurServeur.setText(msg);
                        show(paneErreurServeur, true);
                    } else {
                        new Alert(Alert.AlertType.ERROR, msg).showAndWait();
                    }
                });
            }
        });
        t.setDaemon(true);
        t.start();
    }

    // ─── ACTIONS ──────────────────────────────────────────────────

    @FXML
    private void ouvrirPage() {
        if (urlPublique == null) return;
        try { Desktop.getDesktop().browse(URI.create(urlPublique)); }
        catch (Exception e) { new Alert(Alert.AlertType.ERROR, e.getMessage()).showAndWait(); }
    }

    @FXML
    private void copierLien() {
        if (urlPublique == null) return;
        javafx.scene.input.ClipboardContent cc = new javafx.scene.input.ClipboardContent();
        cc.putString(urlPublique);
        javafx.scene.input.Clipboard.getSystemClipboard().setContent(cc);
        btnCopierLien.setText("✅  Copié !");
        btnCopierLien.setDisable(true);
        new Thread(() -> {
            try { Thread.sleep(2000); } catch (InterruptedException ignored) {}
            Platform.runLater(() -> {
                btnCopierLien.setText("📋  Copier le lien");
                btnCopierLien.setDisable(false);
            });
        }).start();
    }

    @FXML
    private void fermer() {
        ((Stage) btnFermer.getScene().getWindow()).close();
    }

    private void show(VBox pane, boolean v) {
        if (pane == null) return;
        pane.setVisible(v);
        pane.setManaged(v);
    }

    private String nvl(String s) { return s == null ? "—" : s; }
}