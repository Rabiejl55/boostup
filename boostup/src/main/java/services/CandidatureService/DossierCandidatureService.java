package services.CandidatureService;

import entities.GCandidature.DossierCandidature;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DossierCandidatureService {

    private Connection connection;

    public DossierCandidatureService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    // ────────────────────────── UNICITÉ ────────────────────────────────────
    /**
     * Vérifie si un nomDossier existe déjà.
     * @param nom       le nom à vérifier
     * @param excludeId id à exclure (0 pour ajout, id réel pour update)
     */
    public boolean existsNomDossier(String nom, int excludeId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM dossiercandidature WHERE nomDossier = ? AND idDossier <> ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, nom);
            ps.setInt(2, excludeId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        }
    }

    // ────────────────────────── CREATE ─────────────────────────────────────
    public void addDossier(DossierCandidature dossier) throws SQLException {
        String sql = "INSERT INTO dossiercandidature (nomDossier, descriptionProjet, businessPlan, dateCreation, etat, idCandidature, visible) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, dossier.getNomDossier());
            ps.setString(2, dossier.getDescriptionProjet());
            ps.setString(3, dossier.getBusinessPlan());
            ps.setDate(4, dossier.getDateCreation());
            ps.setString(5, dossier.getEtat());
            ps.setInt(6, dossier.getIdCandidature());
            ps.setBoolean(7, dossier.isVisible());
            ps.executeUpdate();
        }
    }

    // ────────────────────────── READ ───────────────────────────────────────
    public List<DossierCandidature> getAllDossiers(boolean onlyVisible) throws SQLException {
        List<DossierCandidature> dossiers = new ArrayList<>();
        String sql = "SELECT d.*, c.nomCandidature FROM dossiercandidature d " +
                "JOIN candidature c ON d.idCandidature = c.idCandidature" +
                (onlyVisible ? " WHERE d.visible = 1" : "");
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            while (rs.next()) {
                DossierCandidature d = new DossierCandidature();
                d.setIdDossier(rs.getInt("idDossier"));
                d.setNomDossier(rs.getString("nomDossier"));
                d.setDescriptionProjet(rs.getString("descriptionProjet"));
                d.setBusinessPlan(rs.getString("businessPlan"));
                d.setDateCreation(rs.getDate("dateCreation"));
                d.setEtat(rs.getString("etat"));
                d.setIdCandidature(rs.getInt("idCandidature"));
                d.setNomCandidature(rs.getString("nomCandidature"));
                d.setVisible(rs.getBoolean("visible"));
                dossiers.add(d);
            }
        }
        return dossiers;
    }

    // ────────────────────────── UPDATE ─────────────────────────────────────
    public void updateDossier(DossierCandidature dossier) throws SQLException {
        String sql = "UPDATE dossiercandidature SET nomDossier=?, descriptionProjet=?, businessPlan=?, dateCreation=?, etat=?, idCandidature=?, visible=? WHERE idDossier=?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, dossier.getNomDossier());
            ps.setString(2, dossier.getDescriptionProjet());
            ps.setString(3, dossier.getBusinessPlan());
            ps.setDate(4, dossier.getDateCreation());
            ps.setString(5, dossier.getEtat());
            ps.setInt(6, dossier.getIdCandidature());
            ps.setBoolean(7, dossier.isVisible());
            ps.setInt(8, dossier.getIdDossier());
            ps.executeUpdate();
        }
    }

    // ────────────────────────── SOFT DELETE ────────────────────────────────
    public void hideDossier(int idDossier) throws SQLException {
        String sql = "UPDATE dossiercandidature SET visible = 0 WHERE idDossier = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idDossier);
            ps.executeUpdate();
        }
    }

    // ────────────────────────── GET BY ID ──────────────────────────────────
    public DossierCandidature getDossierById(int id) throws SQLException {
        String sql = "SELECT d.*, c.nomCandidature FROM dossiercandidature d " +
                "JOIN candidature c ON d.idCandidature = c.idCandidature WHERE d.idDossier = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    DossierCandidature d = new DossierCandidature();
                    d.setIdDossier(rs.getInt("idDossier"));
                    d.setNomDossier(rs.getString("nomDossier"));
                    d.setDescriptionProjet(rs.getString("descriptionProjet"));
                    d.setBusinessPlan(rs.getString("businessPlan"));
                    d.setDateCreation(rs.getDate("dateCreation"));
                    d.setEtat(rs.getString("etat"));
                    d.setIdCandidature(rs.getInt("idCandidature"));
                    d.setNomCandidature(rs.getString("nomCandidature"));
                    d.setVisible(rs.getBoolean("visible"));
                    return d;
                }
            }
        }
        return null;
    }
}