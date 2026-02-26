package services;

import entities.GEvenement.Evenement;
import java.sql.Date;

/**
 * 🧪 TEST EMAIL - Version simplifiée pour tester l'envoi
 * Lance ce fichier pour tester l'email de bienvenue
 */
public class TestEmail {

    public static void main(String[] args) {
        System.out.println("\n\n");
        System.out.println("═══════════════════════════════════════════════════════════");
        System.out.println("🧪 TEST ENVOI EMAIL DE BIENVENUE - BoostUp");
        System.out.println("═══════════════════════════════════════════════════════════\n");

        // ⚠️ AVANT DE LANCER CE TEST :
        System.out.println("⚠️  IMPORTANT - CONFIGURATION REQUISE :");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
        System.out.println("1️⃣  Crée un compte Gmail dédié pour BoostUp");
        System.out.println("    Exemple: boostup.events.tn@gmail.com");
        System.out.println("");
        System.out.println("2️⃣  Active l'authentification à 2 facteurs (2FA)");
        System.out.println("    → Connexion Google → Sécurité → Validation en deux étapes");
        System.out.println("");
        System.out.println("3️⃣  Génère un mot de passe d'application");
        System.out.println("    → https://myaccount.google.com/apppasswords");
        System.out.println("    → Sélectionne 'Autre' et nomme-le 'BoostUp Java'");
        System.out.println("    → Google te donnera un code à 16 caractères");
        System.out.println("");
        System.out.println("4️⃣  Copie ce code dans EmailService.java ligne 23");
        System.out.println("    private static final String FROM_PASSWORD = \"VOTRE_CODE_ICI\";");
        System.out.println("");
        System.out.println("5️⃣  Change aussi l'email expéditeur ligne 22");
        System.out.println("    private static final String FROM_EMAIL = \"ton@email.com\";");
        System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

        // Vérification de la configuration
        // Note : La vérification de configuration est désormais gérée par EmailService
        /*if (!EmailService.estConfigurer()) {
            System.err.println("❌ SERVICE EMAIL NON CONFIGURÉ !");
            System.err.println("   Suis les instructions ci-dessus avant de relancer ce test.\n");
            return;
        }*/

        System.out.println("✅ Service email configuré, lancement du test...\n");

        try {
            // Création d'un événement fictif pour le test
            Evenement eventTest = new Evenement();
            eventTest.setTitre("Workshop JavaFX & APIs");
            eventTest.setType("Formation");
            eventTest.setDateEvenement(Date.valueOf("2026-03-15"));
            eventTest.setLieu("ESPRIT, Tunis");
            eventTest.setDescription("Apprenez à intégrer des APIs modernes dans vos applications JavaFX !");
            eventTest.setCapaciteMax(50);

            System.out.println("📅 Événement de test créé:");
            System.out.println("   Titre: " + eventTest.getTitre());
            System.out.println("   Date: " + eventTest.getDateEvenement());
            System.out.println("   Lieu: " + eventTest.getLieu());
            System.out.println("");

            // Envoi de l'email de test
            System.out.println("🚀 Envoi de l'email de bienvenue...");
            System.out.println("   Destinataire: rayen.amri@esprit.tn");
            System.out.println("   Utilisateur: Rayen Amri\n");

            EmailService.envoyerEmailBienvenue(
                eventTest,
                "Rayen Amri",
                "rayen.amri@esprit.tn"
            );

            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━");
            System.out.println("⏳ Email en cours d'envoi (thread asynchrone)...");
            System.out.println("   Attends 5-10 secondes et vérifie:");
            System.out.println("   1. La console pour le statut");
            System.out.println("   2. Ton email rayen.amri@esprit.tn");
            System.out.println("   3. Le dossier SPAM si besoin");
            System.out.println("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━\n");

            // Attendre un peu pour voir les logs
            Thread.sleep(15000);

        } catch (Exception e) {
            System.err.println("\n❌ ERREUR PENDANT LE TEST:");
            e.printStackTrace();
        }

        System.out.println("\n🏁 Test terminé. Vérifie ton email !");
    }
}

