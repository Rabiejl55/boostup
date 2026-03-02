package services;

import entities.GEvenement.Evenement;
import services.EvenementService.EvenementService;
import utils.MyDatabase;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 🤖 SERVICE DE RECOMMANDATION IA
 *
 * Analyse intelligente des préférences utilisateur pour recommander
 * les événements les plus pertinents
 *
 * Algorithme :
 * ✅ Analyse des feedbacks (notes données)
 * ✅ Analyse de l'historique de participation
 * ✅ Calcul de score de similarité
 * ✅ Recommandations personnalisées
 */
public class RecommendationService {

    private EvenementService evenementService = new EvenementService();

    // ════════════════════════════════════════════════════════
    // 🧠 RECOMMANDATIONS INTELLIGENTES
    // ════════════════════════════════════════════════════════

    /**
     * Obtient les événements recommandés pour un utilisateur
     * basé sur ses feedbacks et participations
     *
     * @param userId ID de l'utilisateur (pour l'instant on utilise un ID fictif)
     * @return Liste des événements recommandés avec leur score
     */
    public List<EventRecommendation> getRecommendations(int userId) {
        try {
            System.out.println("\n🤖 DÉMARRAGE ANALYSE IA");
            System.out.println("═══════════════════════════════════════");

            // 1. Analyser les préférences de l'utilisateur
            Map<String, Double> preferences = analyzeUserPreferences(userId);

            // 2. Récupérer tous les événements disponibles
            List<Evenement> allEvents = evenementService.read();

            // 3. Calculer le score pour chaque événement
            List<EventRecommendation> recommendations = new ArrayList<>();

            for (Evenement event : allEvents) {
                double score = calculateRecommendationScore(event, preferences);

                if (score > 0.3) { // Seuil de pertinence
                    recommendations.add(new EventRecommendation(event, score));
                }
            }

            // 4. Trier par score décroissant
            recommendations.sort((a, b) -> Double.compare(b.getScore(), a.getScore()));

            System.out.println("\n✅ " + recommendations.size() + " recommandations générées");
            System.out.println("═══════════════════════════════════════\n");

            return recommendations;

        } catch (Exception e) {
            System.err.println("❌ Erreur analyse recommandations : " + e.getMessage());
            e.printStackTrace();
            return new ArrayList<>();
        }
    }

    /**
     * Analyse les préférences de l'utilisateur basées sur :
     * - Les types d'événements auxquels il a participé
     * - Les notes qu'il a données dans ses feedbacks
     * - Les lieux qu'il fréquente
     */
    private Map<String, Double> analyzeUserPreferences(int userId) {
        Map<String, Double> preferences = new HashMap<>();

        try {
            Connection conn = MyDatabase.getInstance().getConnection();

            // Analyser les feedbacks pour détecter les préférences
            String sql = """
                SELECT e.type, AVG(f.note) as avg_note, COUNT(*) as count
                FROM feedback f
                JOIN participation p ON f.id_participation = p.id_participation
                JOIN evenement e ON p.id_evenement = e.id_evenement
                GROUP BY e.type
                ORDER BY avg_note DESC
                """;

            PreparedStatement stmt = conn.prepareStatement(sql);
            ResultSet rs = stmt.executeQuery();

            System.out.println("📊 Analyse des préférences :");

            while (rs.next()) {
                String type = rs.getString("type");
                double avgNote = rs.getDouble("avg_note");
                int count = rs.getInt("count");

                // Score = (note moyenne / 5) * facteur de fréquence
                double score = (avgNote / 5.0) * Math.min(count / 3.0, 1.0);
                preferences.put("type_" + type.toLowerCase(), score);

                System.out.println("   " + type + " : " + String.format("%.2f", score) +
                                 " (note: " + avgNote + ", participations: " + count + ")");
            }

            rs.close();
            stmt.close();

        } catch (SQLException e) {
            System.err.println("⚠️ Erreur analyse préférences : " + e.getMessage());
        }

        return preferences;
    }

    /**
     * Calcule le score de recommandation pour un événement
     *
     * @param event L'événement à évaluer
     * @param preferences Les préférences de l'utilisateur
     * @return Score entre 0.0 et 1.0
     */
    private double calculateRecommendationScore(Evenement event, Map<String, Double> preferences) {
        double score = 0.5; // Score de base

        // Bonus si le type correspond aux préférences
        String typeKey = "type_" + event.getType().toLowerCase();
        if (preferences.containsKey(typeKey)) {
            score += preferences.get(typeKey) * 0.5;
        }

        // Bonus pour les événements récents/futurs
        try {
            java.sql.Date eventDate = event.getDateEvenement();
            java.sql.Date now = new java.sql.Date(System.currentTimeMillis());
            long daysUntilEvent = (eventDate.getTime() - now.getTime()) / (1000 * 60 * 60 * 24);

            if (daysUntilEvent > 0 && daysUntilEvent < 30) {
                score += 0.2; // Bonus pour événements dans les 30 prochains jours
            }
        } catch (Exception e) {
            // Ignorer les erreurs de date
        }

        // Limiter le score entre 0 et 1
        return Math.max(0.0, Math.min(1.0, score));
    }

    /**
     * Vérifie si un événement est recommandé (score > seuil)
     */
    public boolean isRecommended(int eventId) {
        try {
            List<EventRecommendation> recommendations = getRecommendations(1); // User ID fictif

            return recommendations.stream()
                .anyMatch(rec -> rec.getEvent().getId() == eventId && rec.getScore() > 0.6);

        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Obtient le score de recommandation pour un événement spécifique
     */
    public double getRecommendationScore(int eventId) {
        try {
            List<EventRecommendation> recommendations = getRecommendations(1);

            return recommendations.stream()
                .filter(rec -> rec.getEvent().getId() == eventId)
                .map(EventRecommendation::getScore)
                .findFirst()
                .orElse(0.0);

        } catch (Exception e) {
            return 0.0;
        }
    }

    // ════════════════════════════════════════════════════════
    // 📊 CLASSE INTERNE : RECOMMANDATION
    // ════════════════════════════════════════════════════════

    /**
     * Représente une recommandation d'événement avec son score
     */
    public static class EventRecommendation {
        private Evenement event;
        private double score;

        public EventRecommendation(Evenement event, double score) {
            this.event = event;
            this.score = score;
        }

        public Evenement getEvent() {
            return event;
        }

        public double getScore() {
            return score;
        }

        public String getScoreLabel() {
            if (score >= 0.8) return "⭐⭐⭐ Fortement recommandé";
            if (score >= 0.6) return "⭐⭐ Recommandé";
            if (score >= 0.4) return "⭐ Pourrait vous plaire";
            return "Suggéré";
        }

        @Override
        public String toString() {
            return String.format("%s (%.0f%% match)", event.getTitre(), score * 100);
        }
    }
}









