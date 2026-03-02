package services.FinancementService;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import utils.MyDatabase;

import java.io.File;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.util.Locale;

public class FinancementPdfService {

    // ✅ éviter les séparateurs FR qui peuvent produire U+202F
    private final DecimalFormat df = new DecimalFormat("#0.00",
            new DecimalFormatSymbols(Locale.US));

    public void genererRapportProjet(int idProjet, File outFile) throws Exception {
        ProjetData projet = fetchProjet(idProjet);
        if (projet == null) throw new IllegalArgumentException("Projet introuvable (ID=" + idProjet + ")");

        double totalLeve = fetchTotalLeve(idProjet);
        int nbInvest = fetchNbInvestissements(idProjet);
        double progression = (projet.budget <= 0) ? 0 : (totalLeve * 100.0 / projet.budget);

        try (PDDocument doc = new PDDocument()) {

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {

                float margin = 50;
                float y = page.getMediaBox().getHeight() - margin;

                // Titre
                y = writeTitle(cs, "Rapport Financier - Projet", margin, y);
                y -= 10;

                // Date
                y = writeText(cs, "Date : " + LocalDate.now(), margin, y);
                y = writeText(cs, "ID Projet : " + projet.id, margin, y);
                y -= 8;

                // Infos projet
                y = writeSection(cs, "Informations Projet", margin, y);
                y = writeText(cs, "Titre : " + safe(projet.titre), margin, y);
                y = writeText(cs, "Statut : " + safe(projet.statut), margin, y);
                y = writeText(cs, "Budget : " + df.format(projet.budget), margin, y);
                y = writeText(cs, "Description : " + safe(projet.description), margin, y);
                y -= 8;

                // KPI
                y = writeSection(cs, "Indicateurs (KPI)", margin, y);
                y = writeText(cs, "Total leve (investissements FINANCE) : " + df.format(totalLeve), margin, y);
                y = writeText(cs, "Nombre d'investissements : " + nbInvest, margin, y);
                y = writeText(cs, "Progression : " + df.format(progression) + " %", margin, y);
                y -= 8;

                // Liste investissements
                y = writeSection(cs, "Investissements (dernieres lignes)", margin, y);
                y = writeText(cs, "ID | Montant | Statut | Date | User", margin, y);
                y -= 4;

                String sql = "SELECT id_investissement, montantInvestissement, statut, date_investissement, id_user " +
                        "FROM investissement WHERE id_projet=? ORDER BY id_investissement DESC LIMIT 15";

                try (Connection c = MyDatabase.getInstance().getConnection();
                     PreparedStatement ps = c.prepareStatement(sql)) {
                    ps.setInt(1, idProjet);
                    try (ResultSet rs = ps.executeQuery()) {
                        while (rs.next()) {
                            String line = rs.getInt("id_investissement") + " | "
                                    + df.format(rs.getDouble("montantInvestissement")) + " | "
                                    + safe(rs.getString("statut")) + " | "
                                    + safe(rs.getString("date_investissement")) + " | "
                                    + rs.getInt("id_user");

                            y = writeText(cs, line, margin, y);

                            if (y < 80) break;
                        }
                    }
                }
            }

            doc.save(outFile);
        }
    }

    // -------------------- DB helpers --------------------

    private ProjetData fetchProjet(int idProjet) throws SQLException {
        String sql = "SELECT id_projet, titre, description, budget, statut FROM projet WHERE id_projet=?";
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                ProjetData p = new ProjetData();
                p.id = rs.getInt("id_projet");
                p.titre = rs.getString("titre");
                p.description = rs.getString("description");
                p.budget = rs.getDouble("budget");
                p.statut = rs.getString("statut");
                return p;
            }
        }
    }

    private double fetchTotalLeve(int idProjet) throws SQLException {
        String sql = "SELECT COALESCE(SUM(montantInvestissement),0) AS total " +
                "FROM investissement WHERE id_projet=? AND statut='FINANCE'";
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getDouble("total");
            }
        }
    }

    private int fetchNbInvestissements(int idProjet) throws SQLException {
        String sql = "SELECT COUNT(*) AS nb FROM investissement WHERE id_projet=?";
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt("nb");
            }
        }
    }

    // -------------------- PDF helpers --------------------

    private float writeTitle(PDPageContentStream cs, String text, float x, float y) throws Exception {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 18);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitizeForPdf(text));
        cs.endText();
        return y - 26;
    }

    private float writeSection(PDPageContentStream cs, String text, float x, float y) throws Exception {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA_BOLD, 12);
        cs.newLineAtOffset(x, y);
        cs.showText(sanitizeForPdf(text));
        cs.endText();
        return y - 18;
    }

    private float writeText(PDPageContentStream cs, String text, float x, float y) throws Exception {
        cs.beginText();
        cs.setFont(PDType1Font.HELVETICA, 10);
        cs.newLineAtOffset(x, y);
        cs.showText(cut(text, 140)); // cut() appelle sanitize
        cs.endText();
        return y - 14;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    /**
     * ✅ Nettoyage des caractères non supportés par Helvetica / WinAnsi.
     * - U+202F (Narrow NBSP) cause TON crash
     * - U+00A0 (NBSP) arrive aussi parfois
     */
    private String sanitizeForPdf(String s) {
        if (s == null) return "";

        // Remplacer espaces problématiques par espace normal
        s = s.replace("\u202F", " ")  // Narrow NBSP
                .replace("\u00A0", " ")  // NBSP
                .replace("\u2007", " "); // Figure space (au cas où)

        // Remplacer sauts de lignes / tabs
        s = s.replace("\n", " ")
                .replace("\r", " ")
                .replace("\t", " ");

        // Enlever autres caractères de contrôle invisibles
        s = s.replaceAll("[\\p{Cntrl}&&[^\n\t\r]]", "");

        return s;
    }

    private String cut(String s, int max) {
        s = sanitizeForPdf(s);
        if (s.length() <= max) return s;
        if (max <= 3) return s.substring(0, Math.max(0, max));
        return s.substring(0, max - 3) + "...";
    }

    private static class ProjetData {
        int id;
        String titre;
        String description;
        double budget;
        String statut;
    }
}