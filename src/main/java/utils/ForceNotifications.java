package utils;

import entities.GEvenement.Evenement;
import services.EvenementService.EvenementService;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Force-envoie des notifications pour les événements dans 2 jours
 */
public class ForceNotifications {

    public static void main(String[] args) {
        System.out.println("\n╔════════════════════════════════════════════════════════╗");
        System.out.println("║  FORCE ENVOI NOTIFICATIONS WINDOWS - TEST DIRECT      ║");
        System.out.println("╚════════════════════════════════════════════════════════╝\n");

        try {
            EvenementService service = new EvenementService();
            List<Evenement> events = service.getEventsInExactlyTwoDays();

            System.out.println("Événements trouvés : " + events.size());

            if (events.isEmpty()) {
                System.out.println("\n❌ Aucun événement dans 2 jours !");
                System.out.println("Ajoute des événements avec la date : " + LocalDate.now().plusDays(2));
                return;
            }

            System.out.println("\n Envoi des notifications Windows...\n");

            for (Evenement evt : events) {
                String titre = evt.getTitre() != null ? evt.getTitre() : "Événement";
                String lieu = evt.getLieu() != null ? evt.getLieu() : "Lieu non spécifié";

                String dateFormatted;
                if (evt.getDateEvenement() != null) {
                    LocalDate localDate = evt.getDateEvenement().toLocalDate();
                    dateFormatted = localDate.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } else {
                    dateFormatted = "Date non spécifiée";
                }

                System.out.println("Notification " + titre + "...");

                WindowsToastNotifier.sendEventReminder(
                    titre,
                    dateFormatted,
                    lieu,
                    2
                );

                Thread.sleep(600);
            }

            System.out.println("\n✅ TERMINÉ ! Vérifie ton centre de notifications Windows !");
            System.out.println("   (Clique sur l'icône 🔔 en bas à droite)\n");

        } catch (Exception e) {
            System.err.println("❌ Erreur : " + e.getMessage());
            e.printStackTrace();
        }
    }
}

