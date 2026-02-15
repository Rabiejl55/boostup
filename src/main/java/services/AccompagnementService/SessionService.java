package services.AccompagnementService;

import entities.GAccompagnement.Coach;
import entities.GAccompagnement.Session;
import utils.MyDatabase;

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
        String sql = "INSERT INTO session(date_session, duree, lieu, type_session, objectif, id_coach) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setDate(1, Date.valueOf(s.getDateSession()));
        ps.setInt(2, s.getDuree());
        ps.setString(3, s.getLieu());
        ps.setString(4, s.getTypeSession());
        ps.setString(5, s.getObjectif());
        ps.setInt(6, s.getCoach().getIdCoach());
        ps.executeUpdate();
        ps.close();
    }

    // ================= MODIFIER =================
    public void modifier(Session s) throws SQLException {
        String sql = "UPDATE session SET date_session=?, duree=?, lieu=?, type_session=?, objectif=?, id_coach=? WHERE id_session=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setDate(1, Date.valueOf(s.getDateSession()));
        ps.setInt(2, s.getDuree());
        ps.setString(3, s.getLieu());
        ps.setString(4, s.getTypeSession());
        ps.setString(5, s.getObjectif());
        ps.setInt(6, s.getCoach().getIdCoach());
        ps.setInt(7, s.getIdSession());
        ps.executeUpdate();
        ps.close();
    }

    // ================= SUPPRIMER =================
    public void supprimer(int idSession) throws SQLException {
        String sql = "DELETE FROM session WHERE id_session=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, idSession);
        ps.executeUpdate();
        ps.close();
    }

    // ================= AFFICHER =================
    public List<Session> afficherAll() throws SQLException {
        List<Session> liste = new ArrayList<>();

        String sql = """
        SELECT s.*, 
               c.id_coach, c.nom AS nom_coach, c.prenom, c.email, c.telephone, c.imagecoach
        FROM session s
        LEFT JOIN coach c ON s.id_coach = c.id_coach
        ORDER BY s.date_session DESC
    """;

        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Coach coach = null;
                if (rs.getInt("id_coach") != 0) {
                    coach = new Coach(
                            rs.getInt("id_coach"),
                            rs.getString("nom_coach"),
                            rs.getString("prenom"),
                            rs.getString("email"),
                            rs.getString("telephone"),
                            rs.getString("imagecoach")
                    );
                }

                Session s = new Session(
                        rs.getInt("id_session"),
                        rs.getDate("date_session").toLocalDate(),
                        rs.getInt("duree"),
                        rs.getString("lieu"),
                        rs.getString("type_session"),
                        rs.getString("objectif"),
                        coach
                );

                liste.add(s);
            }
        }

        return liste;
    }

}
