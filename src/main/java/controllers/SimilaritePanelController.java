package controllers;

import entities.GCandidature.Candidature;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import services.CandidatureService.CandidatureService;
import services.CandidatureService.SimilarityService;
import services.CandidatureService.SimilarityService.SimilarityResult;

import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.ResourceBundle;

public class SimilaritePanelController implements Initializable {

    // ── FXML ──────────────────────────────────────────────────────
    @FXML private Slider     sliderSeuil;
    @FXML private Label      lblSeuilValeur;
    @FXML private Button     btnAnalyser;
    @FXML private Label      lblResultatCount;
    @FXML private VBox       listeResultats;
    @FXML private VBox       paneLoading;
    @FXML private VBox       paneVide;
    @FXML private VBox       paneResultats;
    @FXML private Label      lblTotalCandidatures;

    // ── ÉTAT ──────────────────────────────────────────────────────
    private final CandidatureService service    = new CandidatureService();
    private final SimilarityService  similarity = new SimilarityService();
    private List<Candidature>        toutesLes;

    public SimilaritePanelController() throws SQLException {
    }

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        // Seuil : 50% → 95%, défaut 75%
        sliderSeuil.setMin(50);
        sliderSeuil.setMax(95);
        sliderSeuil.setValue(75);
        sliderSeuil.setMajorTickUnit(5);
        sliderSeuil.setSnapToTicks(true);
        lblSeuilValeur.setText("75%");

        sliderSeuil.valueProperty().addListener((obs, old, val) -> {
            int v = val.intValue();
            lblSeuilValeur.setText(v + "%");
        });

        show(paneVide, true);
        show(paneLoading, false);
        show(paneResultats, false);

        // Charger le nombre de candidatures
        chargerStats();
    }

    // ─── CHARGEMENT STATS ─────────────────────────────────────────

    private void chargerStats() {
        new Thread(() -> {
            try {
                toutesLes = service.getAllCandidatures(true);
                Platform.runLater(() ->
                        lblTotalCandidatures.setText(toutesLes.size()
                                + " candidature" + (toutesLes.size() > 1 ? "s" : "") + " à analyser")
                );
            } catch (SQLException e) {
                Platform.runLater(() -> lblTotalCandidatures.setText("Erreur de chargement"));
            }
        }).start();
    }

    // ─── LANCEMENT ANALYSE ────────────────────────────────────────

    @FXML
    private void lancerAnalyse() {
        int seuil = (int) sliderSeuil.getValue();

        show(paneVide, false);
        show(paneResultats, false);
        show(paneLoading, true);
        btnAnalyser.setDisable(true);
        btnAnalyser.setText("⏳  Analyse en cours…");

        new Thread(() -> {
            try {
                if (toutesLes == null) {
                    toutesLes = service.getAllCandidatures(true);
                }

                List<SimilarityResult> resultats =
                        similarity.detecterSimilarites(toutesLes, seuil);

                Platform.runLater(() -> {
                    afficherResultats(resultats, seuil);
                    show(paneLoading, false);
                    show(paneResultats, true);
                    btnAnalyser.setDisable(false);
                    btnAnalyser.setText("🔍  Lancer l'analyse");
                });

            } catch (Exception e) {
                Platform.runLater(() -> {
                    show(paneLoading, false);
                    show(paneVide, true);
                    btnAnalyser.setDisable(false);
                    btnAnalyser.setText("🔍  Lancer l'analyse");
                    new Alert(Alert.AlertType.ERROR,
                            "Erreur lors de l'analyse : " + e.getMessage()).showAndWait();
                });
            }
        }).start();
    }

    // ─── AFFICHAGE DES RÉSULTATS ──────────────────────────────────

    private void afficherResultats(List<SimilarityResult> resultats, int seuil) {
        listeResultats.getChildren().clear();

        if (resultats.isEmpty()) {
            lblResultatCount.setText("✅  Aucun doublon détecté au-dessus de " + seuil + "%");
            lblResultatCount.setStyle("-fx-text-fill:#10b981; -fx-font-weight:700; -fx-font-size:13px;");
            return;
        }

        // Compteur
        String plural = resultats.size() > 1 ? "s" : "";
        lblResultatCount.setText("🚨  " + resultats.size()
                + " paire" + plural + " suspecte" + plural + " détectée" + plural);
        lblResultatCount.setStyle("-fx-text-fill:#ef4444; -fx-font-weight:700; -fx-font-size:13px;");

        // Cartes de résultats
        for (int i = 0; i < resultats.size(); i++) {
            listeResultats.getChildren().add(buildCarteResultat(resultats.get(i), i + 1));
        }
    }

    // ─── CARTE D'UN RÉSULTAT ──────────────────────────────────────

    private VBox buildCarteResultat(SimilarityResult r, int num) {
        VBox carte = new VBox(0);
        carte.setStyle(
                "-fx-background-color: white;" +
                        "-fx-background-radius: 14px;" +
                        "-fx-border-color: #e8ecf0;" +
                        "-fx-border-radius: 14px;" +
                        "-fx-border-width: 1.5px;" +
                        "-fx-effect: dropshadow(gaussian, rgba(31,41,89,0.06), 12, 0, 0, 3);"
        );

        // ── Barre de score colorée en haut ──
        String couleur = r.getCouleur();
        HBox barreScore = new HBox(12);
        barreScore.setAlignment(Pos.CENTER_LEFT);
        barreScore.setPadding(new Insets(12, 18, 12, 18));
        barreScore.setStyle("-fx-background-color:" + couleur + "18;" +
                "-fx-border-color: transparent transparent #e8ecf0 transparent;" +
                "-fx-border-width: 0 0 1 0;" +
                "-fx-background-radius: 14 14 0 0;");

        // Numéro
        Label lblNum = new Label("#" + num);
        lblNum.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill:" + couleur + ";" +
                "-fx-background-color:" + couleur + "22;" +
                "-fx-background-radius:20; -fx-padding:3 9;");

        // Niveau
        Label lblNiveau = new Label(r.getNiveau());
        lblNiveau.setStyle("-fx-font-size:11px; -fx-font-weight:800; -fx-text-fill:" + couleur + ";");

        Region sp = new Region(); HBox.setHgrow(sp, Priority.ALWAYS);

        // Score badge
        Label lblScore = new Label(r.pourcentage + "%");
        lblScore.setStyle(
                "-fx-font-size:22px; -fx-font-weight:900;" +
                        "-fx-text-fill:" + couleur + ";" +
                        "-fx-font-family:'Georgia';"
        );

        Label lblSimilaire = new Label("similaire");
        lblSimilaire.setStyle("-fx-font-size:11px; -fx-text-fill:#9ca3af; -fx-font-weight:500;");

        VBox scoreBox = new VBox(0, lblScore, lblSimilaire);
        scoreBox.setAlignment(Pos.CENTER_RIGHT);

        barreScore.getChildren().addAll(lblNum, lblNiveau, sp, scoreBox);

        // ── Corps : les deux candidatures ──
        HBox corps = new HBox(0);
        corps.setPadding(new Insets(16, 18, 16, 18));

        // Candidature A
        VBox boxA = buildCandidatureBox(r.candidatureA, "A", "#4f62e8");
        HBox.setHgrow(boxA, Priority.ALWAYS);

        // Séparateur central avec icône
        VBox separateur = new VBox();
        separateur.setAlignment(Pos.CENTER);
        separateur.setPadding(new Insets(0, 14, 0, 14));
        Label iconSim = new Label("≈");
        iconSim.setStyle("-fx-font-size:22px; -fx-text-fill:" + couleur + "; -fx-font-weight:900;");
        separateur.getChildren().add(iconSim);

        // Candidature B
        VBox boxB = buildCandidatureBox(r.candidatureB, "B", "#7c3aed");
        HBox.setHgrow(boxB, Priority.ALWAYS);

        corps.getChildren().addAll(boxA, separateur, boxB);

        // ── Barre de progression visuelle ──
        HBox barreProgress = new HBox(10);
        barreProgress.setAlignment(Pos.CENTER_LEFT);
        barreProgress.setPadding(new Insets(0, 18, 14, 18));

        Label lblProgLabel = new Label("Similarité :");
        lblProgLabel.setStyle("-fx-font-size:11px; -fx-text-fill:#9ca3af; -fx-font-weight:600; -fx-min-width:70;");

        // Background de la barre
        StackPane progBg = new StackPane();
        progBg.setStyle("-fx-background-color:#f0f2f8; -fx-background-radius:10;");
        progBg.setPrefHeight(8);
        HBox.setHgrow(progBg, Priority.ALWAYS);

        // Remplissage coloré
        Region progFill = new Region();
        progFill.setStyle("-fx-background-color:" + couleur + "; -fx-background-radius:10;");
        progFill.setPrefHeight(8);
        progFill.setPrefWidth(0); // sera mis à jour après rendu

        StackPane.setAlignment(progFill, Pos.CENTER_LEFT);
        progBg.getChildren().add(progFill);

        // Mettre à jour la largeur après que la scène soit rendue
        progBg.widthProperty().addListener((obs, old, width) -> {
            double w = width.doubleValue() * r.score;
            progFill.setPrefWidth(Math.max(0, w));
        });

        barreProgress.getChildren().addAll(lblProgLabel, progBg);

        carte.getChildren().addAll(barreScore, corps, barreProgress);
        return carte;
    }

    private VBox buildCandidatureBox(Candidature c, String lettre, String couleur) {
        VBox box = new VBox(6);
        box.setStyle("-fx-background-color:#f8fafc; -fx-background-radius:10; -fx-padding:12;");

        HBox titre = new HBox(8);
        titre.setAlignment(Pos.CENTER_LEFT);

        Label badge = new Label(lettre);
        badge.setStyle("-fx-background-color:" + couleur + "; -fx-text-fill:white;" +
                "-fx-font-size:11px; -fx-font-weight:800;" +
                "-fx-background-radius:6; -fx-padding:2 7;");

        Label nomCand = new Label(nvl(c.getNomCandidature()));
        nomCand.setStyle("-fx-font-size:13px; -fx-font-weight:800; -fx-text-fill:#1e2746;");
        nomCand.setWrapText(true);
        HBox.setHgrow(nomCand, Priority.ALWAYS);

        titre.getChildren().addAll(badge, nomCand);

        Label startup = new Label("🏢  " + nvl(c.getNomStartup()));
        startup.setStyle("-fx-font-size:11px; -fx-text-fill:#6b7494; -fx-font-weight:500;");

        Label statut = new Label(emojiStatut(c.getStatut()) + "  " + nvl(c.getStatut()));
        statut.setStyle("-fx-font-size:11px; -fx-font-weight:700; -fx-text-fill:"
                + couleurStatut(c.getStatut()) + ";");

        // Extrait du commentaire
        String cmt = nvl(c.getCommentaire());
        if (!cmt.isEmpty()) {
            String extrait = cmt.length() > 80 ? cmt.substring(0, 77) + "…" : cmt;
            Label lblCmt = new Label("« " + extrait + " »");
            lblCmt.setStyle("-fx-font-size:11px; -fx-text-fill:#9ca3af;" +
                    "-fx-font-style:italic; -fx-wrap-text:true;");
            lblCmt.setWrapText(true);
            box.getChildren().addAll(titre, startup, statut, lblCmt);
        } else {
            box.getChildren().addAll(titre, startup, statut);
        }

        return box;
    }

    // ─── HELPERS ──────────────────────────────────────────────────

    private void show(VBox pane, boolean v) {
        if (pane == null) return;
        pane.setVisible(v); pane.setManaged(v);
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private String emojiStatut(String s) {
        return switch (s == null ? "" : s) {
            case "VALIDEE" -> "✅";
            case "REFUSEE" -> "❌";
            default        -> "⏳";
        };
    }

    private String couleurStatut(String s) {
        return switch (s == null ? "" : s) {
            case "VALIDEE" -> "#0d7a57";
            case "REFUSEE" -> "#b91c1c";
            default        -> "#92610a";
        };
    }

    @FXML
    private void fermer() {
        ((Stage) btnAnalyser.getScene().getWindow()).close();
    }
}