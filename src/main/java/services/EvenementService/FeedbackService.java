package services.EvenementService;

import entities.GEvenement.Feedback;
import services.IService;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FeedbackService implements IService<Feedback> {

    private Connection connection;

    public FeedbackService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Feedback feedback) throws SQLException {
        String req = "INSERT INTO feedback (commentaire, note, date_feedback, id_participation) " +
                "VALUES ('" + feedback.getCommentaire() + "', " + feedback.getNote() + ", '" +
                feedback.getDateFeedback() + "', " + feedback.getIdParticipation() + ")";

        Statement st = connection.createStatement();
        st.executeUpdate(req);
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM feedback WHERE id_feedback = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public void update(Feedback feedback) throws SQLException {
        String req = "UPDATE feedback SET commentaire = ?, note = ?, " +
                "date_feedback = ?, id_participation = ? WHERE id_feedback = ?";

        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, feedback.getCommentaire());
        ps.setInt(2, feedback.getNote());
        ps.setDate(3, feedback.getDateFeedback());
        ps.setInt(4, feedback.getIdParticipation());
        ps.setInt(5, feedback.getId());

        ps.executeUpdate();
    }

    @Override
    public List<Feedback> read() throws SQLException {
        List<Feedback> feedbacks = new ArrayList<>();
        String req = "SELECT * FROM feedback";

        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Feedback f = new Feedback();
            f.setId(rs.getInt("id_feedback"));
            f.setCommentaire(rs.getString("commentaire"));
            f.setNote(rs.getInt("note"));
            f.setDateFeedback(rs.getDate("date_feedback"));
            f.setIdParticipation(rs.getInt("id_participation"));

            feedbacks.add(f);
        }
        return feedbacks;
    }
}