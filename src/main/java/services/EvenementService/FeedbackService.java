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
                "VALUES (?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, feedback.getCommentaire());
            ps.setInt(2, feedback.getNote());
            ps.setDate(3, feedback.getDateFeedback());
            ps.setInt(4, feedback.getIdParticipation());
            ps.executeUpdate();
        }
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

    /**
     * Liste des participations avec un libellé lisible:
     * "{titre_evenement} - {nom_investisseur}"
     */
    public List<ParticipationRow> readParticipationsOptions() throws SQLException {
        List<ParticipationRow> list = new ArrayList<>();
        String req = "SELECT p.id_participation, e.titre AS evenement_titre, p.nom_investisseur " +
                "FROM participation p LEFT JOIN evenement e ON p.id_evenement = e.id_evenement " +
                "ORDER BY e.titre ASC, p.nom_investisseur ASC";

        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            ParticipationRow row = new ParticipationRow();
            row.idParticipation = rs.getInt("id_participation");
            row.evenementTitre = rs.getString("evenement_titre");
            row.nomInvestisseur = rs.getString("nom_investisseur");
            list.add(row);
        }
        return list;
    }

    /**
     * Lecture des feedbacks avec jointure pour obtenir un libellé lisible de la participation.
     */
    public List<FeedbackAvecParticipation> readAvecParticipation() throws SQLException {
        List<FeedbackAvecParticipation> list = new ArrayList<>();
        String req = "SELECT f.id_feedback, f.commentaire, f.note, f.date_feedback, f.id_participation, " +
                "e.titre AS evenement_titre, p.nom_investisseur " +
                "FROM feedback f " +
                "LEFT JOIN participation p ON f.id_participation = p.id_participation " +
                "LEFT JOIN evenement e ON p.id_evenement = e.id_evenement";

        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            FeedbackAvecParticipation row = new FeedbackAvecParticipation();
            row.idFeedback = rs.getInt("id_feedback");
            row.commentaire = rs.getString("commentaire");
            row.note = rs.getInt("note");
            row.dateFeedback = rs.getDate("date_feedback");
            row.idParticipation = rs.getInt("id_participation");
            row.evenementTitre = rs.getString("evenement_titre");
            row.nomInvestisseur = rs.getString("nom_investisseur");
            list.add(row);
        }
        return list;
    }

    public static class ParticipationRow {
        public int idParticipation;
        public String evenementTitre;
        public String nomInvestisseur;
    }

    public static class FeedbackAvecParticipation {
        public int idFeedback;
        public String commentaire;
        public int note;
        public Date dateFeedback;
        public int idParticipation;
        public String evenementTitre;
        public String nomInvestisseur;
    }
}