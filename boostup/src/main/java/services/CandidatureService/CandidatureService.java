package services.CandidatureService;

import entities.GCandidature.Candidature;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CandidatureService {

    private final Connection  connection;
    private final EmailService emailService = new EmailService(); // ✅ NOUVEAU

    public CandidatureService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    // ────────────────────── UNICITÉ ────────────────────────────────

    public boolean existsNomCandidature(String nom, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM candidature WHERE nomCandidature = ? AND idCandidature <> ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nom);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ────────────────────── CREATE ─────────────────────────────────

    public void addCandidature(Candidature c) throws SQLException {
        String sql = """
            INSERT INTO candidature
                (nomCandidature, nomStartup, dateDepot, statut, score,
                 commentaire, idStartup, visible, emailContact)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, c.getNomCandidature());
            ps.setString(2, c.getNomStartup());
            ps.setDate(3, c.getDateDepot());
            ps.setString(4, c.getStatut() != null ? c.getStatut() : "EN_ATTENTE");
            ps.setObject(5, c.getScore());
            ps.setString(6, c.getCommentaire());
            ps.setInt(7, c.getIdStartup());
            ps.setBoolean(8, c.isVisible());
            ps.setString(9, c.getEmailContact()); // ✅ NOUVEAU
            ps.executeUpdate();
        }
    }

    // ────────────────────── READ ────────────────────────────────────

    public List<Candidature> getAllCandidatures(boolean onlyVisible) throws SQLException {
        List<Candidature> list = new ArrayList<>();
        String sql = "SELECT * FROM candidature"
                + (onlyVisible ? " WHERE visible = 1" : "")
                + " ORDER BY dateDepot DESC";
        try (Statement stmt = connection.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) list.add(mapRow(rs));
        }
        return list;
    }

    public Candidature getCandidatureById(int id) throws SQLException {
        String sql = "SELECT * FROM candidature WHERE idCandidature = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return mapRow(rs);
            }
        }
        return null;
    }

    // ────────────────────── UPDATE GÉNÉRAL ─────────────────────────

    public void updateCandidature(Candidature c) throws SQLException {
        String sql = """
            UPDATE candidature
            SET nomCandidature = ?, nomStartup = ?, dateDepot = ?,
                statut = ?, score = ?, commentaire = ?, emailContact = ?
            WHERE idCandidature = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, c.getNomCandidature());
            ps.setString(2, c.getNomStartup());
            ps.setDate(3, c.getDateDepot());
            ps.setString(4, c.getStatut());
            ps.setObject(5, c.getScore());
            ps.setString(6, c.getCommentaire());
            ps.setString(7, c.getEmailContact()); // ✅ NOUVEAU
            ps.setInt(8, c.getIdCandidature());
            ps.executeUpdate();
        }
    }

    // ────────────────────── ✅ UPDATE STATUT (avec email) ──────────
    /**
     * Met à jour UNIQUEMENT le statut, le score et le commentaire.
     * Envoie automatiquement un email de notification à la startup.
     *
     * C'est cette méthode qu'on appelle depuis le back-office
     * lors d'une validation / refus.
     */
    public void updateStatut(Candidature c) throws SQLException {
        // 1. Récupère l'ancien statut pour comparer
        Candidature ancien = getCandidatureById(c.getIdCandidature());
        String ancienStatut = ancien != null ? ancien.getStatut() : "";

        // 2. Mise à jour BDD
        String sql = """
            UPDATE candidature
            SET statut = ?, score = ?, commentaire = ?
            WHERE idCandidature = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, c.getStatut());
            ps.setObject(2, c.getScore());
            ps.setString(3, c.getCommentaire());
            ps.setInt(4, c.getIdCandidature());
            ps.executeUpdate();
        }

        // 3. ✅ Envoi email si le statut a changé ET qu'un email est disponible
        boolean statutChange = !nvl(c.getStatut()).equals(nvl(ancienStatut));
        String email = c.getEmailContact();

        if (statutChange && email != null && !email.isBlank()) {
            emailService.envoyerNotificationStatut(c, email);
        }
    }

    // ────────────────────── SOFT DELETE ────────────────────────────

    public void hideCandidature(int id) throws SQLException {
        String sql = "UPDATE candidature SET visible = 0 WHERE idCandidature = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ────────────────────── HARD DELETE ────────────────────────────

    public void deleteCandidature(int id) throws SQLException {
        String sql = "DELETE FROM candidature WHERE idCandidature = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ────────────────────── MAPPING ────────────────────────────────

    private Candidature mapRow(ResultSet rs) throws SQLException {
        Candidature c = new Candidature();
        c.setIdCandidature(rs.getInt("idCandidature"));
        c.setNomCandidature(rs.getString("nomCandidature"));
        c.setNomStartup(rs.getString("nomStartup"));
        c.setDateDepot(rs.getDate("dateDepot"));
        c.setStatut(rs.getString("statut"));
        Object scoreObj = rs.getObject("score");
        c.setScore(scoreObj == null ? null : rs.getDouble("score"));
        c.setCommentaire(rs.getString("commentaire"));
        c.setIdStartup(rs.getInt("idStartup"));
        c.setVisible(rs.getBoolean("visible"));
        // ✅ NOUVEAU — emailContact peut être NULL en base
        try { c.setEmailContact(rs.getString("emailContact")); }
        catch (SQLException ignored) {} // colonne absente si migration non faite
        return c;
    }

    private String nvl(String s) { return s == null ? "" : s; }
}