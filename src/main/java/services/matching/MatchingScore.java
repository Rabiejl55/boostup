package services.matching;

import java.util.LinkedHashMap;
import java.util.Map;

public class MatchingScore {
    private final int score; // 0..100
    private final Map<String, Integer> breakdown; // "+30 Secteur", ...

    public MatchingScore(int score, Map<String, Integer> breakdown) {
        this.score = Math.max(0, Math.min(100, score));
        this.breakdown = (breakdown == null) ? new LinkedHashMap<>() : new LinkedHashMap<>(breakdown);
    }

    public int getScore() { return score; }
    public Map<String, Integer> getBreakdown() { return new LinkedHashMap<>(breakdown); }

    public String shortExplain() {
        // ex: "+30 secteur, +20 stade, +18 ticket"
        StringBuilder sb = new StringBuilder();
        int i = 0;
        for (var e : breakdown.entrySet()) {
            if (i >= 3) break;
            if (sb.length() > 0) sb.append(", ");
            sb.append((e.getValue() >= 0 ? "+" : "")).append(e.getValue()).append(" ").append(e.getKey().toLowerCase());
            i++;
        }
        return sb.toString();
    }

    public String fullExplain() {
        StringBuilder sb = new StringBuilder();
        int total = 0;
        for (var e : breakdown.entrySet()) {
            sb.append((e.getValue() >= 0 ? "+" : "")).append(e.getValue()).append("  ").append(e.getKey()).append("\n");
            total += e.getValue();
        }
        sb.append("\nTotal = ").append(Math.max(0, Math.min(100, total))).append("/100");
        return sb.toString();
    }
}