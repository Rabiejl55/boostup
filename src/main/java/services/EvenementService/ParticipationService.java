package services.EvenementService;

import entities.GEvenement.Participation;
import services.IService;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipationService implements IService<Participation> {

    private Connection connection;

    public ParticipationService() throws SQLException {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Participation participation) throws SQLException {
        String req = "INSERT INTO participation (nom_startup, nom_investisseur, presence, date_inscription, id_evenement) " +
                "VALUES (?, ?, ?, ?, ?)";

        try (PreparedStatement ps = connection.prepareStatement(req)) {
            ps.setString(1, participation.getNomStartup());
            ps.setString(2, participation.getNomInvestisseur());
            ps.setBoolean(3, participation.isPresence());
            ps.setDate(4, participation.getDateInscription());
            ps.setInt(5, participation.getIdEvenement());
            ps.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM participation WHERE id_participation = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public void update(Participation participation) throws SQLException {
        String req = "UPDATE participation SET nom_startup = ?, nom_investisseur = ?, " +
                "presence = ?, date_inscription = ?, id_evenement = ? WHERE id_participation = ?";

        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, participation.getNomStartup());
        ps.setString(2, participation.getNomInvestisseur());
        ps.setBoolean(3, participation.isPresence());
        ps.setDate(4, participation.getDateInscription());
        ps.setInt(5, participation.getIdEvenement());
        ps.setInt(6, participation.getId());

        ps.executeUpdate();
    }

    @Override
    public List<Participation> read() throws SQLException {
        List<Participation> participations = new ArrayList<>();
        String req = "SELECT * FROM participation";

        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Participation p = new Participation();
            p.setId(rs.getInt("id_participation"));
            p.setNomStartup(rs.getString("nom_startup"));
            p.setNomInvestisseur(rs.getString("nom_investisseur"));
            p.setPresence(rs.getBoolean("presence"));
            p.setDateInscription(rs.getDate("date_inscription"));
            p.setIdEvenement(rs.getInt("id_evenement"));

            participations.add(p);
        }
        return participations;
    }

    /**
     * Liste des événements (id+titre), pour alimenter la ChoiceBox.
     */
    public List<EvenementRow> readEvenements() throws SQLException {
        List<EvenementRow> list = new ArrayList<>();
        String req = "SELECT id_evenement, titre FROM evenement ORDER BY titre ASC";
        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);
        while (rs.next()) {
            list.add(new EvenementRow(rs.getInt("id_evenement"), rs.getString("titre")));
        }
        return list;
    }

    /**
     * Lecture avec jointure pour pouvoir afficher le titre de l'événement lié.
     */
    public List<ParticipationAvecEvenement> readAvecEvenement() throws SQLException {
        List<ParticipationAvecEvenement> participations = new ArrayList<>();
        String req = "SELECT p.id_participation, p.nom_startup, p.nom_investisseur, p.presence, p.date_inscription, " +
                "p.id_evenement, e.titre AS evenement_titre " +
                "FROM participation p LEFT JOIN evenement e ON p.id_evenement = e.id_evenement";

        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            ParticipationAvecEvenement p = new ParticipationAvecEvenement();
            p.idParticipation = rs.getInt("id_participation");
            p.nomStartup = rs.getString("nom_startup");
            p.nomInvestisseur = rs.getString("nom_investisseur");
            p.presence = rs.getBoolean("presence");
            p.dateInscription = rs.getDate("date_inscription");
            p.idEvenement = rs.getInt("id_evenement");
            p.titreEvenement = rs.getString("evenement_titre");
            participations.add(p);
        }
        return participations;
    }

    // Petites structures de retour (pour éviter de créer plein d'entités)
    public static class EvenementRow {
        public final int id;
        public final String titre;

        public EvenementRow(int id, String titre) {
            this.id = id;
            this.titre = titre;
        }
    }

    public static class ParticipationAvecEvenement {
        public int idParticipation;
        public String nomStartup;
        public String nomInvestisseur;
        public boolean presence;
        public Date dateInscription;
        public int idEvenement;
        public String titreEvenement;
    }
}