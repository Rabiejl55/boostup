package services.AccompagnementService;

import entities.GAccompagnement.Coach;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CoachService {

    private final Connection conn;

    public CoachService() throws SQLException {
        conn = MyDatabase.getInstance().getConnection();
    }

    public void ajouter(Coach coach) throws SQLException {
        // Utiliser le nom de colonne réel en base : `image`
        String sql = "INSERT INTO coach(nom, prenom, email, telephone, image) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, coach.getNom());
            ps.setString(2, coach.getPrenom());
            ps.setString(3, coach.getEmail());
            ps.setString(4, coach.getTelephone());
            ps.setString(5, coach.getImagecoach());
            ps.executeUpdate();
        }
    }

    public void modifier(Coach coach) throws SQLException {
        String sql = "UPDATE coach SET nom=?, prenom=?, email=?, telephone=?, image=? WHERE id_coach=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, coach.getNom());
            ps.setString(2, coach.getPrenom());
            ps.setString(3, coach.getEmail());
            ps.setString(4, coach.getTelephone());
            ps.setString(5, coach.getImagecoach());
            ps.setInt(6, coach.getIdCoach());
            ps.executeUpdate();
        }
    }

    public void supprimer(int idCoach) throws SQLException {
        String sql = "DELETE FROM coach WHERE id_coach=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCoach);
            ps.executeUpdate();
        }
    }

    public List<Coach> afficherAll() throws SQLException {
        List<Coach> liste = new ArrayList<>();
        String sql = "SELECT * FROM coach";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Coach c = new Coach(
                        rs.getInt("id_coach"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("telephone"),
                        rs.getString("image")      // mappe la colonne `image` vers le champ imagecoach
                );
                liste.add(c);
            }
        }
        return liste;
    }

    public boolean emailExiste(String email) throws SQLException {
        String query = "SELECT COUNT(*) FROM coach WHERE email = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, email);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public boolean telephoneExiste(String telephone) throws SQLException {
        String query = "SELECT COUNT(*) FROM coach WHERE telephone = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, telephone);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

    public boolean imageExiste(String imagePath) throws SQLException {
        String query = "SELECT COUNT(*) FROM coach WHERE image = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, imagePath);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }
    public List<Coach> rechercherParNom(String nom) throws SQLException {
        List<Coach> liste = new ArrayList<>();

        String sql = "SELECT * FROM coach WHERE LOWER(nom) LIKE ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, "%" + nom.toLowerCase() + "%");

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Coach c = new Coach(
                            rs.getInt("id_coach"),
                            rs.getString("nom"),
                            rs.getString("prenom"),
                            rs.getString("email"),
                            rs.getString("telephone"),
                            rs.getString("image")   // idem : colonne `image`
                    );
                    liste.add(c);
                }
            }
        }

        return liste;
    }
}
