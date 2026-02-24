package services.CandidatureService;

import entities.GCandidature.Candidature;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service CRUD pour la table candidature.
 * Méthodes utilisées par le front office :
 *   - addCandidature(c)
 *   - getAllCandidatures(onlyVisible)
 *   - updateCandidature(c)
 *   - hideCandidature(id)
 *   - getCandidatureById(id)
 */
public class CandidatureService {

    private final Connection connection;

    public CandidatureService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    // ────────────────────────── CREATE ─────────────────────────────────────
    public void addCandidature(Candidature c) throws SQLException {
        String sql = """
            INSERT INTO candidature
                (nomCandidature, nomStartup, dateDepot, statut, score, commentaire, idStartup, visible)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, c.getNomCandidature());
            ps.setString(2, c.getNomStartup());
            ps.setDate(3, c.getDateDepot());
            ps.setString(4, c.getStatut() != null ? c.getStatut() : "EN_ATTENTE");
            ps.setObject(5, c.getScore());        // peut être null
            ps.setString(6, c.getCommentaire());
            ps.setInt(7, c.getIdStartup());
            ps.setBoolean(8, c.isVisible());
            ps.executeUpdate();
        }
    }

    // ────────────────────────── READ ───────────────────────────────────────
    /**
     * Retourne toutes les candidatures, filtrées ou non par visible=1.
     *
     * @param onlyVisible true → WHERE visible = 1
     */
    public List<Candidature> getAllCandidatures(boolean onlyVisible) throws SQLException {
        List<Candidature> list = new ArrayList<>();
        String sql = "SELECT * FROM candidature"
                + (onlyVisible ? " WHERE visible = 1" : "")
                + " ORDER BY dateDepot DESC";

        try (Statement stmt = connection.createStatement();
             ResultSet rs   = stmt.executeQuery(sql)) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
        }
        return list;
    }

    /**
     * Récupère une candidature par son identifiant.
     */
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

    // ────────────────────────── UPDATE ─────────────────────────────────────
    /**
     * Met à jour tous les champs éditables d'une candidature.
     * Utilisé par le front office pour la modification inline.
     */
    public void updateCandidature(Candidature c) throws SQLException {
        String sql = """
            UPDATE candidature
            SET nomCandidature = ?,
                nomStartup     = ?,
                dateDepot      = ?,
                statut         = ?,
                score          = ?,
                commentaire    = ?
            WHERE idCandidature = ?
            """;
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, c.getNomCandidature());
            ps.setString(2, c.getNomStartup());
            ps.setDate(3, c.getDateDepot());
            ps.setString(4, c.getStatut());
            ps.setObject(5, c.getScore());        // peut être null
            ps.setString(6, c.getCommentaire());
            ps.setInt(7, c.getIdCandidature());
            ps.executeUpdate();
        }
    }

    // ────────────────────────── SOFT DELETE ────────────────────────────────
    /**
     * Cache une candidature (visible = 0) sans la supprimer.
     */
    public void hideCandidature(int id) throws SQLException {
        String sql = "UPDATE candidature SET visible = 0 WHERE idCandidature = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ────────────────────────── HARD DELETE ────────────────────────────────
    /**
     * Suppression définitive (à utiliser uniquement depuis le back office).
     */
    public void deleteCandidature(int id) throws SQLException {
        String sql = "DELETE FROM candidature WHERE idCandidature = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    // ────────────────────────── MAPPING ────────────────────────────────────
    private Candidature mapRow(ResultSet rs) throws SQLException {
        Candidature c = new Candidature();
        c.setIdCandidature(rs.getInt("idCandidature"));
        c.setNomCandidature(rs.getString("nomCandidature"));
        c.setNomStartup(rs.getString("nomStartup"));
        c.setDateDepot(rs.getDate("dateDepot"));
        c.setStatut(rs.getString("statut"));

        // score : null en DB → null en Java (rs.getDouble retourne 0 si null)
        Object scoreObj = rs.getObject("score");
        c.setScore(scoreObj == null ? null : rs.getDouble("score"));

        c.setCommentaire(rs.getString("commentaire"));
        c.setIdStartup(rs.getInt("idStartup"));
        c.setVisible(rs.getBoolean("visible"));
        return c;
    }
}
