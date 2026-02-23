package services.CandidatureService;

import entities.GCandidature.Candidature;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CandidatureService {
    private Connection connection;

    public CandidatureService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    // CREATE: Ajouter une candidature (pour front, mais inclus pour complétude)
    public void addCandidature(Candidature candidature) throws SQLException {
        String sql = "INSERT INTO candidature (nomCandidature, nomStartup, dateDepot, statut, score, commentaire, idStartup, visible) VALUES (?, ?, ?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setString(1, candidature.getNomCandidature());
        ps.setString(2, candidature.getNomStartup());
        ps.setDate(3, candidature.getDateDepot());
        ps.setString(4, candidature.getStatut());
        ps.setObject(5, candidature.getScore());
        ps.setString(6, candidature.getCommentaire());
        ps.setInt(7, candidature.getIdStartup());
        ps.setBoolean(8, candidature.isVisible());
        ps.executeUpdate();
    }

    // READ: Lister toutes les candidatures (optionnel: filtrer par visible=true)
    public List<Candidature> getAllCandidatures(boolean onlyVisible) throws SQLException {
        List<Candidature> candidatures = new ArrayList<>();
        String sql = "SELECT * FROM candidature" + (onlyVisible ? " WHERE visible = 1" : "");
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery(sql);
        while (rs.next()) {
            Candidature c = new Candidature();
            c.setIdCandidature(rs.getInt("idCandidature"));
            c.setNomCandidature(rs.getString("nomCandidature"));
            c.setNomStartup(rs.getString("nomStartup"));
            c.setDateDepot(rs.getDate("dateDepot"));
            c.setStatut(rs.getString("statut"));
            c.setScore(rs.getDouble("score") == 0 ? null : rs.getDouble("score")); // Handle NULL
            c.setCommentaire(rs.getString("commentaire"));
            c.setIdStartup(rs.getInt("idStartup"));
            c.setVisible(rs.getBoolean("visible"));
            candidatures.add(c);
        }
        return candidatures;
    }

    // UPDATE: Cacher une candidature (set visible=0)
    public void hideCandidature(int id) throws SQLException {
        String sql = "UPDATE candidature SET visible = 0 WHERE idCandidature = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    // DELETE: Supprimer définitivement (si besoin, sinon utilise hide)
    public void deleteCandidature(int id) throws SQLException {
        String sql = "DELETE FROM candidature WHERE idCandidature = ?";
        PreparedStatement ps = connection.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    public Candidature getCandidatureById(int id) throws SQLException {
        String sql = "SELECT * FROM candidature WHERE idCandidature = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Candidature c = new Candidature();
                c.setIdCandidature(rs.getInt("idCandidature"));
                c.setNomCandidature(rs.getString("nomCandidature"));
                c.setNomStartup(rs.getString("nomStartup"));
                c.setDateDepot(rs.getDate("dateDepot"));
                c.setStatut(rs.getString("statut"));
                c.setScore(rs.getDouble("score") == 0 ? null : rs.getDouble("score"));
                c.setCommentaire(rs.getString("commentaire"));
                c.setIdStartup(rs.getInt("idStartup"));
                c.setVisible(rs.getBoolean("visible"));
                return c;
            }
        }
        return null;
    }
}