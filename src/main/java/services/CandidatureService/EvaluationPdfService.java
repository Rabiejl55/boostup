package services.CandidatureService;

import com.itextpdf.io.font.constants.StandardFonts;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFont;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.canvas.PdfCanvas;
import com.itextpdf.kernel.pdf.extgstate.PdfExtGState;
import com.itextpdf.kernel.pdf.xobject.PdfFormXObject;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.*;
import com.itextpdf.layout.properties.AreaBreakType;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import entities.GCandidature.Evaluation;

import java.io.File;
import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class EvaluationPdfService {

    // ── Palette BOOSTUP ──
    private static final DeviceRgb NAVY      = new DeviceRgb(0x0a, 0x0f, 0x1e);
    private static final DeviceRgb NAVY2     = new DeviceRgb(0x14, 0x1d, 0x35);
    private static final DeviceRgb NAVY3     = new DeviceRgb(0x1a, 0x26, 0x40);
    private static final DeviceRgb INDIGO    = new DeviceRgb(0x4f, 0x62, 0xe8);
    private static final DeviceRgb INDIGO_L  = new DeviceRgb(0x96, 0xb4, 0xff);
    private static final DeviceRgb GREEN     = new DeviceRgb(0x10, 0xb9, 0x81);
    private static final DeviceRgb GREEN_L   = new DeviceRgb(0xe8, 0xfa, 0xf2);
    private static final DeviceRgb RED       = new DeviceRgb(0xef, 0x44, 0x44);
    private static final DeviceRgb RED_L     = new DeviceRgb(0xfe, 0xf2, 0xf2);
    private static final DeviceRgb AMBER     = new DeviceRgb(0xf5, 0x9e, 0x0b);
    private static final DeviceRgb GRAY_L    = new DeviceRgb(0xf7, 0xf8, 0xfc);
    private static final DeviceRgb GRAY_M    = new DeviceRgb(0xe5, 0xe9, 0xf2);
    private static final DeviceRgb GRAY_B    = new DeviceRgb(0xdc, 0xe0, 0xf0);
    private static final DeviceRgb TEXT_DARK = new DeviceRgb(0x1e, 0x27, 0x46);
    private static final DeviceRgb TEXT_MID  = new DeviceRgb(0x6b, 0x74, 0x94);
    private static final DeviceRgb NAVY_DIM  = new DeviceRgb(0x2d, 0x3a, 0x55);

    // ── Résultat enrichi retourné au controller ──
    public static class RapportInfo {
        public final String chemin;
        public final String nomFichier;
        public final String candidature;
        public final String decision;
        public final double score;
        public final String dateGeneration;
        public final long   tailleFichierKo;

        public RapportInfo(String chemin, String nomFichier, String candidature,
                           String decision, double score, String dateGeneration) {
            this.chemin          = chemin;
            this.nomFichier      = nomFichier;
            this.candidature     = candidature;
            this.decision        = decision;
            this.score           = score;
            this.dateGeneration  = dateGeneration;
            this.tailleFichierKo = new File(chemin).length() / 1024;
        }
    }

    // ─── POINT D'ENTRÉE ───────────────────────────────────────────

    public RapportInfo genererRapport(Evaluation ev) throws IOException {
        String dir = System.getProperty("user.home")
                + File.separator + "Documents"
                + File.separator + "BOOSTUP_Rapports";
        new File(dir).mkdirs();

        String dateTag    = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmm"));
        String nomFichier = "Rapport_" + sanitize(ev.getNomCandidature()) + "_" + dateTag + ".pdf";
        String chemin     = dir + File.separator + nomFichier;

        PdfWriter   writer = new PdfWriter(chemin);
        PdfDocument pdf    = new PdfDocument(writer);
        Document    doc    = new Document(pdf, PageSize.A4);
        doc.setMargins(0, 0, 0, 0);

        PdfFont bold    = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);
        PdfFont regular = PdfFontFactory.createFont(StandardFonts.HELVETICA);
        PdfFont italic  = PdfFontFactory.createFont(StandardFonts.HELVETICA_OBLIQUE);

        // Page 1 : Couverture
        doc.add(buildCoverPage(ev, bold, regular));
        doc.add(new AreaBreak(AreaBreakType.NEXT_PAGE));

        // Page 2 : Corps
        doc.add(buildPageHeader(ev, bold, regular));
        doc.add(buildDecisionBlock(ev, bold, regular));
        doc.add(buildRadarAndNotes(ev, bold, regular, pdf));
        doc.add(buildCommentaire(ev, bold, italic));
        doc.add(buildFooter(bold, regular));

        doc.close();

        String dateGen = LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"));
        double score   = ev.getNoteGlobale() != null ? ev.getNoteGlobale() : 0.0;
        return new RapportInfo(chemin, nomFichier, nvl(ev.getNomCandidature()),
                nvl(ev.getDecision()), score, dateGen);
    }

    // ════ PAGE 1 — COUVERTURE ════════════════════════════════════

    private Table buildCoverPage(Evaluation ev, PdfFont bold, PdfFont regular) {
        boolean acc = isAcceptee(ev);
        Table cover = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setHeight(PageSize.A4.getHeight());

        Cell bg = new Cell().setBorder(Border.NO_BORDER)
                .setBackgroundColor(NAVY)
                .setBorderLeft(new SolidBorder(INDIGO, 6))
                .setPaddingLeft(56).setPaddingRight(56)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);

        bg.add(spacer(48));

        // Marque
        bg.add(new Paragraph("BOOSTUP")
                .setFont(bold).setFontSize(42).setFontColor(ColorConstants.WHITE)
                .setCharacterSpacing(7).setMarginBottom(6));
        bg.add(new Paragraph("Programme d'Incubation de Startups")
                .setFont(regular).setFontSize(13).setFontColor(INDIGO_L)
                .setMarginBottom(60));

        bg.add(hRule(NAVY3, 1.5f));
        bg.add(spacer(28));

        // Label + Nom candidature
        bg.add(new Paragraph("RAPPORT D'EVALUATION")
                .setFont(bold).setFontSize(10).setFontColor(INDIGO_L)
                .setCharacterSpacing(3).setMarginBottom(14));
        bg.add(new Paragraph(nvl(ev.getNomCandidature()))
                .setFont(bold).setFontSize(28).setFontColor(ColorConstants.WHITE)
                .setMultipliedLeading(1.15f).setMarginBottom(22));

        // Badge décision
        bg.add(new Paragraph(acc ? "CANDIDATURE ACCEPTEE" : "CANDIDATURE REFUSEE")
                .setFont(bold).setFontSize(11).setFontColor(ColorConstants.WHITE)
                .setBackgroundColor(acc ? GREEN : RED)
                .setPaddingLeft(16).setPaddingRight(16)
                .setPaddingTop(8).setPaddingBottom(8)
                .setCharacterSpacing(1.5f).setMarginBottom(52));

        bg.add(hRule(NAVY3, 1f));
        bg.add(spacer(26));

        // Grille métadonnées 2 colonnes
        Table meta = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER);
        double score = ev.getNoteGlobale() != null ? ev.getNoteGlobale() : 0.0;
        meta.addCell(coverMeta("Score global",
                String.format("%.2f / 10", score), bold, regular, false));
        meta.addCell(coverMeta("Decision",
                acc ? "Acceptee" : "Refusee", bold, regular, true));
        meta.addCell(coverMeta("Date de generation",
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")),
                bold, regular, false));
        meta.addCell(coverMeta("Ref. evaluation",
                "#" + ev.getIdEvaluation(), bold, regular, false));
        bg.add(meta);

        bg.add(spacer(36));
        bg.add(new Paragraph("CONFIDENTIEL  -  Document reserve au jury d'evaluation")
                .setFont(regular).setFontSize(8).setFontColor(NAVY_DIM)
                .setTextAlignment(TextAlignment.CENTER));

        cover.addCell(bg);
        return cover;
    }

    private Cell coverMeta(String label, String val, PdfFont bold, PdfFont regular,
                           boolean highlight) {
        Cell c = new Cell().setBorder(Border.NO_BORDER).setPaddingBottom(16);
        c.add(new Paragraph(label.toUpperCase())
                .setFont(regular).setFontSize(8).setFontColor(TEXT_MID)
                .setCharacterSpacing(1).setMarginBottom(4));
        c.add(new Paragraph(val).setFont(bold).setFontSize(14)
                .setFontColor(highlight ? GREEN : ColorConstants.WHITE));
        return c;
    }

    // ════ PAGE 2 — EN-TÊTE ═══════════════════════════════════════

    private Table buildPageHeader(Evaluation ev, PdfFont bold, PdfFont regular) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100));

        Cell left = new Cell().setBorder(Border.NO_BORDER).setBackgroundColor(NAVY2)
                .setPaddingLeft(40).setPaddingTop(20).setPaddingBottom(20).setPaddingRight(20);
        left.add(new Paragraph("BOOSTUP")
                .setFont(bold).setFontSize(15).setFontColor(ColorConstants.WHITE)
                .setCharacterSpacing(3).setMarginBottom(4));
        left.add(new Paragraph("Rapport d'evaluation")
                .setFont(regular).setFontSize(10).setFontColor(INDIGO_L));

        Cell right = new Cell().setBorder(Border.NO_BORDER).setBackgroundColor(NAVY3)
                .setPaddingRight(40).setPaddingTop(20).setPaddingBottom(20).setPaddingLeft(20)
                .setTextAlignment(TextAlignment.RIGHT)
                .setVerticalAlignment(VerticalAlignment.MIDDLE);
        right.add(new Paragraph(nvl(ev.getNomCandidature()))
                .setFont(bold).setFontSize(12).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.RIGHT).setMarginBottom(4));
        right.add(new Paragraph("Genere le " +
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy")))
                .setFont(regular).setFontSize(9).setFontColor(INDIGO_L)
                .setTextAlignment(TextAlignment.RIGHT));

        t.addCell(left); t.addCell(right);
        return t;
    }

    // ════ BLOC DÉCISION ══════════════════════════════════════════

    private Table buildDecisionBlock(Evaluation ev, PdfFont bold, PdfFont regular) {
        boolean   acc  = isAcceptee(ev);
        DeviceRgb bg   = acc ? GREEN_L : RED_L;
        DeviceRgb ac   = acc ? GREEN   : RED;
        DeviceRgb tc   = acc ? new DeviceRgb(0x06, 0x5f, 0x46)
                : new DeviceRgb(0x99, 0x1b, 0x1b);
        double    score = ev.getNoteGlobale() != null ? ev.getNoteGlobale() : 0.0;

        Table t = new Table(UnitValue.createPercentArray(new float[]{3, 2}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginTop(22).setMarginBottom(18)
                .setMarginLeft(40).setMarginRight(40);

        Cell left = new Cell().setBackgroundColor(bg)
                .setBorderLeft(new SolidBorder(ac, 5))
                .setBorderTop(new SolidBorder(ac, 1))
                .setBorderBottom(new SolidBorder(ac, 1))
                .setBorderRight(Border.NO_BORDER).setPadding(22);
        left.add(new Paragraph(acc ? "CANDIDATURE ACCEPTEE" : "CANDIDATURE REFUSEE")
                .setFont(bold).setFontSize(14).setFontColor(tc).setMarginBottom(6));
        left.add(new Paragraph("Decision finale du jury d'evaluation")
                .setFont(regular).setFontSize(10).setFontColor(TEXT_MID));

        Cell right = new Cell().setBackgroundColor(NAVY)
                .setBorderRight(new SolidBorder(ac, 5))
                .setBorderTop(new SolidBorder(ac, 1))
                .setBorderBottom(new SolidBorder(ac, 1))
                .setBorderLeft(Border.NO_BORDER).setPadding(22)
                .setVerticalAlignment(VerticalAlignment.MIDDLE)
                .setTextAlignment(TextAlignment.CENTER);
        right.add(new Paragraph(String.format("%.2f", score))
                .setFont(bold).setFontSize(40).setFontColor(ColorConstants.WHITE)
                .setTextAlignment(TextAlignment.CENTER).setMarginBottom(0));
        right.add(new Paragraph("/ 10  -  Score global")
                .setFont(regular).setFontSize(10).setFontColor(INDIGO_L)
                .setTextAlignment(TextAlignment.CENTER));

        t.addCell(left); t.addCell(right);
        return t;
    }

    // ════ RADAR + BARRES CÔTE À CÔTE ════════════════════════════

    private Table buildRadarAndNotes(Evaluation ev, PdfFont bold, PdfFont regular,
                                     PdfDocument pdf) throws IOException {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100))
                .setMarginLeft(40).setMarginRight(40).setMarginBottom(14);

        Cell radarCell = new Cell().setBorder(Border.NO_BORDER).setPaddingRight(14);
        radarCell.add(sectionTitle("GRAPHIQUE RADAR", bold));
        radarCell.add(buildRadarImage(ev, pdf));
        t.addCell(radarCell);

        Cell barsCell = new Cell().setBorder(Border.NO_BORDER).setPaddingLeft(14);
        barsCell.add(sectionTitle("NOTES PAR CRITERE", bold));
        barsCell.add(buildNotesBars(ev, bold, regular));
        t.addCell(barsCell);

        return t;
    }

    private Image buildRadarImage(Evaluation ev, PdfDocument pdf) throws IOException {
        float size = 210f;
        float cx = size / 2f, cy = size / 2f, r = 74f;
        int   N  = 4;
        double[] angles = { Math.PI/2, 0, -Math.PI/2, Math.PI };
        String[] labels = { "Innovation", "Marche", "Equipe", "Viabilite" };
        int[] notes = {
                ev.getNoteInnovation() != null ? ev.getNoteInnovation() : 0,
                ev.getNoteMarche()     != null ? ev.getNoteMarche()     : 0,
                ev.getNoteEquipe()     != null ? ev.getNoteEquipe()     : 0,
                ev.getNoteViabilite()  != null ? ev.getNoteViabilite()  : 0
        };

        PdfFormXObject xobj = new PdfFormXObject(new Rectangle(size, size));
        PdfCanvas      c    = new PdfCanvas(xobj, pdf);
        PdfFont        lf   = PdfFontFactory.createFont(StandardFonts.HELVETICA_BOLD);

        // Fond
        c.setFillColor(GRAY_L).rectangle(0, 0, size, size).fill();
        c.saveState();
        PdfExtGState bgGs = new PdfExtGState(); bgGs.setFillOpacity(0.45f);
        c.setExtGState(bgGs);
        c.setFillColor(GRAY_M).circle(cx, cy, r + 10).fill();
        c.restoreState();

        // Grilles (5 niveaux)
        for (int lv = 1; lv <= 5; lv++) {
            float lr = r * lv / 5f;
            c.setStrokeColor(lv == 5 ? GRAY_B : GRAY_M)
                    .setLineWidth(lv == 5 ? 1.0f : 0.5f);
            drawNgon(c, cx, cy, lr, N, angles[0]);
        }

        // Axes + marques
        c.setStrokeColor(GRAY_B).setLineWidth(0.7f);
        for (int i = 0; i < N; i++) {
            c.moveTo(cx, cy)
                    .lineTo(cx + (float)(r * Math.cos(angles[i])),
                            cy + (float)(r * Math.sin(angles[i]))).stroke();
        }
        c.setFillColor(TEXT_MID);
        for (int i = 0; i < N; i++)
            for (int lv = 2; lv <= 10; lv += 2)
                c.circle(cx + (float)(r * lv/10.0 * Math.cos(angles[i])),
                        cy + (float)(r * lv/10.0 * Math.sin(angles[i])), 1.5f).fill();

        // Polygone données
        float[] px = new float[N], py = new float[N];
        for (int i = 0; i < N; i++) {
            px[i] = cx + (float)(r * notes[i]/10.0 * Math.cos(angles[i]));
            py[i] = cy + (float)(r * notes[i]/10.0 * Math.sin(angles[i]));
        }

        // Remplissage 20% opaque
        c.saveState();
        PdfExtGState gs = new PdfExtGState(); gs.setFillOpacity(0.20f);
        c.setExtGState(gs);
        c.setFillColor(INDIGO);
        c.moveTo(px[0], py[0]);
        for (int i = 1; i < N; i++) c.lineTo(px[i], py[i]);
        c.closePath().fill();
        c.restoreState();

        // Contour
        c.setStrokeColor(INDIGO).setLineWidth(2.0f);
        c.moveTo(px[0], py[0]);
        for (int i = 1; i < N; i++) c.lineTo(px[i], py[i]);
        c.closePath().stroke();

        // Sommets
        for (int i = 0; i < N; i++) {
            c.saveState();
            PdfExtGState h = new PdfExtGState(); h.setFillOpacity(0.28f);
            c.setExtGState(h);
            c.setFillColor(INDIGO).circle(px[i], py[i], 9).fill();
            c.restoreState();
            c.setFillColor(INDIGO).circle(px[i], py[i], 5).fill();
            c.setFillColor(new DeviceRgb(0xff, 0xff, 0xff)).circle(px[i], py[i], 2.2f).fill();
        }

        // Étiquettes (label + note)
        float[][] lp = {
                { cx-26, cy+r+14 }, { cx+r+7, cy-4 },
                { cx-22, cy-r-24 }, { cx-r-50, cy-4 }
        };
        for (int i = 0; i < N; i++) {
            c.setFillColor(TEXT_DARK)
                    .beginText().setFontAndSize(lf, 8).moveText(lp[i][0], lp[i][1])
                    .showText(labels[i]).endText();
            c.setFillColor(INDIGO)
                    .beginText().setFontAndSize(lf, 8.5f).moveText(lp[i][0]+2, lp[i][1]-12)
                    .showText(notes[i] + "/10").endText();
        }

        c.release();
        return new Image(xobj).setAutoScale(true);
    }

    private void drawNgon(PdfCanvas c, float cx, float cy, float r,
                          int n, double startAngle) {
        for (int i = 0; i <= n; i++) {
            double a = startAngle + i * 2 * Math.PI / n;
            float x = cx + (float)(r * Math.cos(a)), y = cy + (float)(r * Math.sin(a));
            if (i == 0) c.moveTo(x, y); else c.lineTo(x, y);
        }
        c.stroke();
    }

    private Table buildNotesBars(Evaluation ev, PdfFont bold, PdfFont regular) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{3, 5, 1}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(8);
        Object[][] rows = {
                {"Innovation", ev.getNoteInnovation()},
                {"Viabilite",  ev.getNoteViabilite()},
                {"Marche",     ev.getNoteMarche()},
                {"Equipe",     ev.getNoteEquipe()}
        };
        for (int i = 0; i < rows.length; i++) {
            String  label = (String)  rows[i][0];
            Integer note  = (Integer) rows[i][1];
            int     n     = note != null ? note : 0;
            boolean alt   = i % 2 == 1;
            DeviceRgb bg  = alt ? GRAY_L : new DeviceRgb(0xff, 0xff, 0xff);
            DeviceRgb bar = n >= 7 ? GREEN : n >= 5 ? AMBER : RED;

            Cell lc = new Cell().setBorder(Border.NO_BORDER).setBackgroundColor(bg).setPadding(10);
            lc.add(new Paragraph(label).setFont(bold).setFontSize(10).setFontColor(TEXT_DARK));

            // Barre de progression
            Table barre;
            if (n > 0 && n < 10) {
                barre = new Table(UnitValue.createPercentArray(new float[]{n, 10-n}))
                        .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER);
                barre.addCell(new Cell().setHeight(10).setBackgroundColor(bar).setBorder(Border.NO_BORDER));
                barre.addCell(new Cell().setHeight(10).setBackgroundColor(GRAY_M).setBorder(Border.NO_BORDER));
            } else {
                barre = new Table(UnitValue.createPercentArray(new float[]{1}))
                        .setWidth(UnitValue.createPercentValue(100)).setBorder(Border.NO_BORDER);
                barre.addCell(new Cell().setHeight(10)
                        .setBackgroundColor(n >= 10 ? bar : GRAY_M).setBorder(Border.NO_BORDER));
            }

            Cell bc = new Cell().setBorder(Border.NO_BORDER).setBackgroundColor(bg)
                    .setPaddingTop(15).setPaddingBottom(8).setPaddingLeft(6).setPaddingRight(6)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            bc.add(barre);

            Cell nc = new Cell().setBorder(Border.NO_BORDER).setBackgroundColor(bg).setPadding(10)
                    .setVerticalAlignment(VerticalAlignment.MIDDLE);
            nc.add(new Paragraph(String.valueOf(n)).setFont(bold).setFontSize(14)
                    .setFontColor(bar).setTextAlignment(TextAlignment.RIGHT));

            t.addCell(lc); t.addCell(bc); t.addCell(nc);
        }
        return t;
    }

    // ════ COMMENTAIRE ════════════════════════════════════════════

    private Div buildCommentaire(Evaluation ev, PdfFont bold, PdfFont italic) {
        Div d = new Div().setMarginLeft(40).setMarginRight(40).setMarginBottom(18);
        d.add(sectionTitle("COMMENTAIRE & RECOMMANDATIONS", bold));
        d.add(new Paragraph(buildCommentaireAuto(ev))
                .setFont(italic).setFontSize(10.5f).setFontColor(TEXT_DARK)
                .setBackgroundColor(GRAY_L)
                .setBorderLeft(new SolidBorder(INDIGO, 3.5f))
                .setPaddingLeft(18).setPaddingTop(16).setPaddingBottom(16).setPaddingRight(16)
                .setMultipliedLeading(1.65f));
        return d;
    }

    private String buildCommentaireAuto(Evaluation ev) {
        boolean acc   = isAcceptee(ev);
        double  score = ev.getNoteGlobale() != null ? ev.getNoteGlobale() : 0.0;
        StringBuilder sb = new StringBuilder();
        sb.append(acc
                ? "Le jury a decide d'accepter la candidature \"" + nvl(ev.getNomCandidature())
                + "\" (score : " + String.format("%.2f", score) + "/10). "
                : "Le jury n'a pas retenu la candidature \"" + nvl(ev.getNomCandidature())
                + "\" (score : " + String.format("%.2f", score) + "/10). ");
        int max = Math.max(Math.max(
                        ev.getNoteInnovation() != null ? ev.getNoteInnovation() : 0,
                        ev.getNoteViabilite()  != null ? ev.getNoteViabilite()  : 0),
                Math.max(ev.getNoteMarche() != null ? ev.getNoteMarche() : 0,
                        ev.getNoteEquipe() != null ? ev.getNoteEquipe() : 0));
        String pf = "";
        if      (ev.getNoteInnovation() != null && ev.getNoteInnovation() == max) pf = "l'innovation";
        else if (ev.getNoteMarche()     != null && ev.getNoteMarche()     == max) pf = "le potentiel marche";
        else if (ev.getNoteEquipe()     != null && ev.getNoteEquipe()     == max) pf = "la qualite de l'equipe";
        else if (ev.getNoteViabilite()  != null && ev.getNoteViabilite()  == max) pf = "la viabilite financiere";
        if (!pf.isEmpty())
            sb.append("Point fort : ").append(pf).append(" (").append(max).append("/10). ");
        sb.append(acc
                ? "Candidature recommandee pour le programme BOOSTUP."
                : "Il est conseille au candidat de renforcer son dossier avant une nouvelle soumission.");
        return sb.toString();
    }

    // ════ PIED DE PAGE ═══════════════════════════════════════════

    private Table buildFooter(PdfFont bold, PdfFont regular) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1, 1}))
                .setWidth(UnitValue.createPercentValue(100)).setMarginTop(8);
        Cell left = new Cell().setBorder(Border.NO_BORDER).setBackgroundColor(NAVY)
                .setPaddingLeft(40).setPaddingTop(14).setPaddingBottom(14);
        left.add(new Paragraph("BOOSTUP  -  Programme d'Incubation de Startups")
                .setFont(bold).setFontSize(9).setFontColor(INDIGO_L));
        Cell right = new Cell().setBorder(Border.NO_BORDER).setBackgroundColor(NAVY2)
                .setPaddingRight(40).setPaddingTop(14).setPaddingBottom(14)
                .setTextAlignment(TextAlignment.RIGHT).setVerticalAlignment(VerticalAlignment.MIDDLE);
        right.add(new Paragraph("Document confidentiel  -  Jury uniquement")
                .setFont(regular).setFontSize(8).setFontColor(NAVY_DIM)
                .setTextAlignment(TextAlignment.RIGHT));
        t.addCell(left); t.addCell(right);
        return t;
    }

    // ─── HELPERS ─────────────────────────────────────────────────

    private Paragraph sectionTitle(String titre, PdfFont bold) {
        return new Paragraph(titre)
                .setFont(bold).setFontSize(9).setFontColor(TEXT_MID)
                .setCharacterSpacing(1.5f).setMarginBottom(10)
                .setBorderBottom(new SolidBorder(GRAY_M, 1.5f)).setPaddingBottom(5);
    }

    private Table hRule(DeviceRgb color, float h) {
        Table t = new Table(UnitValue.createPercentArray(new float[]{1}))
                .setWidth(UnitValue.createPercentValue(100));
        t.addCell(new Cell().setHeight(h).setBackgroundColor(color).setBorder(Border.NO_BORDER));
        return t;
    }

    private Paragraph spacer(float mb) {
        return new Paragraph(" ").setMarginBottom(mb).setFontSize(1);
    }

    private boolean isAcceptee(Evaluation ev) {
        return "ACCEPTEE".equalsIgnoreCase(ev.getDecision())
                || "VALIDEE".equalsIgnoreCase(ev.getDecision());
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private String sanitize(String s) {
        if (s == null) return "rapport";
        String clean = s.replaceAll("[^a-zA-Z0-9_\\-]", "_");
        return clean.substring(0, Math.min(clean.length(), 30));
    }
}