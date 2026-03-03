package utils;

import entities.GEvenement.Evenement;
import services.EvenementService.EvenementService;

import java.sql.SQLException;
import java.time.LocalDate;
import java.util.List;

/**
 * Script de debug pour vérifier les événements et les notifications
 */
public class DebugNotifications {

    public static void main(String[] args) throws SQLException {
        System.out.println("\n╔═══════════════════════════════════════════════════════╗");
        System.out.println("║  🔍 DEBUG NOTIFICATIONS - VÉRIFICATION BDD           ║");
        System.out.println("╚═══════════════════════════════════════════════════════╝\n");

        LocalDate today = LocalDate.now();
        LocalDate inTwoDays = today.plusDays(2);

        System.out.println("📅 Date d'aujourd'hui : " + today);
        System.out.println("📅 Dans 2 jours sera  : " + inTwoDays);
        System.out.println();

        EvenementService service = new EvenementService();

        try {
            // 1. Tous les événements actifs
            System.out.println("════════════════════════════════════════════════════");
            System.out.println("📋 TOUS LES ÉVÉNEMENTS ACTIFS DANS LA BASE");
            System.out.println("════════════════════════════════════════════════════");

            List<Evenement> all = service.readActifs();
            System.out.println("Total : " + all.size() + " événements actifs\n");

            if (all.isEmpty()) {
                System.out.println("❌ AUCUN ÉVÉNEMENT ACTIF TROUVÉ !");
                System.out.println("   → Vérifie que tu as bien des événements dans la table 'evenement'");
                System.out.println("   → Vérifie que le champ 'archive' = 0 (non archivé)");
                return;
            }

            for (Evenement e : all) {
                System.out.println("📌 " + e.getTitre());
                System.out.println("   ID          : " + e.getId());
                System.out.println("   Type        : " + e.getType());
                System.out.println("   Date        : " + (e.getDateEvenement() != null ? e.getDateEvenement().toLocalDate() : "NULL"));
                System.out.println("   Lieu        : " + e.getLieu());
                System.out.println("   Capacité    : " + e.getCapaciteMax());

                if (e.getDateEvenement() != null) {
                    LocalDate eventDate = e.getDateEvenement().toLocalDate();
                    long daysUntil = java.time.temporal.ChronoUnit.DAYS.between(today, eventDate);
                    System.out.println("   ⏳ Dans " + daysUntil + " jour(s)");

                    if (eventDate.equals(inTwoDays)) {
                        System.out.println("   ✅ CET ÉVÉNEMENT DEVRAIT DÉCLENCHER UNE NOTIFICATION !");
                    }
                }

                System.out.println();
            }

            // 2. Test de la méthode getEventsInExactlyTwoDays
            System.out.println("════════════════════════════════════════════════════");
            System.out.println("🔔 TEST DE LA MÉTHODE getEventsInExactlyTwoDays()");
            System.out.println("════════════════════════════════════════════════════");

            List<Evenement> eventsIn2Days = service.getEventsInExactlyTwoDays();

            System.out.println("\n✅ Résultat : " + eventsIn2Days.size() + " événement(s) trouvé(s) dans 2 jours\n");

            if (eventsIn2Days.isEmpty()) {
                System.out.println("⚠️  AUCUN ÉVÉNEMENT DANS 2 JOURS !");
                System.out.println("\n💡 SOLUTIONS :");
                System.out.println("   1. Vérifie que la date de l'événement est bien : " + inTwoDays);
                System.out.println("   2. Dans MySQL/PhpMyAdmin, exécute :");
                System.out.println("      SELECT titre, date_evenement, archive FROM evenement;");
                System.out.println("   3. Assure-toi que archive = 0");
                System.out.println("   4. Le format de date doit être : " + inTwoDays + " (YYYY-MM-DD)");
            } else {
                System.out.println("🎉 ÉVÉNEMENTS QUI DÉCLENCHERONT DES NOTIFICATIONS :");
                for (Evenement e : eventsIn2Days) {
                    System.out.println("   ✅ " + e.getTitre() + " le " + e.getDateEvenement().toLocalDate());
                }
            }

        } catch (SQLException e) {
            System.err.println("\n❌ ERREUR SQL : " + e.getMessage());
            e.printStackTrace();
        }

        System.out.println("\n════════════════════════════════════════════════════");
        System.out.println("FIN DU DEBUG");
        System.out.println("════════════════════════════════════════════════════\n");
    }
}

