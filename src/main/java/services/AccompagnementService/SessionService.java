package services.AccompagnementService;
import java.time.LocalDate;
import entities.GAccompagnement.Coach;
import entities.GAccompagnement.Domaine;
import entities.GAccompagnement.Session;
import utils.MyDatabase;
import java.util.stream.Collectors;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class SessionService {

    private final Connection conn;

    public SessionService() {
        conn = MyDatabase.getInstance().getConnection();
    }

    // ================= AJOUTER =================
    public void ajouter(Session s) throws SQLException {
        String sql = "INSERT INTO session(date_session, duree, lieu, type_session, objectif, id_coach, id_domaine) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(s.getDateSession()));
            ps.setInt(2, s.getDuree());
            ps.setString(3, s.getLieu());
            ps.setString(4, s.getTypeSession());
            ps.setString(5, s.getObjectif());
            ps.setInt(6, s.getCoach() != null ? s.getCoach().getIdCoach() : 0);
            ps.setInt(7, s.getDomaine() != null ? s.getDomaine().getId() : 0);
            ps.executeUpdate();
        }
    }

    // ================= MODIFIER =================
    public void modifier(Session s) throws SQLException {
        String sql = "UPDATE session SET date_session=?, duree=?, lieu=?, type_session=?, objectif=?, id_coach=?, id_domaine=? " +
                "WHERE id_session=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDate(1, Date.valueOf(s.getDateSession()));
            ps.setInt(2, s.getDuree());
            ps.setString(3, s.getLieu());
            ps.setString(4, s.getTypeSession());
            ps.setString(5, s.getObjectif());
            ps.setInt(6, s.getCoach() != null ? s.getCoach().getIdCoach() : 0);
            ps.setInt(7, s.getDomaine() != null ? s.getDomaine().getId() : 0);
            ps.setInt(8, s.getIdSession());
            ps.executeUpdate();
        }
    }

    // ================= SUPPRIMER =================
    public void supprimer(int idSession) throws SQLException {
        String sql = "DELETE FROM session WHERE id_session=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idSession);
            ps.executeUpdate();
        }
    }

    // ================= AFFICHER =================
    public List<Session> afficherAll() throws SQLException {
        List<Session> liste = new ArrayList<>();

        String sql = """
            SELECT s.*, 
                   c.id_coach, c.nom AS nom_coach, c.prenom,
                   d.nom AS nom_domaine
            FROM session s
            LEFT JOIN coach c ON s.id_coach = c.id_coach
            LEFT JOIN domaine d ON s.id_domaine = d.id
            ORDER BY s.date_session DESC
        """;

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                // Coach
                Coach coach = null;
                if (rs.getInt("id_coach") != 0) {
                    coach = new Coach(
                            rs.getInt("id_coach"),
                            rs.getString("nom_coach"),
                            rs.getString("prenom"),
                            null, null, null
                    );
                }
                Domaine domaine = null;
                if (rs.getString("nom_domaine") != null) {
                    domaine = new Domaine();
                    domaine.setNom(rs.getString("nom_domaine"));
                }
                Session s = new Session(
                        rs.getInt("id_session"),
                        rs.getDate("date_session").toLocalDate(),
                        rs.getInt("duree"),
                        rs.getString("lieu"),
                        rs.getString("type_session"),
                        rs.getString("objectif"),
                        coach,
                        domaine
                );

                liste.add(s);
            }
        }

        return liste;
    }
    public List<Session> rechercherParNom(String motCle) throws SQLException {
        List<Session> liste = new ArrayList<>();

        String req = "SELECT * FROM session WHERE LOWER(objectif) LIKE LOWER(?)";

        PreparedStatement ps = conn.prepareStatement(req);
        ps.setString(1, "%" + motCle + "%");

        ResultSet rs = ps.executeQuery();

        while (rs.next()) {
            Session s = new Session();
            s.setIdSession(rs.getInt("id_session"));
            s.setObjectif(rs.getString("objectif"));
            s.setLieu(rs.getString("lieu"));
            s.setDuree(rs.getInt("duree"));
            s.setDateSession(rs.getDate("date_session").toLocalDate());

            liste.add(s);
        }

        return liste;
    }
    // ================= PARTICIPER AVEC AUTO-ACCEPTATION =================
    public void participerAvecAutoAcceptation(int sessionId, int userId) throws SQLException {
        // 1️⃣ Récupérer l'email depuis la table user
        String email = null;
        String sqlEmail = "SELECT email FROM user WHERE id_user = ?";
        try (PreparedStatement ps = conn.prepareStatement(sqlEmail)) {
            ps.setInt(1, userId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                email = rs.getString("email");
            }
        }

        if (email == null) {
            throw new SQLException("Utilisateur introuvable !");
        }

        // 2️⃣ Vérifier le domaine de l'email
        String statut = email.endsWith("@boostup.tn") ? "accepté" : "en_attente";

        // 3️⃣ Ajouter le participant à la session avec le statut
        String sqlInsert = "INSERT INTO session_participant(id_session, id_user, statut) VALUES (?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sqlInsert)) {
            ps.setInt(1, sessionId);
            ps.setInt(2, userId);
            ps.setString(3, statut);
            ps.executeUpdate();
        }

    }
    public void participerSession(int idSession, int idUser) {
        String sql = "SELECT email FROM user WHERE id_user = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idUser);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                String email = rs.getString("email");
                boolean estBoostup = email.endsWith("@boostup.tn");

                String statut = estBoostup ? "accepté" : "en attente";

                String insert = "INSERT INTO participation(id_session, id_user, statut) VALUES (?, ?, ?)";
                try (PreparedStatement ps2 = conn.prepareStatement(insert)) {
                    ps2.setInt(1, idSession);
                    ps2.setInt(2, idUser);
                    ps2.setString(3, statut);
                    ps2.executeUpdate();
                }

                System.out.println("Participation ajoutée avec statut: " + statut);
            }
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
}
