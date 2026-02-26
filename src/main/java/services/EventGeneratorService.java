package services;

import entities.GEvenement.Evenement;
import java.sql.Date;
import java.time.LocalDate;
import java.util.Random;
import java.util.List;
import java.util.Arrays;

/**
 * 🤖 SERVICE DE GÉNÉRATION D'ÉVÉNEMENTS PAR IA
 *
 * Génère intelligemment des suggestions d'événements basées sur :
 * - Les tendances actuelles
 * - Les types d'événements populaires
 * - Les dates optimales
 * - Les lieux stratégiques
 */
public class EventGeneratorService {

    private final Random random = new Random();

    // Données de génération IA
    private final List<String> types = Arrays.asList(
        "Conférence", "Atelier", "Séminaire", "Workshop", "Hackathon",
        "Meetup", "Formation", "Webinaire", "Networking", "Pitch Night"
    );

    private final List<String> themes = Arrays.asList(
        "Intelligence Artificielle", "Blockchain", "Développement Durable",
        "Innovation Technologique", "Marketing Digital", "Entrepreneuriat",
        "Cybersécurité", "Data Science", "Cloud Computing", "IoT",
        "Transformation Digitale", "UX/UI Design", "DevOps", "Fintech"
    );

    private final List<String> lieux = Arrays.asList(
        "Paris, Station F", "Lyon, La Sucrière", "Marseille, Le Pharo",
        "Toulouse, La Cantine", "Bordeaux, Darwin", "Nantes, Le Lieu Unique",
        "Lille, Euratechnologies", "Strasbourg, Le Shadok", "Tunis, Cogite",
        "Nice, Sophia Antipolis", "Montpellier, BIC", "En ligne"
    );

    private final List<String> descriptions = Arrays.asList(
        "Une opportunité unique de découvrir les dernières innovations et de networker avec des experts du domaine.",
        "Rejoignez-nous pour une journée intensive d'apprentissage et de partage d'expériences avec des professionnels reconnus.",
        "Participez à cet événement exceptionnel qui réunira les acteurs clés de l'écosystème et des intervenants de renommée internationale.",
        "Découvrez les tendances émergentes et connectez-vous avec une communauté passionnée lors de cette rencontre inspirante.",
        "Un événement incontournable pour approfondir vos connaissances et développer votre réseau professionnel.",
        "Plongez au cœur de l'innovation avec des ateliers pratiques, des conférences inspirantes et des sessions de networking enrichissantes."
    );

    /**
     * 🎯 Génère une suggestion d'événement intelligente
     */
    public Evenement generateEventSuggestion() {
        Evenement event = new Evenement();

        // Type aléatoire
        String type = types.get(random.nextInt(types.size()));
        event.setType(type);

        // Thème aléatoire
        String theme = themes.get(random.nextInt(themes.size()));

        // Titre = Type + Theme + année
        String titre = type + " " + theme + " 2026";
        event.setTitre(titre);

        // Date future aléatoire (entre 7 et 90 jours)
        int daysInFuture = 7 + random.nextInt(84);
        LocalDate futureDate = LocalDate.now().plusDays(daysInFuture);
        event.setDateEvenement(Date.valueOf(futureDate));

        // Lieu aléatoire
        String lieu = lieux.get(random.nextInt(lieux.size()));
        event.setLieu(lieu);

        // Description personnalisée
        String description = descriptions.get(random.nextInt(descriptions.size()));
        description = "🎯 " + type + " sur " + theme + "\n\n" + description;
        event.setDescription(description);

        // Capacité intelligente selon le type
        int capacite = generateSmartCapacity(type);
        event.setCapaciteMax(capacite);

        // Image par défaut
        event.setImage("https://via.placeholder.com/400x225/667eea/ffffff?text=" +
                      type.replace(" ", "+"));

        System.out.println("\n🤖 ÉVÉNEMENT GÉNÉRÉ PAR IA");
        System.out.println("═══════════════════════════════════════");
        System.out.println("📋 Titre : " + titre);
        System.out.println("🎯 Type : " + type);
        System.out.println("📅 Date : " + futureDate);
        System.out.println("📍 Lieu : " + lieu);
        System.out.println("👥 Capacité : " + capacite);
        System.out.println("═══════════════════════════════════════\n");

        return event;
    }

    /**
     * Génère une capacité intelligente selon le type d'événement
     */
    private int generateSmartCapacity(String type) {
        switch (type.toLowerCase()) {
            case "conférence":
            case "séminaire":
                return 150 + random.nextInt(100); // 150-250
            case "atelier":
            case "workshop":
            case "formation":
                return 20 + random.nextInt(30); // 20-50
            case "hackathon":
                return 50 + random.nextInt(100); // 50-150
            case "meetup":
            case "networking":
                return 30 + random.nextInt(70); // 30-100
            case "webinaire":
                return 200 + random.nextInt(50); // 200-250
            case "pitch night":
                return 80 + random.nextInt(120); // 80-200
            default:
                return 50 + random.nextInt(150); // 50-200
        }
    }
}

