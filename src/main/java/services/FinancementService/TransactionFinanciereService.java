package services.FinancementService;

import entities.GFinancement.Transaction_Financiere;
import services.IService;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class TransactionFinanciereService implements IService<Transaction_Financiere> {

    @Override
    public void ajouter(Transaction_Financiere t) throws SQLException {
        String sql = "INSERT INTO transaction_financiere (montantTransaction, date_transaction, mode_paiement, statut_transaction, id_investissement) " +
                "VALUES (?, ?, ?, ?, ?)";
        Connection conn = MyDatabase.getInstance().getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, t.getMontantTransaction());
            stmt.setString(2, t.getDate_transaction());
            stmt.setString(3, t.getMode_paiement());
            stmt.setString(4, t.getStatut_transaction());
            stmt.setInt(5, t.getId_investissement());
            stmt.executeUpdate();
        }
    }

    @Override
    public void supprimer(int id) throws SQLException {
        String sql = "DELETE FROM transaction_financiere WHERE id_transaction = ?";
        Connection conn = MyDatabase.getInstance().getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, id);
            stmt.executeUpdate();
        }
    }

    @Override
    public void update(Transaction_Financiere t) throws SQLException {
        String sql = "UPDATE transaction_financiere SET montantTransaction=?, date_transaction=?, mode_paiement=?, statut_transaction=?, id_investissement=? " +
                "WHERE id_transaction=?";
        Connection conn = MyDatabase.getInstance().getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setDouble(1, t.getMontantTransaction());
            stmt.setString(2, t.getDate_transaction());
            stmt.setString(3, t.getMode_paiement());
            stmt.setString(4, t.getStatut_transaction());
            stmt.setInt(5, t.getId_investissement());
            stmt.setInt(6, t.getId_transaction());
            stmt.executeUpdate();
        }
    }

    @Override
    public List<Transaction_Financiere> read() throws SQLException {
        List<Transaction_Financiere> list = new ArrayList<>();
        String sql = "SELECT * FROM transaction_financiere";
        Connection conn = MyDatabase.getInstance().getConnection();

        try (PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(new Transaction_Financiere(
                        rs.getInt("id_transaction"),
                        rs.getDouble("montantTransaction"),
                        rs.getString("date_transaction"),
                        rs.getString("mode_paiement"),
                        rs.getString("statut_transaction"),
                        rs.getInt("id_investissement")
                ));
            }
        }
        return list;
    }
}
