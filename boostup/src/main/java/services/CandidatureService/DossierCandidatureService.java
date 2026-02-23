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

    // CREATE: Ajouter un dossier de candidature
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

    // READ: Lister tous les dossiers (avec JOIN pour nomCandidature au lieu d'idCandidature)
    public List<DossierCandidature> getAllDossiers(boolean onlyVisible) throws SQLException {
        List<DossierCandidature> dossiers = new ArrayList<>();
        String sql = "SELECT d.*, c.nomCandidature " +
                "FROM dossiercandidature d " +
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
                d.setIdCandidature(rs.getInt("idCandidature"));  // Gardé pour logique interne
                d.setNomCandidature(rs.getString("nomCandidature"));  // Nouveau champ pour affichage
                d.setVisible(rs.getBoolean("visible"));
                dossiers.add(d);
            }
        }
        return dossiers;
    }

    // UPDATE: Mettre à jour un dossier (inclus pour CRUD complet)
    public void updateDossier(DossierCandidature dossier) throws SQLException {
        String sql = "UPDATE dossiercandidature SET nomDossier = ?, descriptionProjet = ?, businessPlan = ?, dateCreation = ?, etat = ?, idCandidature = ?, visible = ? WHERE idDossier = ?";
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

    // Cacher un dossier (soft delete)
    public void hideDossier(int idDossier) throws SQLException {
        String sql = "UPDATE dossiercandidature SET visible = 0 WHERE idDossier = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, idDossier);
            ps.executeUpdate();
        }
    }

    // Optionnel : récupérer un dossier par ID (avec JOIN pour nomCandidature)
    public DossierCandidature getDossierById(int id) throws SQLException {
        String sql = "SELECT d.*, c.nomCandidature " +
                "FROM dossiercandidature d " +
                "JOIN candidature c ON d.idCandidature = c.idCandidature " +
                "WHERE d.idDossier = ?";
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