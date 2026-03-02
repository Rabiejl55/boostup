package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextArea;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import services.CandidatureService.GeminiAnalysisService;
import services.CandidatureService.GeminiAnalysisService.AnalysisResult;

import java.util.function.Consumer;

/**
 * Contrôleur du panneau d'analyse IA (AnalyseIAPanel.fxml).
 *
 * ─── Comment l'ouvrir depuis AddDossierController ──────────────────
 *
 *   FXMLLoader loader = new FXMLLoader(
 *       getClass().getResource("/Views/AnalyseIAPanel.fxml"));
 *   Parent root = loader.load();
 *   AnalyseIAPanelController ctrl = loader.getController();
 *
 *   // Pré-remplir avec les données du formulaire
 *   ctrl.setContexte(
 *       cbCandidature.getValue(),   // nom startup
 *       taDescription.getText(),    // description projet
 *       tfBusinessPlan.getText()    // business plan
 *   );
 *
 *   // Optionnel : reporter les notes IA dans EvaluationForm
 *   ctrl.setOnNotesApplied(notes -> {
 *       noteInnovationField.setText(notes[0]);
 *       noteViabiliteField.setText(notes[1]);
 *       noteMarcheField.setText(notes[2]);
 *       noteEquipeField.setText(notes[3]);
 *   });
 *
 *   Stage stage = new Stage();
 *   stage.initModality(Modality.APPLICATION_MODAL);
 *   stage.setScene(new Scene(root));
 *   stage.setTitle("🤖 Analyse IA");
 *   stage.show();
 * ───────────────────────────────────────────────────────────────────
 */
public class AnalyseIAPanelController {

    // ── FXML ──────────────────────────────────────────────────────
    @FXML private TextArea          taContexte;
    @FXML private Button            btnAnalyser;
    @FXML private Button            btnAnalyseRapide;
    @FXML private ProgressIndicator spinner;
    @FXML private Label             lblStatut;

    // Résultats
    @FXML private VBox  paneResultats;
    @FXML private HBox  bandeauDecision;
    @FXML private Label lblDecisionIcon;
    @FXML private Label lblDecision;
    @FXML private Label lblScoreEstime;
    @FXML private Label lblNoteInnov;
    @FXML private Label lblNoteViab;
    @FXML private Label lblNoteMarche;
    @FXML private Label lblNoteEquipe;
    @FXML private Label lblResume;
    @FXML private VBox  vboxPointsForts;
    @FXML private VBox  vboxPointsFaibles;
    @FXML private VBox  vboxRecommandations;
    @FXML private Button btnAppliquerNotes;

    // Erreur
    @FXML private HBox  paneErreur;
    @FXML private Label lblErreur;

    // ── ÉTAT INTERNE ──────────────────────────────────────────────
    private final GeminiAnalysisService geminiService = new GeminiAnalysisService();
    private AnalysisResult lastResult;
    private String nomStartup;
    private String descriptionProjet;
    private String businessPlan;
    private Consumer<String[]> onNotesApplied; // callback vers EvaluationForm

    // ─────────────────────────────────────────────────────────────

    /**
     * Pré-remplit la zone de contexte avec les données du formulaire dossier.
     */
    public void setContexte(String nomStartup, String description, String businessPlan) {
        this.nomStartup        = nomStartup;
        this.descriptionProjet = description;
        this.businessPlan      = businessPlan;

        taContexte.setText(String.format(
                "🏢 Startup : %s\n\n📝 Description du projet :\n%s\n\n📄 Business Plan :\n%s",
                nomStartup   != null ? nomStartup   : "Non précisé",
                description  != null ? description  : "Non fournie",
                businessPlan != null ? businessPlan : "Non fourni"
        ));
    }

    /**
     * Callback optionnel : appelé quand l'utilisateur clique "Appliquer les notes".
     * Le tableau String[] contient : [innovation, viabilité, marché, équipe]
     */
    public void setOnNotesApplied(Consumer<String[]> callback) {
        this.onNotesApplied = callback;
    }

    // ─── ACTIONS FXML ─────────────────────────────────────────────

    @FXML
    private void lancerAnalyse() {
        setLoading(true, "Analyse en cours… (5-10 sec)");
        masquerResultats();

        Thread t = new Thread(() -> {
            AnalysisResult result = geminiService.analyserBusinessPlan(
                    nomStartup, descriptionProjet, businessPlan);
            Platform.runLater(() -> {
                setLoading(false, "");
                if (result.success) afficherResultats(result);
                else                afficherErreur(result.errorMessage);
            });
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void lancerAnalyseRapide() {
        setLoading(true, "Analyse rapide… (2-3 sec)");
        masquerResultats();

        Thread t = new Thread(() -> {
            String desc = (descriptionProjet != null && !descriptionProjet.isBlank())
                    ? descriptionProjet : taContexte.getText();
            AnalysisResult result = geminiService.analyseRapide(desc);
            Platform.runLater(() -> {
                setLoading(false, "");
                if (result.success) afficherResultatsRapides(result);
                else                afficherErreur(result.errorMessage);
            });
        });
        t.setDaemon(true);
        t.start();
    }

    @FXML
    private void appliquerNotes() {
        if (lastResult == null || onNotesApplied == null) return;

        onNotesApplied.accept(new String[]{
                parseNote(lastResult.innovationNote),
                parseNote(lastResult.viabiliteNote),
                parseNote(lastResult.marcheNote),
                parseNote(lastResult.equipeNote)
        });

        // Feedback visuel — badge-validee existe dans style1.css
        btnAppliquerNotes.setText("✅  Notes appliquées !");
        btnAppliquerNotes.getStyleClass().setAll("btn-save"); // reste vert
        btnAppliquerNotes.setDisable(true);
    }

    @FXML
    private void copierAnalyse() {
        if (lastResult == null) return;

        String texte = String.format("""
                ══ ANALYSE IA DU BUSINESS PLAN ══
                Décision     : %s
                Score estimé : %s / 10
                
                RÉSUMÉ
                %s
                
                POINTS FORTS
                %s
                
                RISQUES
                %s
                
                RECOMMANDATIONS
                %s
                
                NOTES SUGGÉRÉES
                  Innovation : %s / 10
                  Viabilité  : %s / 10
                  Marché     : %s / 10
                  Équipe     : %s / 10
                """,
                lastResult.decision,        lastResult.scoreEstime,
                lastResult.resume,
                formatPoints(lastResult.pointsForts),
                formatPoints(lastResult.pointsFaibles),
                formatPoints(lastResult.recommandations),
                lastResult.innovationNote,  lastResult.viabiliteNote,
                lastResult.marcheNote,      lastResult.equipeNote
        );

        var cb = javafx.scene.input.Clipboard.getSystemClipboard();
        var cc = new javafx.scene.input.ClipboardContent();
        cc.putString(texte);
        cb.setContent(cc);
        lblStatut.setText("✅ Analyse copiée !");
    }

    @FXML
    private void fermer() {
        ((Stage) btnAnalyser.getScene().getWindow()).close();
    }

    // ─── AFFICHAGE ────────────────────────────────────────────────

    private void afficherResultats(AnalysisResult result) {
        this.lastResult = result;
        paneErreur.setVisible(false);
        paneErreur.setManaged(false);

        String decision = result.decision != null ? result.decision.toUpperCase() : "MOYENNE";

        // ── Badge décision : réutilise badge-validee / badge-refusee / badge-attente ──
        lblDecision.getStyleClass().setAll(switch (decision) {
            case "PROMETTEUSE"  -> "badge-validee";
            case "INSUFFISANTE" -> "badge-refusee";
            default             -> "badge-attente";
        });
        lblDecision.setText(decision);

        lblDecisionIcon.setText(switch (decision) {
            case "PROMETTEUSE"  -> "✅";
            case "INSUFFISANTE" -> "❌";
            default             -> "⚠️";
        });

        lblScoreEstime.setText("Score estimé : " + result.scoreEstime + " / 10");

        // ── Notes ──
        lblNoteInnov.setText(parseNote(result.innovationNote));
        lblNoteViab.setText(parseNote(result.viabiliteNote));
        lblNoteMarche.setText(parseNote(result.marcheNote));
        lblNoteEquipe.setText(parseNote(result.equipeNote));

        // ── Résumé ──
        lblResume.setText(result.resume != null ? result.resume : "—");

        // ── Points forts — bullet vert (success) ──
        vboxPointsForts.getChildren().clear();
        if (result.pointsForts != null)
            for (String p : result.pointsForts.split("\\|"))
                vboxPointsForts.getChildren().add(
                        bullet("→ " + p.trim(), "#198754")); // vert BoostUp

        // ── Points faibles — bullet rouge ──
        vboxPointsFaibles.getChildren().clear();
        if (result.pointsFaibles != null)
            for (String p : result.pointsFaibles.split("\\|"))
                vboxPointsFaibles.getChildren().add(
                        bullet("→ " + p.trim(), "#dc3545")); // rouge BoostUp

        // ── Recommandations — bullet indigo ──
        vboxRecommandations.getChildren().clear();
        if (result.recommandations != null)
            for (String r : result.recommandations.split("\\|"))
                vboxRecommandations.getChildren().add(
                        bullet("💡 " + r.trim(), "#4f62e8")); // indigo BoostUp

        paneResultats.setVisible(true);
        paneResultats.setManaged(true);
    }

    private void afficherResultatsRapides(AnalysisResult result) {
        this.lastResult = result;
        paneErreur.setVisible(false);
        paneErreur.setManaged(false);

        String decision = result.decision != null ? result.decision.toUpperCase() : "MOYENNE";
        lblDecision.getStyleClass().setAll(switch (decision) {
            case "PROMETTEUSE"  -> "badge-validee";
            case "INSUFFISANTE" -> "badge-refusee";
            default             -> "badge-attente";
        });
        lblDecision.setText(decision);
        lblDecisionIcon.setText(decision.equals("PROMETTEUSE") ? "✅"
                : decision.equals("INSUFFISANTE") ? "❌" : "⚠️");
        lblScoreEstime.setText("Score estimé : " + result.scoreEstime + " / 10");
        lblResume.setText(result.resume != null ? result.resume : "—");

        // Notes non disponibles en mode rapide
        lblNoteInnov.setText("—");
        lblNoteViab.setText("—");
        lblNoteMarche.setText("—");
        lblNoteEquipe.setText("—");

        vboxPointsForts.getChildren().clear();
        vboxPointsFaibles.getChildren().clear();
        vboxRecommandations.getChildren().clear();

        if (result.recommandations != null && !result.recommandations.isBlank())
            vboxRecommandations.getChildren().add(
                    bullet("💡 " + result.recommandations.trim(), "#4f62e8"));

        paneResultats.setVisible(true);
        paneResultats.setManaged(true);
    }

    private void afficherErreur(String message) {
        paneResultats.setVisible(false);
        paneResultats.setManaged(false);
        lblErreur.setText(message != null ? message
                : "Erreur inconnue. Vérifiez votre clé API dans GeminiAnalysisService.java.");
        paneErreur.setVisible(true);
        paneErreur.setManaged(true);
    }

    private void masquerResultats() {
        paneResultats.setVisible(false);
        paneResultats.setManaged(false);
        paneErreur.setVisible(false);
        paneErreur.setManaged(false);
    }

    // ─── HELPERS ──────────────────────────────────────────────────

    private void setLoading(boolean loading, String msg) {
        spinner.setVisible(loading);
        lblStatut.setText(msg);
        btnAnalyser.setDisable(loading);
        btnAnalyseRapide.setDisable(loading);
    }

    /** Crée un label bullet avec la couleur BoostUp correspondante. */
    private Label bullet(String text, String hexColor) {
        Label lbl = new Label(text);
        lbl.setWrapText(true);
        lbl.setStyle("-fx-font-size:12px; -fx-text-fill:" + hexColor + ";");
        return lbl;
    }

    /** Extrait un entier d'une note renvoyée par l'IA ("8", "8.5", "N/A"…). */
    private String parseNote(String raw) {
        if (raw == null || raw.isBlank() || raw.equalsIgnoreCase("N/A")) return "—";
        try {
            return String.valueOf((int) Math.round(
                    Double.parseDouble(raw.replaceAll("[^0-9.]", ""))));
        } catch (NumberFormatException e) {
            return "—";
        }
    }

    private String formatPoints(String raw) {
        if (raw == null || raw.isBlank()) return "Non disponible";
        StringBuilder sb = new StringBuilder();
        for (String p : raw.split("\\|"))
            sb.append("• ").append(p.trim()).append("\n");
        return sb.toString().trim();
    }
}