package services.UtilisateurService;

import entities.GUtilisateurs.User;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService {

    private final Connection connection;

    public UserService() {
        this.connection = MyDatabase.getInstance().getConnection();
    }

    // Créer un utilisateur
    public void ajouter(User user) throws SQLException {
        String query = "INSERT INTO user (nom, email, mdp, role, fullname, phone, avatar) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement stmt = connection.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            stmt.setString(1, user.getNom());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getMdp());
            stmt.setString(4, user.getRole());
            stmt.setString(5, user.getFullname());
            stmt.setString(6, user.getPhone());
            stmt.setString(7, user.getAvatar());
            stmt.executeUpdate();

            ResultSet rs = stmt.getGeneratedKeys();
            if (rs.next()) {
                user.setId(rs.getInt(1));
            }
        }
    }

    // Lire tous les utilisateurs
    public List<User> read() throws SQLException {
        List<User> users = new ArrayList<>();
        String query = "SELECT * FROM user";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                users.add(mapResultSetToUser(rs));
            }
        }
        return users;
    }

    // Trouver un utilisateur par ID
    public User findById(int id) throws SQLException {
        String query = "SELECT * FROM user WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }
        return null;
    }

    // Trouver un utilisateur par email
    public User findByEmail(String email) throws SQLException {
        String query = "SELECT * FROM user WHERE email = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, email);
            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }
        return null;
    }

    // Mettre à jour un utilisateur
    public void update(User user) throws SQLException {
        String query = "UPDATE user SET nom = ?, email = ?, mdp = ?, role = ?, fullname = ?, phone = ?, avatar = ?, active = ? WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, user.getNom());
            stmt.setString(2, user.getEmail());
            stmt.setString(3, user.getMdp());
            stmt.setString(4, user.getRole());
            stmt.setString(5, user.getFullname());
            stmt.setString(6, user.getPhone());
            stmt.setString(7, user.getAvatar());
            stmt.setBoolean(8, user.isActive());
            stmt.setInt(9, user.getId());
            stmt.executeUpdate();
        }
    }

    // Supprimer un utilisateur
    public void delete(int id) throws SQLException {
        String query = "DELETE FROM user WHERE id = ?";
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    // Mapper un ResultSet vers un User
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setNom(rs.getString("nom"));
        user.setEmail(rs.getString("email"));
        user.setMdp(rs.getString("mdp"));
        user.setRole(rs.getString("role"));
        user.setFullname(rs.getString("fullname"));
        user.setPhone(rs.getString("phone"));
        user.setAvatar(rs.getString("avatar"));
        user.setActive(rs.getBoolean("active"));

        Timestamp timestamp = rs.getTimestamp("date_creation");
        if (timestamp != null) {
            user.setDateCreation(timestamp.toLocalDateTime());
        }

        return user;
    }
}
