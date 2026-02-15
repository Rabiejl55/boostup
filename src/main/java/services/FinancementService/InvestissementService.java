package services.FinancementService;

import entities.GFinancement.Investissement;
import services.IService;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvestissementService implements IService<Investissement> {

    @Override
    public void ajouter(Investissement inv) throws SQLException {
        String sql = "INSERT INTO investissement (montantInvestissement, statut, date_investissement, id_projet, id_user) " +
                "VALUES (?, ?, ?, ?, ?)";
        Connection conn = MyDatabase.getInstance().getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, inv.getMontantInvestissement());
            stmt.setString(2, inv.getStatut());
            stmt.setString(3, inv.getDate_investissement());
            stmt.setInt(4, inv.getId_projet());
            stmt.setInt(5, inv.getId_user());
            stmt.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM investissement WHERE id_investissement = ?";
        Connection conn = MyDatabase.getInstance().getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public void update(Investissement inv) throws SQLException {
        String sql = "UPDATE investissement SET montantInvestissement=?, statut=?, date_investissement=?, id_projet=?, id_user=? " +
                "WHERE id_investissement=?";
        Connection conn = MyDatabase.getInstance().getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, inv.getMontantInvestissement());
            stmt.setString(2, inv.getStatut());
            stmt.setString(3, inv.getDate_investissement());
            stmt.setInt(4, inv.getId_projet());
            stmt.setInt(5, inv.getId_user());
            stmt.setInt(6, inv.getId_investissement());
            stmt.executeUpdate();
        }
    }

    @Override
    public List<Investissement> read() throws SQLException {
        List<Investissement> list = new ArrayList<>();
        String sql = "SELECT * FROM investissement";
        Connection conn = MyDatabase.getInstance().getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new Investissement(
                        rs.getInt("id_investissement"),
                        rs.getDouble("montantInvestissement"),
                        rs.getString("statut"),
                        rs.getString("date_investissement"),
                        rs.getInt("id_projet"),
                        rs.getInt("id_user")
                ));
            }
        }
        return list;
    }
}
