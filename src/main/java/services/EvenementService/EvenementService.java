package services.EvenementService;

import entities.GEvenement.Evenement;
import services.IService;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EvenementService implements IService<Evenement> {

    private Connection connection;

    public EvenementService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    @Override
    public void ajouter(Evenement evenement) throws SQLException {
        // On inclut image (nullable)
        String req = "INSERT INTO evenement (titre, type, date_evenement, lieu, description, capacite_max, image) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, evenement.getTitre());
        ps.setString(2, evenement.getType());
        ps.setDate(3, evenement.getDateEvenement());
        ps.setString(4, evenement.getLieu());
        ps.setString(5, evenement.getDescription());
        ps.setInt(6, evenement.getCapaciteMax());
        ps.setString(7, evenement.getImage());

        ps.executeUpdate();
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String req = "DELETE FROM evenement WHERE id_evenement = ?";
        PreparedStatement ps = connection.prepareStatement(req);
        ps.setInt(1, id);
        ps.executeUpdate();
    }

    @Override
    public void update(Evenement evenement) throws SQLException {
        String req = "UPDATE evenement SET titre = ?, type = ?, date_evenement = ?, " +
                "lieu = ?, description = ?, capacite_max = ?, image = ? WHERE id_evenement = ?";

        PreparedStatement ps = connection.prepareStatement(req);
        ps.setString(1, evenement.getTitre());
        ps.setString(2, evenement.getType());
        ps.setDate(3, evenement.getDateEvenement());
        ps.setString(4, evenement.getLieu());
        ps.setString(5, evenement.getDescription());
        ps.setInt(6, evenement.getCapaciteMax());
        ps.setString(7, evenement.getImage());
        ps.setInt(8, evenement.getId());

        ps.executeUpdate();
    }

    @Override
    public List<Evenement> read() throws SQLException {
        List<Evenement> evenements = new ArrayList<>();
        String req = "SELECT * FROM evenement";

        Statement st = connection.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Evenement e = new Evenement();
            e.setId(rs.getInt("id_evenement"));
            e.setTitre(rs.getString("titre"));
            e.setType(rs.getString("type"));
            e.setDateEvenement(rs.getDate("date_evenement"));
            e.setLieu(rs.getString("lieu"));
            e.setDescription(rs.getString("description"));
            e.setCapaciteMax(rs.getInt("capacite_max"));
            // Nouvelle colonne
            try {
                e.setImage(rs.getString("image"));
            } catch (SQLException ignored) {
                // Si la colonne n'existe pas (DB pas migrée), on laisse null
            }

            evenements.add(e);
        }
        return evenements;
    }
}