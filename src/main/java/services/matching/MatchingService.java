package services.matching;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

public class MatchingService {

    // Pondérations (comme ton cahier)
    private static final int W_SECTOR = 30;
    private static final int W_STAGE  = 20;
    private static final int W_TICKET = 25;
    private static final int W_LOC    = 10;
    private static final int W_TRUST  = 15;

    // --- Profil investisseur (version simple) ---
    // Tu pourras plus tard le charger depuis DB (table investor_profile)
    public static class InvestorProfile {
        public Set<String> preferredSectors = Set.of("FinTech", "SaaS", "HealthTech"); // exemple
        public Set<String> preferredStages  = Set.of("Pre-seed", "Seed", "Series A");
        public double ticketMin = 10000;
        public double ticketMax = 100000;
        public String location  = "TN";
        public int trustScore   = 80; // 0..100 (historique/fiabilité)
    }

    public MatchingScore scoreOpportunity(
            InvestorProfile inv,
            String projetText,     // titre/description/meta concat
            String sectorSelected, // filtre UI si tu veux
            String stageSelected,
            double budget,
            double totalLeve
    ) {
        Map<String, Integer> b = new LinkedHashMap<>();
        int total = 0;

        String detectedSector = detectSector(projetText);
        String detectedStage  = detectStage(projetText);
        String detectedLoc    = detectLocation(projetText);

        // 1) Secteur
        int s = 0;
        if (inv != null && detectedSector != null && !detectedSector.isBlank()) {
            if (containsIgnoreCase(inv.preferredSectors, detectedSector)) s = W_SECTOR;
            else if (sectorSelected != null && !"Tous".equalsIgnoreCase(sectorSelected)
                    && detectedSector.equalsIgnoreCase(sectorSelected)) s = (int)(W_SECTOR * 0.8);
        }
        b.put("Secteur", s); total += s;

        // 2) Stade
        int st = 0;
        if (inv != null && detectedStage != null && !detectedStage.isBlank()) {
            if (containsIgnoreCase(inv.preferredStages, detectedStage)) st = W_STAGE;
            else if (stageSelected != null && !"Tous".equalsIgnoreCase(stageSelected)
                    && detectedStage.equalsIgnoreCase(stageSelected)) st = (int)(W_STAGE * 0.7);
        }
        b.put("Stade", st); total += st;

        // 3) Ticket vs besoin restant
        double reste = Math.max(0, budget - totalLeve);
        int t = scoreTicket(inv, reste);
        b.put("Ticket", t); total += t;

        // 4) Localisation
        int l = 0;
        if (inv != null) {
            if (inv.location != null && detectedLoc != null && inv.location.equalsIgnoreCase(detectedLoc)) l = W_LOC;
            else if ("TN".equalsIgnoreCase(inv.location) && (detectedLoc == null || detectedLoc.isBlank())) l = (int)(W_LOC * 0.6); // défaut TN
        }
        b.put("Localisation", l); total += l;

        // 5) Fiabilité (trust)
        int tr = 0;
        if (inv != null) {
            // convert trustScore 0..100 => 0..15
            tr = (int) Math.round((inv.trustScore / 100.0) * W_TRUST);
        }
        b.put("Fiabilité", tr); total += tr;

        return new MatchingScore(total, b);
    }

    private int scoreTicket(InvestorProfile inv, double reste) {
        if (inv == null) return 0;
        if (reste <= 0) return 0;

        // Si reste dans [min,max] => 25
        if (reste >= inv.ticketMin && reste <= inv.ticketMax) return W_TICKET;

        // proche => partiel
        if (reste < inv.ticketMin) {
            double ratio = reste / Math.max(1.0, inv.ticketMin); // 0..1
            return (int) Math.round(W_TICKET * Math.max(0.2, ratio)); // min 20% si pas trop loin
        } else {
            // reste > max : plus c'est au-dessus, plus ça baisse
            double ratio = inv.ticketMax / Math.max(1.0, reste); // <1
            return (int) Math.round(W_TICKET * Math.max(0.2, ratio));
        }
    }

    private boolean containsIgnoreCase(Set<String> set, String value) {
        if (set == null || value == null) return false;
        for (String s : set) if (s != null && s.equalsIgnoreCase(value)) return true;
        return false;
    }

    // --- Détection simple depuis le texte (sans colonnes DB) ---
    private String detectSector(String text) {
        if (text == null) return "";
        String t = text.toLowerCase(Locale.ROOT);
        if (t.contains("fintech")) return "FinTech";
        if (t.contains("health") || t.contains("med") || t.contains("clinique")) return "HealthTech";
        if (t.contains("edtech") || t.contains("école") || t.contains("education")) return "EdTech";
        if (t.contains("agri") || t.contains("ferme") || t.contains("agriculture")) return "AgriTech";
        if (t.contains("saas")) return "SaaS";
        if (t.contains("e-commerce") || t.contains("ecommerce") || t.contains("boutique")) return "E-commerce";
        return "";
    }

    private String detectStage(String text) {
        if (text == null) return "";
        String t = text.toLowerCase(Locale.ROOT);
        if (t.contains("series a") || t.contains("série a")) return "Series A";
        if (t.contains("seed")) return "Seed";
        if (t.contains("pre-seed") || t.contains("preseed")) return "Pre-seed";
        if (t.contains("idea") || t.contains("idée")) return "Idea";
        return "";
    }

    private String detectLocation(String text) {
        if (text == null) return "";
        String t = text.toLowerCase(Locale.ROOT);
        // ultra simple: tu peux enrichir
        if (t.contains("tunisie") || t.contains("tn") || t.contains("tunis") || t.contains("sfax") || t.contains("sousse")) return "TN";
        if (t.contains("qatar") || t.contains("doha") || t.contains("qa")) return "QA";
        return "";
    }
}