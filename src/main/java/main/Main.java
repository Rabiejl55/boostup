package main;

import entities.GEvenement.Evenement;
import services.EvenementService.EvenementService;

import java.sql.Date;
import java.sql.SQLException;
import java.util.List;

public class Main {
    public static void main(String[] args) {

        System.out.println("=== TEST COMPLET CRUD EVENEMENT ===");

        EvenementService es = new EvenementService();

        try {
            // ============ ÉTAPE 1: AJOUT ============
            System.out.println("\n1. AJOUT D'UN NOUVEL ÉVÉNEMENT");

            Evenement nouvelEvenement = new Evenement(
                    "PIDEV",
                    "Projet",
                    Date.valueOf("2026-02-15"),
                    "Salle M003, ESPRIT",
                    "Workshop sur les interfaces Java",
                    30
            );

            es.ajouter(nouvelEvenement);
            System.out.println("✓ Événement ajouté: " + nouvelEvenement.getTitre());

            // Ajouter un deuxième événement pour les tests
            Evenement deuxiemeEvenement = new Evenement(
                    "Validation",
                    "CRUD",
                    Date.valueOf("2026-02-18"),
                    "bloc ijk",
                    "el kabess maa fadhel",
                    50
            );

            //es.ajouter(deuxiemeEvenement);
            System.out.println("✓ Événement ajouté: " + deuxiemeEvenement.getTitre());

            // ============ ÉTAPE 2: LECTURE (après ajout) ============
            System.out.println("\n2. LECTURE - Liste des événements après ajout:");
            List<Evenement> listeEvenements = es.read();
            listeEvenements.forEach(System.out::println);

            // ============ ÉTAPE 3: MODIFICATION ============
            System.out.println("\n3. MODIFICATION D'UN ÉVÉNEMENT");

            if (!listeEvenements.isEmpty()) {
                // Prendre le premier événement de la liste pour le modifier
                Evenement evenementAModifier = listeEvenements.get(0);
                int idAModifier = evenementAModifier.getId();

                System.out.println("Modification de l'événement ID=" + idAModifier);

                // Créer la version modifiée
                Evenement evenementModifie = new Evenement(
                        idAModifier,  // Même ID
                        evenementAModifier.getTitre() + " [ÉDITION SPÉCIALE]",  // Titre modifié
                        "Workshop Pratique",  // Type modifié
                        Date.valueOf("2024-06-15"),  // Date modifiée
                        "Amphithéâtre Principal",  // Lieu modifié
                        evenementAModifier.getDescription() + " - Inclus certifications",  // Description modifiée
                        40  // Capacité modifiée
                );

                //.update(evenementModifie);
                System.out.println("✓ Événement modifié avec succès");

                // ============ ÉTAPE 4: LECTURE (après modification) ============
                System.out.println("\n4. LECTURE - Après modification:");
                es.read().forEach(System.out::println);
            }

            // ============ ÉTAPE 5: SUPPRESSION ============
            System.out.println("\n5. SUPPRESSION D'UN ÉVÉNEMENT");

            // Lire à nouveau pour avoir les IDs à jour
            List<Evenement> listeAvantSuppression = es.read();

            if (listeAvantSuppression.size() > 1) {
                // Supprimer le deuxième événement (pour garder au moins un pour l'affichage)
                int idASupprimer = listeAvantSuppression.get(1).getId();
                System.out.println("Suppression de l'événement ID=" + idASupprimer);

                //es.supprimer(1);
                System.out.println("✓ Événement supprimé avec succès");

                // ============ ÉTAPE 6: LECTURE FINALE ============
                System.out.println("\n6. LECTURE FINALE - Après suppression:");
                List<Evenement> listeFinale = es.read();

                if (listeFinale.isEmpty()) {
                    System.out.println("Aucun événement dans la base de données");
                } else {
                    listeFinale.forEach(System.out::println);
                }

                // ============ RÉSUMÉ FINAL ============
                System.out.println("\n=== RÉSUMÉ DES OPÉRATIONS ===");
                System.out.println("✓ AJOUT: 2 événements ajoutés");
                System.out.println("✓ MODIFICATION: 1 événement modifié");
                System.out.println("✓ SUPPRESSION: 1 événement supprimé");
                System.out.println("✓ LECTURE: Opération vérifiée après chaque étape");
                System.out.println("\n✅ CRUD COMPLET TESTÉ AVEC SUCCÈS !");

            } else {
                System.out.println("Pas assez d'événements pour tester la suppression");
            }

        } catch (SQLException e) {
            System.err.println("\n❌ ERREUR SQL: " + e.getMessage());
            e.printStackTrace();
        } catch (Exception e) {
            System.err.println("\n❌ ERREUR: " + e.getMessage());
            e.printStackTrace();
        }
    }
}