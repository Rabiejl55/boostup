package services.AccompagnementService;

import entities.GAccompagnement.Domaine;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DomaineService {

    private final Connection conn;

    public DomaineService() throws SQLException {
        conn = MyDatabase.getInstance().getConnection();
    }

    // Ajouter un domaine
    public void ajouter(Domaine domaine) throws SQLException {
        String sql = "INSERT INTO domaine(nom, description, niveau, statut, image) VALUES (?, ?, ?, ?, ?)";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, domaine.getNom());
        ps.setString(2, domaine.getDescription()); // On ne récupère plus la description depuis Wikipedia
        ps.setString(3, domaine.getNiveau());
        ps.setString(4, domaine.getStatut());
        ps.setString(5, domaine.getImage());
        ps.executeUpdate();
        ps.close();
    }

    // Modifier un domaine existant
    public void modifier(Domaine domaine) throws SQLException {
        String sql = "UPDATE domaine SET nom=?, description=?, niveau=?, statut=?, image=? WHERE id_domaine=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setString(1, domaine.getNom());
        ps.setString(2, domaine.getDescription());
        ps.setString(3, domaine.getNiveau());
        ps.setString(4, domaine.getStatut());
        ps.setString(5, domaine.getImage());
        ps.setInt(6, domaine.getId());
        ps.executeUpdate();
        ps.close();
    }

    // Supprimer un domaine
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM domaine WHERE id_domaine=?";
        PreparedStatement ps = conn.prepareStatement(sql);
        ps.setInt(1, id);
        ps.executeUpdate();
        ps.close();
    }

    // Afficher tous les domaines
    public List<Domaine> afficherAll() throws SQLException {
        List<Domaine> liste = new ArrayList<>();
        String sql = "SELECT * FROM domaine";
        Statement st = conn.createStatement();
        ResultSet rs = st.executeQuery(sql);

        while (rs.next()) {
            Domaine d = new Domaine(
                    rs.getInt("id_domaine"),
                    rs.getString("nom"),
                    rs.getString("description"),
                    rs.getString("niveau"),
                    rs.getString("statut"),
                    rs.getString("image")
            );
            liste.add(d);
        }

        rs.close();
        st.close();
        return liste;
    }

    // Rechercher domaine par nom
    public List<Domaine> rechercherParNom(String motCle) throws SQLException {
        List<Domaine> result = new ArrayList<>();
        String sql = "SELECT * FROM domaine WHERE LOWER(nom) LIKE ?";
        PreparedStatement pst = conn.prepareStatement(sql);
        pst.setString(1, "%" + motCle.toLowerCase() + "%");
        ResultSet rs = pst.executeQuery();

        while (rs.next()) {
            Domaine d = new Domaine(
                    rs.getInt("id_domaine"),
                    rs.getString("nom"),
                    rs.getString("description"),
                    rs.getString("niveau"),
                    rs.getString("statut"),
                    rs.getString("image")
            );
            result.add(d);
        }

        rs.close();
        pst.close();
        return result;
    }
}