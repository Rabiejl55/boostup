package services.FinancementService;

import entities.GFinancement.Projet;
import services.IService;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ProjetService implements IService<Projet> {

    @Override
    public void ajouter(Projet projet) throws SQLException {
        String sql = "INSERT INTO projet (titre, description, budget, statut) VALUES (?, ?, ?, ?)";
        Connection conn = MyDatabase.getInstance().getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, projet.getTitre());
            stmt.setString(2, projet.getDescription());
            stmt.setDouble(3, projet.getBudget());
            stmt.setString(4, projet.getStatut());
            stmt.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM projet WHERE id_projet = ?";
        Connection conn = MyDatabase.getInstance().getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public void update(Projet projet) throws SQLException {
        String sql = "UPDATE projet SET titre=?, description=?, budget=?, statut=? WHERE id_projet=?";
        Connection conn = MyDatabase.getInstance().getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, projet.getTitre());
            stmt.setString(2, projet.getDescription());
            stmt.setDouble(3, projet.getBudget());
            stmt.setString(4, projet.getStatut());
            stmt.setInt(5, projet.getId_projet());
            stmt.executeUpdate();
        }
    }

    @Override
    public List<Projet> read() throws SQLException {
        List<Projet> projets = new ArrayList<>();
        String sql = "SELECT * FROM projet";
        Connection conn = MyDatabase.getInstance().getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                projets.add(new Projet(
                        rs.getInt("id_projet"),
                        rs.getString("titre"),
                        rs.getString("description"),
                        rs.getDouble("budget"),
                        rs.getString("statut")
                ));
            }
        }
        return projets;
    }

    public List<Projet> recuperer() {
        return List.of();
    }
}
