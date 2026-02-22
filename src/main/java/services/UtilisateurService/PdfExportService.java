package services.UtilisateurService;

import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.draw.SolidLine;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.io.font.constants.StandardFonts;

import entities.GUtilisateurs.User;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Service d'export PDF pour générer des rapports professionnels.
 * Utilise iText 7 pour la génération de documents PDF.
 *
 * Fonctionnalités :
 * - Export de la liste des utilisateurs
 * - Rapport de statistiques
 * - Certificats et attestations
 */
public class PdfExportService {

    private static final Logger LOGGER = Logger.getLogger(PdfExportService.class.getName());

    // Couleurs du thème BoostUp
    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(27, 42, 74);       // #1b2a4a
    private static final DeviceRgb SECONDARY_COLOR = new DeviceRgb(45, 27, 78);     // #2d1b4e
    private static final DeviceRgb ACCENT_COLOR = new DeviceRgb(230, 57, 86);       // #e63956
    private static final DeviceRgb LIGHT_BG = new DeviceRgb(248, 249, 250);         // #f8f9fa
    private static final DeviceRgb TABLE_HEADER_BG = new DeviceRgb(27, 42, 74);     // #1b2a4a
    private static final DeviceRgb TABLE_ROW_ALT = new DeviceRgb(240, 242, 245);    // #f0f2f5
    private static final DeviceRgb SUCCESS_COLOR = new DeviceRgb(46, 204, 113);     // #2ecc71
    private static final DeviceRgb DANGER_COLOR = new DeviceRgb(231, 76, 60);       // #e74c3c

    /**
     * Exporte la liste des utilisateurs en PDF
     *
     * @param users    Liste des utilisateurs à exporter
     * @param filePath Chemin du fichier PDF de sortie
     * @return true si l'export a réussi
     */
    public static boolean exportUsersList(List<User> users, String filePath) {
        try {
            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4.rotate()); // Format paysage

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // ═══ EN-TÊTE ═══
            addHeader(document, boldFont, regularFont, "Liste des Utilisateurs");

            // ═══ STATISTIQUES RÉSUMÉ ═══
            addUserStats(document, users, boldFont, regularFont);

            // ═══ TABLEAU DES UTILISATEURS ═══
            Table table = new Table(UnitValue.createPercentArray(new float[]{5, 20, 25, 15, 10, 25}))
                    .useAllAvailableWidth();

            // En-têtes du tableau
            String[] headers = {"#", "Nom", "Email", "Rôle", "Statut", "Date d'inscription"};
            for (String header : headers) {
                Cell cell = new Cell()
                        .add(new Paragraph(header).setFont(boldFont).setFontSize(10))
                        .setBackgroundColor(TABLE_HEADER_BG)
                        .setFontColor(ColorConstants.WHITE)
                        .setPadding(8)
                        .setTextAlignment(TextAlignment.CENTER);
                table.addHeaderCell(cell);
            }

            // Données
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            int index = 1;
            for (User user : users) {
                DeviceRgb bgColor = (index % 2 == 0) ? TABLE_ROW_ALT : new DeviceRgb(255, 255, 255);

                table.addCell(createDataCell(String.valueOf(index), regularFont, bgColor, TextAlignment.CENTER));
                table.addCell(createDataCell(user.getDisplayName(), regularFont, bgColor, TextAlignment.LEFT));
                table.addCell(createDataCell(user.getEmail(), regularFont, bgColor, TextAlignment.LEFT));
                table.addCell(createRoleBadgeCell(user.getRole().name(), regularFont, bgColor));
                table.addCell(createStatusCell(user.isActive(), boldFont, bgColor));
                table.addCell(createDataCell(
                        user.getDateCreation() != null ? sdf.format(user.getDateCreation()) : "N/A",
                        regularFont, bgColor, TextAlignment.CENTER));

                index++;
            }

            document.add(table);

            // ═══ PIED DE PAGE ═══
            addFooter(document, regularFont, users.size());

            document.close();
            LOGGER.info("✅ PDF exporté avec succès → " + filePath + " (" + users.size() + " utilisateurs)");
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Erreur export PDF", e);
            return false;
        }
    }

    /**
     * Génère un rapport de statistiques en PDF
     */
    public static boolean exportStatsReport(List<User> users, String filePath) {
        try {
            PdfWriter writer = new PdfWriter(filePath);
            PdfDocument pdfDoc = new PdfDocument(writer);
            Document document = new Document(pdfDoc, PageSize.A4);

            PdfFont boldFont = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
            PdfFont regularFont = PdfFontFactory.createFont(StandardFonts.HELVETICA);

            // ═══ EN-TÊTE ═══
            addHeader(document, boldFont, regularFont, "Rapport de Statistiques");

            // ═══ RÉSUMÉ GLOBAL ═══
            document.add(new Paragraph("📊 Vue d'ensemble")
                    .setFont(boldFont).setFontSize(16).setFontColor(PRIMARY_COLOR)
                    .setMarginTop(20));

            Table statsTable = new Table(UnitValue.createPercentArray(new float[]{25, 25, 25, 25}))
                    .useAllAvailableWidth();

            long totalUsers = users.size();
            long activeUsers = users.stream().filter(User::isActive).count();
            long inactiveUsers = totalUsers - activeUsers;

            // Répartition par rôle
            Map<String, Long> roleDistribution = users.stream()
                    .collect(Collectors.groupingBy(u -> u.getRole().name(), Collectors.counting()));

            addStatCard(statsTable, "Total Utilisateurs", String.valueOf(totalUsers), PRIMARY_COLOR, boldFont, regularFont);
            addStatCard(statsTable, "Utilisateurs Actifs", String.valueOf(activeUsers), SUCCESS_COLOR, boldFont, regularFont);
            addStatCard(statsTable, "Utilisateurs Inactifs", String.valueOf(inactiveUsers), DANGER_COLOR, boldFont, regularFont);
            addStatCard(statsTable, "Taux d'activité", String.format("%.1f%%", totalUsers > 0 ? (activeUsers * 100.0 / totalUsers) : 0),
                    new DeviceRgb(13, 110, 253), boldFont, regularFont);

            document.add(statsTable);

            // ═══ RÉPARTITION PAR RÔLE ═══
            document.add(new Paragraph("👥 Répartition par rôle")
                    .setFont(boldFont).setFontSize(16).setFontColor(PRIMARY_COLOR)
                    .setMarginTop(25));

            Table roleTable = new Table(UnitValue.createPercentArray(new float[]{40, 30, 30}))
                    .useAllAvailableWidth();

            // Headers
            roleTable.addHeaderCell(new Cell().add(new Paragraph("Rôle").setFont(boldFont).setFontSize(10))
                    .setBackgroundColor(TABLE_HEADER_BG).setFontColor(ColorConstants.WHITE).setPadding(8));
            roleTable.addHeaderCell(new Cell().add(new Paragraph("Nombre").setFont(boldFont).setFontSize(10))
                    .setBackgroundColor(TABLE_HEADER_BG).setFontColor(ColorConstants.WHITE).setPadding(8).setTextAlignment(TextAlignment.CENTER));
            roleTable.addHeaderCell(new Cell().add(new Paragraph("Pourcentage").setFont(boldFont).setFontSize(10))
                    .setBackgroundColor(TABLE_HEADER_BG).setFontColor(ColorConstants.WHITE).setPadding(8).setTextAlignment(TextAlignment.CENTER));

            int roleIndex = 0;
            for (Map.Entry<String, Long> entry : roleDistribution.entrySet()) {
                DeviceRgb bgColor = (roleIndex % 2 == 0) ? new DeviceRgb(255, 255, 255) : TABLE_ROW_ALT;

                roleTable.addCell(new Cell().add(new Paragraph(getRoleEmoji(entry.getKey()) + " " + entry.getKey())
                        .setFont(regularFont).setFontSize(10)).setBackgroundColor(bgColor).setPadding(8));
                roleTable.addCell(new Cell().add(new Paragraph(String.valueOf(entry.getValue()))
                        .setFont(boldFont).setFontSize(10)).setBackgroundColor(bgColor).setPadding(8).setTextAlignment(TextAlignment.CENTER));
                roleTable.addCell(new Cell().add(new Paragraph(String.format("%.1f%%", totalUsers > 0 ? (entry.getValue() * 100.0 / totalUsers) : 0))
                        .setFont(regularFont).setFontSize(10)).setBackgroundColor(bgColor).setPadding(8).setTextAlignment(TextAlignment.CENTER));

                roleIndex++;
            }

            document.add(roleTable);

            // ═══ DERNIÈRES INSCRIPTIONS ═══
            document.add(new Paragraph("🕐 Dernières inscriptions")
                    .setFont(boldFont).setFontSize(16).setFontColor(PRIMARY_COLOR)
                    .setMarginTop(25));

            List<User> recentUsers = users.stream()
                    .sorted((a, b) -> {
                        if (b.getDateCreation() == null) return -1;
                        if (a.getDateCreation() == null) return 1;
                        return b.getDateCreation().compareTo(a.getDateCreation());
                    })
                    .limit(10)
                    .collect(Collectors.toList());

            Table recentTable = new Table(UnitValue.createPercentArray(new float[]{5, 30, 35, 30}))
                    .useAllAvailableWidth();

            recentTable.addHeaderCell(new Cell().add(new Paragraph("#").setFont(boldFont).setFontSize(10))
                    .setBackgroundColor(TABLE_HEADER_BG).setFontColor(ColorConstants.WHITE).setPadding(8).setTextAlignment(TextAlignment.CENTER));
            recentTable.addHeaderCell(new Cell().add(new Paragraph("Nom").setFont(boldFont).setFontSize(10))
                    .setBackgroundColor(TABLE_HEADER_BG).setFontColor(ColorConstants.WHITE).setPadding(8));
            recentTable.addHeaderCell(new Cell().add(new Paragraph("Email").setFont(boldFont).setFontSize(10))
                    .setBackgroundColor(TABLE_HEADER_BG).setFontColor(ColorConstants.WHITE).setPadding(8));
            recentTable.addHeaderCell(new Cell().add(new Paragraph("Date").setFont(boldFont).setFontSize(10))
                    .setBackgroundColor(TABLE_HEADER_BG).setFontColor(ColorConstants.WHITE).setPadding(8).setTextAlignment(TextAlignment.CENTER));

            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
            int recentIdx = 1;
            for (User user : recentUsers) {
                DeviceRgb bgColor = (recentIdx % 2 == 0) ? TABLE_ROW_ALT : new DeviceRgb(255, 255, 255);
                recentTable.addCell(createDataCell(String.valueOf(recentIdx), regularFont, bgColor, TextAlignment.CENTER));
                recentTable.addCell(createDataCell(user.getDisplayName(), regularFont, bgColor, TextAlignment.LEFT));
                recentTable.addCell(createDataCell(user.getEmail(), regularFont, bgColor, TextAlignment.LEFT));
                recentTable.addCell(createDataCell(
                        user.getDateCreation() != null ? sdf.format(user.getDateCreation()) : "N/A",
                        regularFont, bgColor, TextAlignment.CENTER));
                recentIdx++;
            }

            document.add(recentTable);

            // ═══ PIED DE PAGE ═══
            addFooter(document, regularFont, users.size());

            document.close();
            LOGGER.info("✅ Rapport statistiques exporté → " + filePath);
            return true;

        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "❌ Erreur export rapport statistiques", e);
            return false;
        }
    }

    // ═══════════════════════════════════════════════════════
    // MÉTHODES UTILITAIRES PRIVÉES
    // ═══════════════════════════════════════════════════════

    private static void addHeader(Document document, PdfFont boldFont, PdfFont regularFont, String title) {
        // Bande de couleur en haut
        Table headerBand = new Table(1).useAllAvailableWidth();
        Cell bandCell = new Cell()
                .setBackgroundColor(PRIMARY_COLOR)
                .setPadding(25)
                .setBorder(Border.NO_BORDER);

        bandCell.add(new Paragraph("⚡ BoostUp")
                .setFont(boldFont).setFontSize(24).setFontColor(ColorConstants.WHITE));
        bandCell.add(new Paragraph("Startup Platform")
                .setFont(regularFont).setFontSize(11)
                .setFontColor(new DeviceRgb(200, 200, 200)));

        headerBand.addCell(bandCell);
        document.add(headerBand);

        // Titre du rapport
        document.add(new Paragraph(title)
                .setFont(boldFont).setFontSize(20).setFontColor(PRIMARY_COLOR)
                .setMarginTop(15));

        // Date de génération
        SimpleDateFormat sdf = new SimpleDateFormat("dd MMMM yyyy 'à' HH:mm");
        document.add(new Paragraph("📅 Généré le " + sdf.format(new Date()))
                .setFont(regularFont).setFontSize(10).setFontColor(new DeviceRgb(128, 128, 128))
                .setMarginBottom(15));

        // Ligne de séparation
        SolidLine accentLine = new SolidLine(2f);
        accentLine.setColor(ACCENT_COLOR);
        document.add(new LineSeparator(accentLine));
    }

    private static void addUserStats(Document document, List<User> users, PdfFont boldFont, PdfFont regularFont) {
        long active = users.stream().filter(User::isActive).count();
        long inactive = users.size() - active;

        Table statsRow = new Table(UnitValue.createPercentArray(new float[]{33, 34, 33}))
                .useAllAvailableWidth().setMarginTop(15).setMarginBottom(15);

        addStatCard(statsRow, "Total", String.valueOf(users.size()), PRIMARY_COLOR, boldFont, regularFont);
        addStatCard(statsRow, "Actifs", String.valueOf(active), SUCCESS_COLOR, boldFont, regularFont);
        addStatCard(statsRow, "Inactifs", String.valueOf(inactive), DANGER_COLOR, boldFont, regularFont);

        document.add(statsRow);
    }

    private static void addStatCard(Table table, String label, String value, DeviceRgb accentColor,
                                    PdfFont boldFont, PdfFont regularFont) {
        Cell cell = new Cell()
                .setBorderLeft(new SolidBorder(accentColor, 4))
                .setBorderTop(new SolidBorder(LIGHT_BG, 1))
                .setBorderRight(new SolidBorder(LIGHT_BG, 1))
                .setBorderBottom(new SolidBorder(LIGHT_BG, 1))
                .setBackgroundColor(new DeviceRgb(255, 255, 255))
                .setPadding(15);

        cell.add(new Paragraph(value)
                .setFont(boldFont).setFontSize(28).setFontColor(accentColor)
                .setMarginBottom(2));
        cell.add(new Paragraph(label)
                .setFont(regularFont).setFontSize(10).setFontColor(new DeviceRgb(128, 128, 128)));

        table.addCell(cell);
    }

    private static Cell createDataCell(String text, PdfFont font, DeviceRgb bgColor, TextAlignment alignment) {
        return new Cell()
                .add(new Paragraph(text != null ? text : "N/A").setFont(font).setFontSize(9))
                .setBackgroundColor(bgColor)
                .setPadding(6)
                .setTextAlignment(alignment);
    }

    private static Cell createRoleBadgeCell(String role, PdfFont font, DeviceRgb bgColor) {
        DeviceRgb badgeColor;
        switch (role.toUpperCase()) {
            case "ADMIN":
                badgeColor = ACCENT_COLOR;
                break;
            case "INVESTISSEUR":
                badgeColor = new DeviceRgb(111, 66, 193);
                break;
            default:
                badgeColor = new DeviceRgb(13, 110, 253);
                break;
        }

        return new Cell()
                .add(new Paragraph(getRoleEmoji(role) + " " + role)
                        .setFont(font).setFontSize(9).setFontColor(badgeColor))
                .setBackgroundColor(bgColor)
                .setPadding(6)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private static Cell createStatusCell(boolean active, PdfFont font, DeviceRgb bgColor) {
        String status = active ? "✅ Actif" : "❌ Inactif";
        DeviceRgb color = active ? SUCCESS_COLOR : DANGER_COLOR;

        return new Cell()
                .add(new Paragraph(status).setFont(font).setFontSize(9).setFontColor(color))
                .setBackgroundColor(bgColor)
                .setPadding(6)
                .setTextAlignment(TextAlignment.CENTER);
    }

    private static String getRoleEmoji(String role) {
        switch (role.toUpperCase()) {
            case "ADMIN": return "🛡️";
            case "INVESTISSEUR": return "💰";
            case "STARTUP": return "🚀";
            default: return "👤";
        }
    }

    private static void addFooter(Document document, PdfFont font, int totalRecords) {
        document.add(new Paragraph("").setMarginTop(20));
        SolidLine footerLine = new SolidLine(0.5f);
        footerLine.setColor(new DeviceRgb(200, 200, 200));
        document.add(new LineSeparator(footerLine));
        document.add(new Paragraph(
                "📄 Ce rapport contient " + totalRecords + " enregistrement(s) | " +
                        "BoostUp © " + new SimpleDateFormat("yyyy").format(new Date()) +
                        " | Document confidentiel")
                .setFont(font).setFontSize(8)
                .setFontColor(new DeviceRgb(160, 160, 160))
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginTop(10));
    }

    /**
     * Obtient le chemin par défaut pour l'export
     */
    public static String getDefaultExportPath(String fileName) {
        String userHome = System.getProperty("user.home");
        String downloads = userHome + File.separator + "Downloads";
        File dir = new File(downloads);
        if (!dir.exists()) {
            dir = new File(userHome + File.separator + "Desktop");
        }
        return dir.getAbsolutePath() + File.separator + fileName;
    }
}




