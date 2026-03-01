package services.CandidatureService;

import entities.GCandidature.Evaluation;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvaluationService {

    private Connection connection;
    private final EmailService emailService = new EmailService(); // ✅ NOUVEAU

    public EvaluationService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    // ────────────────────────── UNICITÉ ────────────────────────────────────

    public boolean existsNomEvaluation(String nom, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM evaluation WHERE nomEvaluation = ? AND idEvaluation <> ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nom);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ────────────────────────── CREATE ─────────────────────────────────────

    public void addEvaluation(Evaluation evaluation) throws SQLException {
        String sql = "INSERT INTO evaluation (nomEvaluation, noteInnovation, noteViabilite, noteMarche, noteEquipe, noteGlobale, decision, idCandidature, visible) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, evaluation.getNomEvaluation());
            ps.setObject(2, evaluation.getNoteInnovation());
            ps.setObject(3, evaluation.getNoteViabilite());
            ps.setObject(4, evaluation.getNoteMarche());
            ps.setObject(5, evaluation.getNoteEquipe());
            ps.setObject(6, evaluation.getNoteGlobale());
            ps.setString(7, evaluation.getDecision());
            ps.setInt(8, evaluation.getIdCandidature());
            ps.setBoolean(9, evaluation.isVisible());
            ps.executeUpdate();
        }
        if (evaluation.getDecision() != null && evaluation.getNoteGlobale() != null) {
            // ✅ Met à jour la candidature ET envoie l'email
            updateCandidatureFromEvaluation(
                    evaluation.getIdCandidature(),
                    evaluation.getDecision(),
                    evaluation.getNoteGlobale()
            );
        }
    }

    // ────────────────────────── READ ───────────────────────────────────────

    public List<Evaluation> getAllEvaluations(boolean onlyVisible) throws SQLException {
        List<Evaluation> evaluations = new ArrayList<>();
        String sql = "SELECT e.*, c.nomCandidature FROM evaluation e " +
                "JOIN candidature c ON e.idCandidature = c.idCandidature " +
                (onlyVisible ? "WHERE e.visible = 1" : "");
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                Evaluation e = new Evaluation();
                e.setIdEvaluation(rs.getInt("idEvaluation"));
                e.setNomEvaluation(rs.getString("nomEvaluation"));
                e.setNoteInnovation(rs.getObject("noteInnovation") != null ? rs.getInt("noteInnovation") : null);
                e.setNoteViabilite(rs.getObject("noteViabilite")   != null ? rs.getInt("noteViabilite")  : null);
                e.setNoteMarche(rs.getObject("noteMarche")         != null ? rs.getInt("noteMarche")     : null);
                e.setNoteEquipe(rs.getObject("noteEquipe")         != null ? rs.getInt("noteEquipe")     : null);
                e.setNoteGlobale(rs.getObject("noteGlobale")       != null ? rs.getDouble("noteGlobale") : null);
                e.setDecision(rs.getString("decision"));
                e.setIdCandidature(rs.getInt("idCandidature"));
                e.setNomCandidature(rs.getString("nomCandidature"));
                e.setVisible(rs.getBoolean("visible"));
                evaluations.add(e);
            }
        }
        return evaluations;
    }

    // ────────────────────────── UPDATE ─────────────────────────────────────

    public void updateEvaluation(Evaluation evaluation) throws SQLException {
        String sql = "UPDATE evaluation SET nomEvaluation=?, noteInnovation=?, noteViabilite=?, noteMarche=?, noteEquipe=?, noteGlobale=?, decision=?, idCandidature=?, visible=? WHERE idEvaluation=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, evaluation.getNomEvaluation());
            ps.setObject(2, evaluation.getNoteInnovation());
            ps.setObject(3, evaluation.getNoteViabilite());
            ps.setObject(4, evaluation.getNoteMarche());
            ps.setObject(5, evaluation.getNoteEquipe());
            ps.setObject(6, evaluation.getNoteGlobale());
            ps.setString(7, evaluation.getDecision());
            ps.setInt(8, evaluation.getIdCandidature());
            ps.setBoolean(9, evaluation.isVisible());
            ps.setInt(10, evaluation.getIdEvaluation());
            ps.executeUpdate();
        }
        if (evaluation.getDecision() != null && evaluation.getNoteGlobale() != null) {
            // ✅ Met à jour la candidature ET envoie l'email
            updateCandidatureFromEvaluation(
                    evaluation.getIdCandidature(),
                    evaluation.getDecision(),
                    evaluation.getNoteGlobale()
            );
        }
    }

    // ────────────────────────── SOFT DELETE ────────────────────────────────

    public void hideEvaluation(int idEvaluation) throws SQLException {
        String sql = "UPDATE evaluation SET visible = 0 WHERE idEvaluation = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idEvaluation);
            ps.executeUpdate();
        }
    }

    // ────────────────────────── GET BY ID ──────────────────────────────────

    public Evaluation getEvaluationById(int id) throws SQLException {
        String sql = "SELECT e.*, c.nomCandidature FROM evaluation e " +
                "JOIN candidature c ON e.idCandidature = c.idCandidature WHERE e.idEvaluation = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    Evaluation e = new Evaluation();
                    e.setIdEvaluation(rs.getInt("idEvaluation"));
                    e.setNomEvaluation(rs.getString("nomEvaluation"));
                    e.setNoteInnovation(rs.getObject("noteInnovation") != null ? rs.getInt("noteInnovation") : null);
                    e.setNoteViabilite(rs.getObject("noteViabilite")   != null ? rs.getInt("noteViabilite")  : null);
                    e.setNoteMarche(rs.getObject("noteMarche")         != null ? rs.getInt("noteMarche")     : null);
                    e.setNoteEquipe(rs.getObject("noteEquipe")         != null ? rs.getInt("noteEquipe")     : null);
                    e.setNoteGlobale(rs.getObject("noteGlobale")       != null ? rs.getDouble("noteGlobale") : null);
                    e.setDecision(rs.getString("decision"));
                    e.setIdCandidature(rs.getInt("idCandidature"));
                    e.setNomCandidature(rs.getString("nomCandidature"));
                    e.setVisible(rs.getBoolean("visible"));
                    return e;
                }
            }
        }
        return null;
    }

    // ────────────────────────── PRIVATE ────────────────────────────────────

    /**
     * ✅ Met à jour le statut + score de la candidature,
     *    puis envoie automatiquement l'email de notification à la startup.
     */
    private void updateCandidatureFromEvaluation(int idCandidature, String decision,
                                                 Double noteGlobale) throws SQLException {
        String statut = decision.equalsIgnoreCase("ACCEPTEE") ? "VALIDEE" : "REFUSEE";

        // 1. Mise à jour statut + score en BDD
        String sql = "UPDATE candidature SET statut = ?, score = ? WHERE idCandidature = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, statut);
            ps.setObject(2, noteGlobale);
            ps.setInt(3, idCandidature);
            ps.executeUpdate();
        }

        // 2. ✅ Récupérer la candidature complète pour l'email
        CandidatureService candidatureService = new CandidatureService();
        try {
            entities.GCandidature.Candidature candidature =
                    candidatureService.getCandidatureById(idCandidature);

            if (candidature != null) {
                // Mettre à jour les champs pour le template email
                candidature.setStatut(statut);
                candidature.setScore(noteGlobale);

                String email = candidature.getEmailContact();

                // 3. ✅ Envoi email si l'adresse est disponible
                if (email != null && !email.isBlank()) {
                    emailService.envoyerNotificationStatut(candidature, email);
                    System.out.println("✅ Notification envoyée à : " + email
                            + "  [" + statut + " — score: " + noteGlobale + "]");
                } else {
                    System.out.println("⚠️ Pas d'email renseigné pour la candidature #"
                            + idCandidature + " — notification ignorée.");
                }
            }
        } catch (Exception e) {
            // L'email ne doit jamais bloquer la sauvegarde
            System.err.println("⚠️ Erreur lors de l'envoi email (non bloquant) : " + e.getMessage());
        }
    }
}