package services.FinancementService;

import entities.GFinancement.Investissement;
import entities.GFinancement.Projet;
import services.IService;
import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class InvestissementService implements IService<Investissement> {

    @Override
    public void ajouter(Investissement inv) throws SQLException {
        // ✅ Bloquer investissement si projet déjà FINANCE
        ProjetService projetService = new ProjetService();
        Projet p = projetService.getById(inv.getId_projet());
        if (p != null && "FINANCE".equalsIgnoreCase(p.getStatut())) {
            throw new SQLException("Impossible : projet déjà financé.");
        }

        // Si l'UI n'a pas mis statut → par défaut EN_ATTENTE
        if (inv.getStatut() == null || inv.getStatut().isBlank()) {
            inv.setStatut("EN_ATTENTE");
        }

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

    /**
     * ✅ Fonction avancée: ajouter investissement + créer transaction INITIEE automatiquement
     * Retourne l'id_investissement créé.
     */
    public int ajouterAvecTransaction(Investissement inv, String modePaiement) throws SQLException {
        ProjetService projetService = new ProjetService();
        Projet p = projetService.getById(inv.getId_projet());
        if (p != null && "FINANCE".equalsIgnoreCase(p.getStatut())) {
            throw new SQLException("Impossible : projet déjà financé.");
        }

        if (inv.getStatut() == null || inv.getStatut().isBlank()) {
            inv.setStatut("EN_ATTENTE");
        }
        if (modePaiement == null || modePaiement.isBlank()) {
            modePaiement = "CARTE";
        }

        Connection conn = MyDatabase.getInstance().getConnection();
        conn.setAutoCommit(false);

        try {
            // 1) Insert investissement + récupérer ID
            String sqlInv = "INSERT INTO investissement (montantInvestissement, statut, date_investissement, id_projet, id_user) " +
                    "VALUES (?, ?, ?, ?, ?)";
            int idInvest;

            try (PreparedStatement ps = conn.prepareStatement(sqlInv, Statement.RETURN_GENERATED_KEYS)) {
                ps.setDouble(1, inv.getMontantInvestissement());
                ps.setString(2, inv.getStatut());
                ps.setString(3, inv.getDate_investissement());
                ps.setInt(4, inv.getId_projet());
                ps.setInt(5, inv.getId_user());
                ps.executeUpdate();

                try (ResultSet keys = ps.getGeneratedKeys()) {
                    if (!keys.next()) throw new SQLException("Insertion investissement échouée (pas d'ID).");
                    idInvest = keys.getInt(1);
                }
            }

            // 2) Insert transaction INITIEE
            String sqlTr = "INSERT INTO transaction_financiere (montantTransaction, date_transaction, mode_paiement, statut_transaction, id_investissement) " +
                    "VALUES (?, ?, ?, ?, ?)";
            try (PreparedStatement ps2 = conn.prepareStatement(sqlTr)) {
                ps2.setDouble(1, inv.getMontantInvestissement());
                ps2.setString(2, inv.getDate_investissement());
                ps2.setString(3, modePaiement);
                ps2.setString(4, "INITIEE");
                ps2.setInt(5, idInvest);
                ps2.executeUpdate();
            }

            conn.commit();
            return idInvest;

        } catch (SQLException e) {
            conn.rollback();
            throw e;
        } finally {
            conn.setAutoCommit(true);
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

    // ✅ Récupérer un investissement par ID
    public Investissement getById(int idInvestissement) throws SQLException {
        String sql = "SELECT * FROM investissement WHERE id_investissement = ?";
        Connection conn = MyDatabase.getInstance().getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idInvestissement);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new Investissement(
                            rs.getInt("id_investissement"),
                            rs.getDouble("montantInvestissement"),
                            rs.getString("statut"),
                            rs.getString("date_investissement"),
                            rs.getInt("id_projet"),
                            rs.getInt("id_user")
                    );
                }
            }
        }
        return null;
    }

    // ✅ Somme des investissements confirmés d’un projet
    public double getMontantCollecteConfirme(int idProjet) throws SQLException {
        String sql = "SELECT COALESCE(SUM(montantInvestissement), 0) AS total " +
                "FROM investissement WHERE id_projet = ? AND statut = 'CONFIRME'";
        Connection conn = MyDatabase.getInstance().getConnection();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("total");
            }
        }
        return 0;
    }

    public List<Investissement> recuperer() {
        return List.of();
    }
}