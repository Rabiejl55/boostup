package services.FinancementService;

import utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FinancementStatsService {

    // ===== KPI 1: total investi (investissements FINANCE)
    public double totalInvesti() throws SQLException {
        String sql = "SELECT COALESCE(SUM(montantInvestissement),0) AS total " +
                "FROM investissement WHERE statut = 'FINANCE'";
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getDouble("total");
        }
    }

    // ===== KPI 2: nombre investissements
    public int countInvestissements() throws SQLException {
        String sql = "SELECT COUNT(*) AS nb FROM investissement";
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt("nb");
        }
    }

    // ===== KPI 3: nombre investissements confirmés
    public int countInvestissementsFinances() throws SQLException {
        String sql = "SELECT COUNT(*) AS nb FROM investissement WHERE statut = 'FINANCE'";
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt("nb");
        }
    }

    // ===== KPI 4: taux de réussite projets
    public double tauxReussiteProjets() throws SQLException {
        int total = countProjets();
        if (total == 0) return 0;

        String sql = "SELECT COUNT(*) AS nb FROM projet WHERE statut = 'FINANCE'";
        int finances;
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            finances = rs.getInt("nb");
        }
        return (finances * 100.0) / total;
    }

    public int countProjets() throws SQLException {
        String sql = "SELECT COUNT(*) AS nb FROM projet";
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            rs.next();
            return rs.getInt("nb");
        }
    }

    // ===== Top projets par collecte (investissements FINANCE)
    public List<TopProjet> topProjetsCollecte(int limit) throws SQLException {
        String sql =
                "SELECT p.id_projet, p.titre, COALESCE(SUM(i.montantInvestissement),0) AS collecte " +
                        "FROM projet p " +
                        "LEFT JOIN investissement i ON p.id_projet = i.id_projet AND i.statut = 'FINANCE' " +
                        "GROUP BY p.id_projet, p.titre " +
                        "ORDER BY collecte DESC " +
                        "LIMIT ?";
        List<TopProjet> res = new ArrayList<>();

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(new TopProjet(
                            rs.getInt("id_projet"),
                            rs.getString("titre"),
                            rs.getDouble("collecte")
                    ));
                }
            }
        }
        return res;
    }

    // ===== Répartition transactions par statut
    public List<StatutCount> transactionsParStatut() throws SQLException {
        String sql = "SELECT statut_transaction, COUNT(*) AS nb " +
                "FROM transaction_financiere GROUP BY statut_transaction";
        List<StatutCount> res = new ArrayList<>();

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                res.add(new StatutCount(rs.getString("statut_transaction"), rs.getInt("nb")));
            }
        }
        return res;
    }

    // ===== Evolution mensuelle (si date_transaction = yyyy-MM-dd)
    public List<MoisMontant> investissementsParMois() throws SQLException {
        String sql =
                "SELECT DATE_FORMAT(date_investissement, '%Y-%m') AS mois, " +
                        "       COALESCE(SUM(montantInvestissement),0) AS total " +
                        "FROM investissement " +
                        "WHERE statut = 'FINANCE' " +
                        "GROUP BY mois " +
                        "ORDER BY mois ASC";

        List<MoisMontant> res = new ArrayList<>();
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                res.add(new MoisMontant(rs.getString("mois"), rs.getDouble("total")));
            }
        }
        return res;
    }

    // ===== DTOs
    public static class TopProjet {
        public final int idProjet;
        public final String titre;
        public final double collecte;
        public TopProjet(int idProjet, String titre, double collecte) {
            this.idProjet = idProjet;
            this.titre = titre;
            this.collecte = collecte;
        }
    }

    public static class StatutCount {
        public final String statut;
        public final int count;
        public StatutCount(String statut, int count) {
            this.statut = statut;
            this.count = count;
        }
    }

    public static class MoisMontant {
        public final String mois;
        public final double total;
        public MoisMontant(String mois, double total) {
            this.mois = mois;
            this.total = total;
        }
    }
}