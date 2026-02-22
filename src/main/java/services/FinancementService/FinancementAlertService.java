package services.FinancementService;

import entities.GFinancement.AlerteFinancement;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;

public class FinancementAlertService {

    public List<AlerteFinancement> getAlertesProjet(int idProjet) throws Exception {
        List<AlerteFinancement> alertes = new ArrayList<>();

        ProjetKpi kpi = fetchKpiProjet(idProjet);

        // A) Objectif atteint
        if (kpi.budget > 0 && kpi.totalLeve >= kpi.budget) {
            alertes.add(new AlerteFinancement(
                    "OBJECTIF_ATTEINT",
                    "INFO",
                    "🎯 Objectif atteint : " + format2(kpi.totalLeve) + " / " + format2(kpi.budget)
            ));
        }

        // B) Projet à risque (progression faible) — sans date_creation
        double prog = (kpi.budget <= 0) ? 0 : (kpi.totalLeve * 100.0 / kpi.budget);
        if (kpi.nbInvest >= 5 && prog < 30) {
            alertes.add(new AlerteFinancement(
                    "RISQUE",
                    "WARN",
                    "⚠️ Projet à risque : progression faible (" + format2(prog) + "%) avec " + kpi.nbInvest + " investissements"
            ));
        }

        // C) Transactions en attente
        int nbTxAttente = fetchNbTransactionsEnAttente(idProjet);
        if (nbTxAttente > 0) {
            alertes.add(new AlerteFinancement(
                    "PAIEMENT_EN_ATTENTE",
                    "CRITICAL",
                    "⏳ " + nbTxAttente + " transaction(s) EN_ATTENTE sur ce projet"
            ));
        }

        if (alertes.isEmpty()) {
            alertes.add(new AlerteFinancement("AUCUNE", "INFO", "✅ Aucune alerte détectée."));
        }

        return alertes;
    }

    // ----------------- SQL helpers -----------------

    private ProjetKpi fetchKpiProjet(int idProjet) throws Exception {
        String sql = """
                SELECT 
                    p.budget AS budget,
                    COALESCE(SUM(CASE WHEN i.statut='FINANCE' THEN i.montantInvestissement ELSE 0 END),0) AS totalLeve,
                    COALESCE(COUNT(i.id_investissement),0) AS nbInvest
                FROM projet p
                LEFT JOIN investissement i ON i.id_projet = p.id_projet
                WHERE p.id_projet=?
                GROUP BY p.budget
                """;

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) throw new IllegalArgumentException("Projet introuvable (ID=" + idProjet + ")");
                ProjetKpi k = new ProjetKpi();
                k.budget = rs.getDouble("budget");
                k.totalLeve = rs.getDouble("totalLeve");
                k.nbInvest = rs.getInt("nbInvest");
                return k;
            }
        }
    }

    private int fetchNbTransactionsEnAttente(int idProjet) throws Exception {
        // transaction_financiere -> investissement -> projet
        String sql = """
                SELECT COUNT(*) AS nb
                FROM transaction_financiere t
                JOIN investissement i ON i.id_investissement = t.id_investissement
                WHERE i.id_projet = ?
                  AND t.statut_transaction = 'EN_ATTENTE'
                """;

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("nb");
            }
        }
    }

    private static String format2(double v) {
        return String.format(java.util.Locale.US, "%.2f", v);
    }

    private static class ProjetKpi {
        double budget;
        double totalLeve;
        int nbInvest;
    }
}