package services.FinancementService;

import entities.GFinancement.Transaction_Financiere;
import services.IService;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TransactionFinanciereService implements IService<Transaction_Financiere> {

    @Override
    public void ajouter(Transaction_Financiere t) throws SQLException {
        if (t.getStatut_transaction() == null || t.getStatut_transaction().isBlank()) {
            t.setStatut_transaction("EN_ATTENTE");
        }

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

    // ✅ WORKFLOW: Confirmer paiement
    // Transaction -> VALIDEE
    // Investissement -> FINANCE
    // Si budget atteint -> Projet -> FINANCE
    public void confirmerPaiement(int idTransaction) throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        conn.setAutoCommit(false);

        try {
            // 1) Transaction -> VALIDEE
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE transaction_financiere SET statut_transaction = 'VALIDEE' WHERE id_transaction = ?")) {
                ps.setInt(1, idTransaction);
                int updated = ps.executeUpdate();
                if (updated == 0) throw new SQLException("Transaction introuvable.");
            }

            // 2) id_investissement
            int idInvestissement;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id_investissement FROM transaction_financiere WHERE id_transaction = ?")) {
                ps.setInt(1, idTransaction);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    idInvestissement = rs.getInt("id_investissement");
                }
            }

            // 3) Investissement -> FINANCE
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE investissement SET statut = 'FINANCE' WHERE id_investissement = ?")) {
                ps.setInt(1, idInvestissement);
                ps.executeUpdate();
            }

            // 4) id_projet
            int idProjet;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT id_projet FROM investissement WHERE id_investissement = ?")) {
                ps.setInt(1, idInvestissement);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Investissement introuvable.");
                    idProjet = rs.getInt("id_projet");
                }
            }

            // 5) Montant collecté (investissements FINANCE)
            double collecte;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT COALESCE(SUM(montantInvestissement),0) AS total " +
                            "FROM investissement WHERE id_projet = ? AND statut = 'FINANCE'")) {
                ps.setInt(1, idProjet);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    collecte = rs.getDouble("total");
                }
            }

            // 6) Budget projet
            double budget;
            try (PreparedStatement ps = conn.prepareStatement(
                    "SELECT budget FROM projet WHERE id_projet = ?")) {
                ps.setInt(1, idProjet);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) throw new SQLException("Projet introuvable.");
                    budget = rs.getDouble("budget");
                }
            }

            // 7) Si atteint -> Projet FINANCE
            if (collecte >= budget) {
                try (PreparedStatement ps = conn.prepareStatement(
                        "UPDATE projet SET statut = 'FINANCE' WHERE id_projet = ?")) {
                    ps.setInt(1, idProjet);
                    ps.executeUpdate();
                }
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }

    // ✅ WORKFLOW: Clôturer projet en échec + remboursement
    // Projet -> REFUSE
    // Investissements FINANCE -> REMBOURSE
    // Transactions -> REMBOURSEE
    public void rembourserProjet(int idProjet) throws SQLException {
        Connection conn = MyDatabase.getInstance().getConnection();
        conn.setAutoCommit(false);

        try {
            // 1) Projet -> REFUSE
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE projet SET statut = 'REFUSE' WHERE id_projet = ?")) {
                ps.setInt(1, idProjet);
                ps.executeUpdate();
            }

            // 2) Passer investissements FINANCE -> REMBOURSE
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE investissement SET statut = 'REMBOURSE' WHERE id_projet = ? AND statut = 'FINANCE'")) {
                ps.setInt(1, idProjet);
                ps.executeUpdate();
            }

            // 3) Marquer transactions liées -> REMBOURSEE
            try (PreparedStatement ps = conn.prepareStatement(
                    "UPDATE transaction_financiere tf " +
                            "JOIN investissement i ON tf.id_investissement = i.id_investissement " +
                            "SET tf.statut_transaction = 'REMBOURSEE' " +
                            "WHERE i.id_projet = ?")) {
                ps.setInt(1, idProjet);
                ps.executeUpdate();
            }

            conn.commit();
        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
        }
    }
}