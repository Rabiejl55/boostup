package services.FinancementService;

import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.*;

public class FinancementStatsService {

    // ---------- KPI ----------
    public double totalLeve(LocalDate from, LocalDate to, String statutProjet) throws SQLException {
        String sql =
                "SELECT COALESCE(SUM(i.montantInvestissement),0) AS total " +
                        "FROM investissement i " +
                        "JOIN projet p ON p.id_projet = i.id_projet " +
                        "WHERE i.statut='FINANCE' " +
                        "AND (? IS NULL OR i.date_investissement >= ?) " +
                        "AND (? IS NULL OR i.date_investissement <= ?) " +
                        "AND (? = 'TOUS' OR p.statut = ?)";

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            setDateOrNull(ps, 1, from);
            setDateOrNull(ps, 2, from);
            setDateOrNull(ps, 3, to);
            setDateOrNull(ps, 4, to);
            ps.setString(5, statutProjet == null ? "TOUS" : statutProjet);
            ps.setString(6, statutProjet == null ? "TOUS" : statutProjet);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble("total");
            }
        }
    }

    public int nbProjets(String statutProjet) throws SQLException {
        String sql = "SELECT COUNT(*) AS nb FROM projet WHERE (?='TOUS' OR statut=?)";
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String st = (statutProjet == null ? "TOUS" : statutProjet);
            ps.setString(1, st);
            ps.setString(2, st);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("nb");
            }
        }
    }

    public int nbInvestissements(LocalDate from, LocalDate to, String statutProjet) throws SQLException {
        String sql =
                "SELECT COUNT(*) AS nb " +
                        "FROM investissement i JOIN projet p ON p.id_projet=i.id_projet " +
                        "WHERE 1=1 " +
                        "AND (? IS NULL OR i.date_investissement >= ?) " +
                        "AND (? IS NULL OR i.date_investissement <= ?) " +
                        "AND (?='TOUS' OR p.statut=?)";

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            setDateOrNull(ps, 1, from);
            setDateOrNull(ps, 2, from);
            setDateOrNull(ps, 3, to);
            setDateOrNull(ps, 4, to);
            String st = (statutProjet == null ? "TOUS" : statutProjet);
            ps.setString(5, st);
            ps.setString(6, st);

            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("nb");
            }
        }
    }

    public double tauxFinancement(LocalDate from, LocalDate to, String statutProjet) throws SQLException {
        // taux = totalLeve / totalBudget (des projets filtrés)
        double total = totalLeve(from, to, statutProjet);

        String sql = "SELECT COALESCE(SUM(budget),0) AS budgetTotal FROM projet WHERE (?='TOUS' OR statut=?)";
        double budgetTotal;

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            String st = (statutProjet == null ? "TOUS" : statutProjet);
            ps.setString(1, st);
            ps.setString(2, st);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                budgetTotal = rs.getDouble("budgetTotal");
            }
        }

        if (budgetTotal <= 0) return 0;
        return (total * 100.0 / budgetTotal);
    }

    // ---------- DATASETS CHARTS ----------

    // 1) Bar: Top projets (montant levé)
    public List<TopProjet> topProjets(LocalDate from, LocalDate to, String statutProjet, int limit) throws SQLException {
        String sql =
                "SELECT p.id_projet, p.titre, COALESCE(SUM(i.montantInvestissement),0) AS total " +
                        "FROM projet p " +
                        "LEFT JOIN investissement i ON i.id_projet=p.id_projet AND i.statut='FINANCE' " +
                        "WHERE 1=1 " +
                        "AND (?='TOUS' OR p.statut=?) " +
                        "AND (? IS NULL OR i.date_investissement >= ?) " +
                        "AND (? IS NULL OR i.date_investissement <= ?) " +
                        "GROUP BY p.id_projet, p.titre " +
                        "ORDER BY total DESC " +
                        "LIMIT ?";

        List<TopProjet> res = new ArrayList<>();
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            String st = (statutProjet == null ? "TOUS" : statutProjet);
            ps.setString(1, st);
            ps.setString(2, st);

            setDateOrNull(ps, 3, from);
            setDateOrNull(ps, 4, from);
            setDateOrNull(ps, 5, to);
            setDateOrNull(ps, 6, to);

            ps.setInt(7, Math.max(1, limit));

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(new TopProjet(
                            rs.getInt("id_projet"),
                            rs.getString("titre"),
                            rs.getDouble("total")
                    ));
                }
            }
        }
        return res;
    }

    // 2) Pie: Répartition modes paiement (transactions VALIDEE)
    public Map<String, Double> repartitionModesPaiement(LocalDate from, LocalDate to) throws SQLException {
        String sql =
                "SELECT mode_paiement, COALESCE(SUM(montantTransaction),0) AS total " +
                        "FROM transaction_financiere " +
                        "WHERE statut_transaction='VALIDEE' " +
                        "AND (? IS NULL OR date_transaction >= ?) " +
                        "AND (? IS NULL OR date_transaction <= ?) " +
                        "GROUP BY mode_paiement " +
                        "ORDER BY total DESC";

        Map<String, Double> map = new LinkedHashMap<>();
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            setDateOrNull(ps, 1, from);
            setDateOrNull(ps, 2, from);
            setDateOrNull(ps, 3, to);
            setDateOrNull(ps, 4, to);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    String mode = rs.getString("mode_paiement");
                    double total = rs.getDouble("total");
                    map.put(mode == null ? "INCONNU" : mode, total);
                }
            }
        }
        return map;
    }

    // 3) Line: évolution mensuelle des investissements FINANCE
    public List<MoisPoint> evolutionMensuelle(LocalDate from, LocalDate to, String statutProjet) throws SQLException {
        String sql =
                "SELECT DATE_FORMAT(STR_TO_DATE(i.date_investissement, '%Y-%m-%d'), '%Y-%m') AS mois, " +
                        "COALESCE(SUM(i.montantInvestissement),0) AS total " +
                        "FROM investissement i JOIN projet p ON p.id_projet=i.id_projet " +
                        "WHERE i.statut='FINANCE' " +
                        "AND (? IS NULL OR i.date_investissement >= ?) " +
                        "AND (? IS NULL OR i.date_investissement <= ?) " +
                        "AND (?='TOUS' OR p.statut=?) " +
                        "GROUP BY mois " +
                        "ORDER BY mois";

        // ⚠️ Si ta DB n’est PAS MySQL, dis-moi (SQLite/PostgreSQL) -> je te donne la requête adaptée.
        List<MoisPoint> res = new ArrayList<>();

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {

            setDateOrNull(ps, 1, from);
            setDateOrNull(ps, 2, from);
            setDateOrNull(ps, 3, to);
            setDateOrNull(ps, 4, to);

            String st = (statutProjet == null ? "TOUS" : statutProjet);
            ps.setString(5, st);
            ps.setString(6, st);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    res.add(new MoisPoint(rs.getString("mois"), rs.getDouble("total")));
                }
            }
        }
        return res;
    }

    // ---------- Utils ----------
    private void setDateOrNull(PreparedStatement ps, int index, LocalDate date) throws SQLException {
        if (date == null) ps.setNull(index, Types.VARCHAR);
        else ps.setString(index, date.toString());
    }

    // ---------- DTO ----------
    public static class TopProjet {
        public final int idProjet;
        public final String titre;
        public final double total;
        public TopProjet(int idProjet, String titre, double total) {
            this.idProjet = idProjet;
            this.titre = titre;
            this.total = total;
        }
    }

    public static class MoisPoint {
        public final String mois;   // "2026-02"
        public final double total;
        public MoisPoint(String mois, double total) {
            this.mois = mois;
            this.total = total;
        }
    }
}