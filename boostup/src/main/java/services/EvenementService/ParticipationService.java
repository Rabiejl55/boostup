package services.EvenementService;

import entities.GEvenement.Participation;
import services.IService;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ParticipationService implements IService<Participation> {

    private Connection connection;

    public ParticipationService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Participation participation) throws SQLException {
        String req = "INSERT INTO participation (nom_startup, nom_investisseur, presence, date_inscription, id_evenement) " +
                "VALUES ('" + participation.getNomStartup() + "', '" + participation.getNomInvestisseur() + "', " +
                (participation.isPresence() ? 1 : 0) + ", '" + participation.getDateInscription() + "', " +
                participation.getIdEvenement() + ")";

        Statement st = connection.createStatement();
        st.executeUpdate(req);
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
}