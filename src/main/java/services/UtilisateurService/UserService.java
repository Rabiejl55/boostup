package services.UtilisateurService;

import entities.GUtilisateurs.User;
import entities.Role_enum;
import services.IService;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserService implements IService<User> {

    private Connection connection;

    public UserService() throws SQLException {
        connection = MyDatabase.getInstance().getConnection();
        System.out.println("Connection established for UserService");
        initializeRoles();
        ensureProfileColumnsExist();
    }

    private void initializeRoles() {
        try {
            for (Role_enum role : Role_enum.values()) {
                String roleName = role.name();
                String checkSql = "SELECT COUNT(*) FROM role_enum WHERE role_name = ?";
                PreparedStatement checkStmt = connection.prepareStatement(checkSql);
                checkStmt.setString(1, roleName);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next() && rs.getInt(1) == 0) {
                    String insertSql = "INSERT INTO role_enum (role_name) VALUES (?)";
                    PreparedStatement insertStmt = connection.prepareStatement(insertSql);
                    insertStmt.setString(1, roleName);
                    insertStmt.executeUpdate();
                    insertStmt.close();
                    System.out.println("Rôle ajouté: " + roleName);
                }

                rs.close();
                checkStmt.close();
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'initialisation des rôles: " + e.getMessage());
        }
    }

    private void ensureProfileColumnsExist() {
        try {
            // Vérifier si les colonnes du profil existent, sinon les créer
            String[] columns = {"fullname", "phone", "avatar"};
            for (String column : columns) {
                String checkSql = "SELECT COUNT(*) FROM information_schema.columns " +
                        "WHERE table_name = 'user' AND column_name = ?";
                PreparedStatement checkStmt = connection.prepareStatement(checkSql);
                checkStmt.setString(1, column);
                ResultSet rs = checkStmt.executeQuery();

                if (rs.next() && rs.getInt(1) == 0) {
                    String alterSql = "";
                    switch (column) {
                        case "fullname":
                            alterSql = "ALTER TABLE user ADD COLUMN fullname VARCHAR(200) AFTER email";
                            break;
                        case "phone":
                            alterSql = "ALTER TABLE user ADD COLUMN phone VARCHAR(20) AFTER fullname";
                            break;
                        case "avatar":
                            alterSql = "ALTER TABLE user ADD COLUMN avatar VARCHAR(500) AFTER phone";
                            break;
                    }
                    Statement stmt = connection.createStatement();
                    stmt.executeUpdate(alterSql);
                    stmt.close();
                    System.out.println("Colonne " + column + " ajoutée à la table user");
                }
                rs.close();
                checkStmt.close();
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification des colonnes: " + e.getMessage());
        }
    }

    @Override
    public void ajouter(User user) throws SQLException {
        // Vérifier si l'email existe déjà
        String checkSql = "SELECT id FROM user WHERE email = ?";
        PreparedStatement checkStmt = connection.prepareStatement(checkSql);
        checkStmt.setString(1, user.getEmail());
        ResultSet rs = checkStmt.executeQuery();

        if (rs.next()) {
            // Close resources then throw SQLException so callers can react
            rs.close();
            checkStmt.close();
            throw new SQLException("L'email '" + user.getEmail() + "' existe déjà");
        }
        rs.close();
        checkStmt.close();

        // Insérer l'utilisateur avec toutes les informations (y compris le profil)
        String req = "INSERT INTO user (nom, email, MDP, role, active, date_creation, " +
                "fullname, phone, avatar) VALUES(?, ?, ?, ?, ?, NOW(), ?, ?, ?)";
        PreparedStatement preparedStatement = connection.prepareStatement(req);
        preparedStatement.setString(1, user.getNom());
        preparedStatement.setString(2, user.getEmail());
        preparedStatement.setString(3, user.getMDP());
        preparedStatement.setString(4, user.getRole().name());
        preparedStatement.setBoolean(5, user.isActive());
        preparedStatement.setString(6, user.getFullname());
        preparedStatement.setString(7, user.getPhone());
        preparedStatement.setString(8, user.getAvatar());

        preparedStatement.executeUpdate();
        System.out.println("User added successfully with profile information.");
        preparedStatement.close();
    }

    @Override
    public void update(User user) throws SQLException {
        String sql = "UPDATE user SET nom = ?, email = ?, MDP = ?, role = ?, active = ?, " +
                "fullname = ?, phone = ?, avatar = ? WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, user.getNom());
        preparedStatement.setString(2, user.getEmail());
        preparedStatement.setString(3, user.getMDP());
        preparedStatement.setString(4, user.getRole().name());
        preparedStatement.setBoolean(5, user.isActive());
        preparedStatement.setString(6, user.getFullname());
        preparedStatement.setString(7, user.getPhone());
        preparedStatement.setString(8, user.getAvatar());
        preparedStatement.setInt(9, user.getId());

        int rowsUpdated = preparedStatement.executeUpdate();
        if (rowsUpdated > 0) {
            System.out.println("User and profile updated successfully.");
        } else {
            System.out.println("No user found with ID: " + user.getId());
        }
        preparedStatement.close();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM user WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, id);

        int rowsDeleted = preparedStatement.executeUpdate();
        if (rowsDeleted > 0) {
            System.out.println("User and associated profile deleted successfully.");
        } else {
            System.out.println("No user found with id: " + id);
        }
        preparedStatement.close();
    }

    @Override
    public List<User> read() throws SQLException {
        String sql = "SELECT * FROM user ORDER BY date_creation DESC";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        List<User> users = new ArrayList<>();
        while (rs.next()) {
            User u = createUserFromResultSet(rs);
            users.add(u);
        }

        rs.close();
        statement.close();
        System.out.println("Found " + users.size() + " users.");
        return users;
    }

    // Méthode pour créer un User depuis un ResultSet
    private User createUserFromResultSet(ResultSet rs) throws SQLException {
        User u = new User();
        u.setId(rs.getInt("id"));
        u.setNom(rs.getString("nom"));
        u.setEmail(rs.getString("email"));
        u.setMDP(rs.getString("MDP"));
        u.setRole(Role_enum.valueOf(rs.getString("role")));
        u.setActive(rs.getBoolean("active"));
        u.setDateCreation(rs.getTimestamp("date_creation"));

        // Ajout des champs du profil
        u.setFullname(rs.getString("fullname"));
        u.setPhone(rs.getString("phone"));
        u.setAvatar(rs.getString("avatar"));

        return u;
    }

    public User findByEmail(String email) throws SQLException {
        String sql = "SELECT * FROM user WHERE email = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, email);
        ResultSet rs = preparedStatement.executeQuery();

        User user = null;
        if (rs.next()) {
            user = createUserFromResultSet(rs);
        }

        rs.close();
        preparedStatement.close();

        if (user != null) {
            System.out.println("User found: " + user.getNom() + " (" + user.getEmail() + ")");
        } else {
            System.out.println("No user found with email: " + email);
        }

        return user;
    }

    public User findById(int id) throws SQLException {
        String sql = "SELECT * FROM user WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, id);
        ResultSet rs = preparedStatement.executeQuery();

        User user = null;
        if (rs.next()) {
            user = createUserFromResultSet(rs);
        }

        rs.close();
        preparedStatement.close();

        if (user != null) {
            System.out.println("User found: " + user.getNom() + " (ID: " + user.getId() + ")");
        } else {
            System.out.println("No user found with ID: " + id);
        }

        return user;
    }

    public User seConnecter(String email, String password) throws SQLException {
        String sql = "SELECT * FROM user WHERE email = ? AND MDP = ? AND active = TRUE";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, email);
        preparedStatement.setString(2, password);
        ResultSet rs = preparedStatement.executeQuery();

        User user = null;
        if (rs.next()) {
            user = createUserFromResultSet(rs);
            System.out.println("Login successful for: " + user.getNom() +
                    " (" + user.getRole() + ")" +
                    " - Fullname: " + user.getFullname());
        } else {
            System.out.println("Login failed for email: " + email);
        }

        rs.close();
        preparedStatement.close();
        return user;
    }

    public boolean changerStatut(int userId, boolean active) throws SQLException {
        String sql = "UPDATE user SET active = ? WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setBoolean(1, active);
        preparedStatement.setInt(2, userId);

        int rowsUpdated = preparedStatement.executeUpdate();
        preparedStatement.close();

        if (rowsUpdated > 0) {
            System.out.println("Status changed successfully for user ID: " + userId);
            return true;
        } else {
            System.out.println("No user found with ID: " + userId);
            return false;
        }
    }

    public List<Role_enum> getAllRoles() throws SQLException {
        String sql = "SELECT role_name FROM role_enum ORDER BY role_name";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        List<Role_enum> roles = new ArrayList<>();
        while (rs.next()) {
            roles.add(Role_enum.valueOf(rs.getString("role_name")));
        }

        rs.close();
        statement.close();
        System.out.println("Found " + roles.size() + " roles.");
        return roles;
    }

    public List<User> findByRole(Role_enum role) throws SQLException {
        String sql = "SELECT * FROM user WHERE role = ? ORDER BY nom";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, role.name());
        ResultSet rs = preparedStatement.executeQuery();

        List<User> users = new ArrayList<>();
        while (rs.next()) {
            User u = createUserFromResultSet(rs);
            users.add(u);
        }

        rs.close();
        preparedStatement.close();
        System.out.println("Found " + users.size() + " users with role " + role);
        return users;
    }

    // Méthodes pour gérer spécifiquement le profil (anciennement dans UserProfileService)

    public void updateProfile(User user) throws SQLException {
        String sql = "UPDATE user SET fullname = ?, phone = ?, avatar = ? WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, user.getFullname());
        preparedStatement.setString(2, user.getPhone());
        preparedStatement.setString(3, user.getAvatar());
        preparedStatement.setInt(4, user.getId());

        int rowsUpdated = preparedStatement.executeUpdate();
        if (rowsUpdated > 0) {
            System.out.println("Profile updated successfully for user ID: " + user.getId());
        } else {
            System.out.println("No user found with ID: " + user.getId());
        }
        preparedStatement.close();
    }

    public boolean updateProfilePartial(int userId, String fullname, String phone, String avatar) throws SQLException {
        StringBuilder sqlBuilder = new StringBuilder("UPDATE user SET ");
        List<Object> params = new ArrayList<>();

        if (fullname != null) {
            sqlBuilder.append("fullname = ?, ");
            params.add(fullname);
        }
        if (phone != null) {
            sqlBuilder.append("phone = ?, ");
            params.add(phone);
        }
        if (avatar != null) {
            sqlBuilder.append("avatar = ?, ");
            params.add(avatar);
        }

        if (params.size() > 0) {
            sqlBuilder.setLength(sqlBuilder.length() - 2);
        } else {
            System.out.println("Aucune donnée de profil à mettre à jour!");
            return false;
        }

        sqlBuilder.append(" WHERE id = ?");
        params.add(userId);

        PreparedStatement preparedStatement = connection.prepareStatement(sqlBuilder.toString());

        for (int i = 0; i < params.size(); i++) {
            preparedStatement.setObject(i + 1, params.get(i));
        }

        int rowsUpdated = preparedStatement.executeUpdate();
        preparedStatement.close();

        if (rowsUpdated > 0) {
            System.out.println("Profile partially updated successfully for user ID: " + userId);
            return true;
        } else {
            System.out.println("No user found with ID: " + userId);
            return false;
        }
    }

    public boolean hasProfile(int userId) throws SQLException {
        String sql = "SELECT COUNT(*) FROM user WHERE id = ? AND " +
                "(fullname IS NOT NULL OR phone IS NOT NULL OR avatar IS NOT NULL)";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setInt(1, userId);
        ResultSet rs = preparedStatement.executeQuery();

        boolean hasProfile = false;
        if (rs.next()) {
            hasProfile = rs.getInt(1) > 0;
        }

        rs.close();
        preparedStatement.close();
        return hasProfile;
    }

    public List<User> findUsersWithProfile() throws SQLException {
        String sql = "SELECT * FROM user WHERE fullname IS NOT NULL OR phone IS NOT NULL OR avatar IS NOT NULL " +
                "ORDER BY nom";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        List<User> users = new ArrayList<>();
        while (rs.next()) {
            User u = createUserFromResultSet(rs);
            users.add(u);
        }

        rs.close();
        statement.close();
        System.out.println("Found " + users.size() + " users with profile information.");
        return users;
    }

    public int countUsersWithProfile() throws SQLException {
        String sql = "SELECT COUNT(*) FROM user WHERE " +
                "(fullname IS NOT NULL OR phone IS NOT NULL OR avatar IS NOT NULL)";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        int count = 0;
        if (rs.next()) {
            count = rs.getInt(1);
        }

        rs.close();
        statement.close();
        System.out.println("Total users with profile: " + count);
        return count;
    }

    public List<User> searchUsers(String keyword) throws SQLException {
        String sql = "SELECT * FROM user WHERE " +
                "(nom LIKE ? OR email LIKE ? OR fullname LIKE ? OR phone LIKE ?) " +
                "ORDER BY nom";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        String likeKeyword = "%" + keyword + "%";
        preparedStatement.setString(1, likeKeyword);
        preparedStatement.setString(2, likeKeyword);
        preparedStatement.setString(3, likeKeyword);
        preparedStatement.setString(4, likeKeyword);

        ResultSet rs = preparedStatement.executeQuery();

        List<User> users = new ArrayList<>();
        while (rs.next()) {
            User u = createUserFromResultSet(rs);
            users.add(u);
        }

        rs.close();
        preparedStatement.close();
        System.out.println("Found " + users.size() + " users matching keyword: " + keyword);
        return users;
    }

    public List<User> findActiveUsers() throws SQLException {
        String sql = "SELECT * FROM user WHERE active = TRUE ORDER BY nom";
        Statement statement = connection.createStatement();
        ResultSet rs = statement.executeQuery(sql);

        List<User> users = new ArrayList<>();
        while (rs.next()) {
            User u = createUserFromResultSet(rs);
            users.add(u);
        }

        rs.close();
        statement.close();
        System.out.println("Found " + users.size() + " active users.");
        return users;
    }

    public boolean updateAvatar(int userId, String avatarUrl) throws SQLException {
        String sql = "UPDATE user SET avatar = ? WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, avatarUrl);
        preparedStatement.setInt(2, userId);

        int rowsUpdated = preparedStatement.executeUpdate();
        preparedStatement.close();

        if (rowsUpdated > 0) {
            System.out.println("Avatar updated successfully for user ID: " + userId);
            return true;
        } else {
            System.out.println("No user found with ID: " + userId);
            return false;
        }
    }

    public boolean updatePhone(int userId, String phone) throws SQLException {
        String sql = "UPDATE user SET phone = ? WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, phone);
        preparedStatement.setInt(2, userId);

        int rowsUpdated = preparedStatement.executeUpdate();
        preparedStatement.close();

        if (rowsUpdated > 0) {
            System.out.println("Phone updated successfully for user ID: " + userId);
            return true;
        } else {
            System.out.println("No user found with ID: " + userId);
            return false;
        }
    }

    public boolean updateFullname(int userId, String fullname) throws SQLException {
        String sql = "UPDATE user SET fullname = ? WHERE id = ?";
        PreparedStatement preparedStatement = connection.prepareStatement(sql);
        preparedStatement.setString(1, fullname);
        preparedStatement.setInt(2, userId);

        int rowsUpdated = preparedStatement.executeUpdate();
        preparedStatement.close();

        if (rowsUpdated > 0) {
            System.out.println("Fullname updated successfully for user ID: " + userId);
            return true;
        } else {
            System.out.println("No user found with ID: " + userId);
            return false;
        }
    }
}