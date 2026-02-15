package services.AccompagnementService;

import entities.GAccompagnement.Coach;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CoachService {

    private final Connection conn;

    public CoachService() {
        conn = MyDatabase.getInstance().getConnection();
    }

    // Ajouter un coach avec imagecoach
    public void ajouter(Coach coach) throws SQLException {
        String sql = "INSERT INTO Coach(nom, prenom, email, telephone, imagecoach) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, coach.getNom());
            ps.setString(2, coach.getPrenom());
            ps.setString(3, coach.getEmail());
            ps.setString(4, coach.getTelephone());
            ps.setString(5, coach.getImagecoach());
            ps.executeUpdate();
        }
    }

    // Modifier un coach avec imagecoach
    public void modifier(Coach coach) throws SQLException {
        String sql = "UPDATE Coach SET nom=?, prenom=?, email=?, telephone=?, imagecoach=? WHERE id_coach=?";
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

    // Supprimer un coach
    public void supprimer(int idCoach) throws SQLException {
        String sql = "DELETE FROM Coach WHERE id_coach=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idCoach);
            ps.executeUpdate();
        }
    }

    // Afficher tous les coaches
    public List<Coach> afficherAll() throws SQLException {
        List<Coach> liste = new ArrayList<>();
        String sql = "SELECT * FROM Coach";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                Coach c = new Coach(
                        rs.getInt("id_coach"),
                        rs.getString("nom"),
                        rs.getString("prenom"),
                        rs.getString("email"),
                        rs.getString("telephone"),
                        rs.getString("imagecoach")
                );
                liste.add(c);
            }
        }
        return liste;
    }

    // ===== Vérifications d'unicité =====
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
        String query = "SELECT COUNT(*) FROM coach WHERE imagecoach = ?";
        try (PreparedStatement pst = conn.prepareStatement(query)) {
            pst.setString(1, imagePath);
            try (ResultSet rs = pst.executeQuery()) {
                if (rs.next()) return rs.getInt(1) > 0;
            }
        }
        return false;
    }

}
