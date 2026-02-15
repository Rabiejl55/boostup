package Main;

import entities.GFinancement.Projet;
import entities.GFinancement.Investissement;
import entities.GFinancement.Transaction_Financiere;

import services.FinancementService.ProjetService;
import services.FinancementService.InvestissementService;
import services.FinancementService.TransactionFinanciereService;

import java.util.List;
import java.util.Scanner;

public class MainF {
    public static void main(String[] args) {
        try {
            ProjetService projetService = new ProjetService();
            InvestissementService investissementService = new InvestissementService();
            TransactionFinanciereService transactionService = new TransactionFinanciereService();

            Scanner sc = new Scanner(System.in);
            boolean continuer = true;

            while (continuer) {
                System.out.println("\n=== Gestion Financement (Projet / Investissement / Transaction) ===");
                System.out.println("Choisissez l'entité à manipuler :");
                System.out.println("1️⃣ Projet");
                System.out.println("2️⃣ Investissement");
                System.out.println("3️⃣ Transaction Financière");
                System.out.println("0️⃣ Quitter");
                System.out.print("Votre choix : ");

                int choixEntite = sc.nextInt();
                sc.nextLine();

                switch (choixEntite) {

                    // ========================= PROJET =========================
                    case 1:
                        System.out.println("\n--- Gestion des Projets ---");
                        System.out.println("1. Ajouter un projet");
                        System.out.println("2. Modifier un projet");
                        System.out.println("3. Supprimer un projet");
                        System.out.println("4. Afficher tous les projets");
                        System.out.print("Choix : ");
                        int choixProjet = sc.nextInt();
                        sc.nextLine();

                        switch (choixProjet) {
                            case 1: {
                                System.out.print("Titre : ");
                                String titre = sc.nextLine();
                                System.out.print("Description : ");
                                String description = sc.nextLine();
                                System.out.print("Budget : ");
                                double budget = sc.nextDouble();
                                sc.nextLine();
                                System.out.print("Statut : ");
                                String statut = sc.nextLine();

                                Projet p = new Projet(0, titre, description, budget, statut);
                                projetService.ajouter(p);
                                System.out.println("✅ Projet ajouté !");
                                break;
                            }
                            case 2: {
                                List<Projet> projets = projetService.read();
                                System.out.println("--- Projets disponibles ---");
                                for (Projet p : projets) System.out.println(p);

                                System.out.print("ID du projet à modifier : ");
                                int idModif = sc.nextInt();
                                sc.nextLine();

                                Projet projetModif = null;
                                for (Projet p : projets) {
                                    if (p.getId_projet() == idModif) {
                                        projetModif = p;
                                        break;
                                    }
                                }

                                if (projetModif != null) {
                                    System.out.print("Nouveau titre : ");
                                    projetModif.setTitre(sc.nextLine());
                                    System.out.print("Nouvelle description : ");
                                    projetModif.setDescription(sc.nextLine());
                                    System.out.print("Nouveau budget : ");
                                    projetModif.setBudget(sc.nextDouble());
                                    sc.nextLine();
                                    System.out.print("Nouveau statut : ");
                                    projetModif.setStatut(sc.nextLine());

                                    projetService.update(projetModif);
                                    System.out.println("✅ Projet modifié !");
                                } else {
                                    System.out.println("❌ Aucun projet trouvé avec cet ID !");
                                }
                                break;
                            }
                            case 3: {
                                List<Projet> projets = projetService.read();
                                System.out.println("--- Projets disponibles ---");
                                for (Projet p : projets) System.out.println(p);

                                System.out.print("ID du projet à supprimer : ");
                                int idSuppr = sc.nextInt();
                                sc.nextLine();

                                projetService.supprimer(idSuppr);
                                System.out.println("✅ Projet supprimé !");
                                break;
                            }
                            case 4: {
                                List<Projet> projets = projetService.read();
                                System.out.println("--- Liste des projets ---");
                                for (Projet p : projets) System.out.println(p);
                                break;
                            }
                            default:
                                System.out.println("❌ Choix invalide !");
                        }
                        break;

                    // ========================= INVESTISSEMENT =========================
                    case 2:
                        System.out.println("\n--- Gestion des Investissements ---");
                        System.out.println("1. Ajouter un investissement");
                        System.out.println("2. Modifier un investissement");
                        System.out.println("3. Supprimer un investissement");
                        System.out.println("4. Afficher tous les investissements");
                        System.out.print("Choix : ");
                        int choixInv = sc.nextInt();
                        sc.nextLine();

                        switch (choixInv) {
                            case 1: {
                                // Pour éviter erreurs FK : montrer projets existants
                                List<Projet> projets = projetService.read();
                                System.out.println("--- Projets disponibles ---");
                                for (Projet p : projets) System.out.println(p);

                                System.out.print("Montant : ");
                                double montant = sc.nextDouble();
                                sc.nextLine();
                                System.out.print("Statut : ");
                                String statut = sc.nextLine();
                                System.out.print("Date (AAAA-MM-JJ) : ");
                                String date = sc.nextLine();

                                System.out.print("ID Projet : ");
                                int idProjet = sc.nextInt();
                                sc.nextLine();

                                System.out.print("ID User : ");
                                int idUser = sc.nextInt();
                                sc.nextLine();

                                Investissement inv = new Investissement(0, montant, statut, date, idProjet, idUser);
                                investissementService.ajouter(inv);
                                System.out.println("✅ Investissement ajouté !");
                                break;
                            }
                            case 2: {
                                List<Investissement> investissements = investissementService.read();
                                System.out.println("--- Investissements disponibles ---");
                                for (Investissement i : investissements) System.out.println(i);

                                System.out.print("ID de l'investissement à modifier : ");
                                int idModif = sc.nextInt();
                                sc.nextLine();

                                Investissement invModif = null;
                                for (Investissement i : investissements) {
                                    if (i.getId_investissement() == idModif) {
                                        invModif = i;
                                        break;
                                    }
                                }

                                if (invModif != null) {
                                    System.out.print("Nouveau montant : ");
                                    invModif.setMontantInvestissement(sc.nextDouble());
                                    sc.nextLine();
                                    System.out.print("Nouveau statut : ");
                                    invModif.setStatut(sc.nextLine());
                                    System.out.print("Nouvelle date (AAAA-MM-JJ) : ");
                                    invModif.setDate_investissement(sc.nextLine());

                                    System.out.print("Nouveau ID projet : ");
                                    invModif.setId_projet(sc.nextInt());
                                    sc.nextLine();

                                    System.out.print("Nouveau ID user : ");
                                    invModif.setId_user(sc.nextInt());
                                    sc.nextLine();

                                    investissementService.update(invModif);
                                    System.out.println("✅ Investissement modifié !");
                                } else {
                                    System.out.println("❌ Aucun investissement trouvé avec cet ID !");
                                }
                                break;
                            }
                            case 3: {
                                List<Investissement> investissements = investissementService.read();
                                System.out.println("--- Investissements disponibles ---");
                                for (Investissement i : investissements) System.out.println(i);

                                System.out.print("ID de l'investissement à supprimer : ");
                                int idSuppr = sc.nextInt();
                                sc.nextLine();

                                investissementService.supprimer(idSuppr);
                                System.out.println("✅ Investissement supprimé !");
                                break;
                            }
                            case 4: {
                                List<Investissement> investissements = investissementService.read();
                                System.out.println("--- Liste des investissements ---");
                                for (Investissement i : investissements) System.out.println(i);
                                break;
                            }
                            default:
                                System.out.println("❌ Choix invalide !");
                        }
                        break;

                    // ========================= TRANSACTION =========================
                    case 3:
                        System.out.println("\n--- Gestion des Transactions Financières ---");
                        System.out.println("1. Ajouter une transaction");
                        System.out.println("2. Modifier une transaction");
                        System.out.println("3. Supprimer une transaction");
                        System.out.println("4. Afficher toutes les transactions");
                        System.out.print("Choix : ");
                        int choixTrans = sc.nextInt();
                        sc.nextLine();

                        switch (choixTrans) {
                            case 1: {
                                // Pour éviter erreurs FK : montrer investissements existants
                                List<Investissement> investissements = investissementService.read();
                                System.out.println("--- Investissements disponibles ---");
                                for (Investissement i : investissements) System.out.println(i);

                                System.out.print("Montant transaction : ");
                                double montant = sc.nextDouble();
                                sc.nextLine();
                                System.out.print("Date (AAAA-MM-JJ) : ");
                                String date = sc.nextLine();
                                System.out.print("Mode paiement : ");
                                String mode = sc.nextLine();
                                System.out.print("Statut transaction : ");
                                String statut = sc.nextLine();

                                System.out.print("ID Investissement : ");
                                int idInv = sc.nextInt();
                                sc.nextLine();

                                Transaction_Financiere t =
                                        new Transaction_Financiere(0, montant, date, mode, statut, idInv);

                                transactionService.ajouter(t);
                                System.out.println("✅ Transaction ajoutée !");
                                break;
                            }
                            case 2: {
                                List<Transaction_Financiere> transactions = transactionService.read();
                                System.out.println("--- Transactions disponibles ---");
                                for (Transaction_Financiere t : transactions) System.out.println(t);

                                System.out.print("ID de la transaction à modifier : ");
                                int idModif = sc.nextInt();
                                sc.nextLine();

                                Transaction_Financiere tModif = null;
                                for (Transaction_Financiere t : transactions) {
                                    if (t.getId_transaction() == idModif) {
                                        tModif = t;
                                        break;
                                    }
                                }

                                if (tModif != null) {
                                    System.out.print("Nouveau montant : ");
                                    tModif.setMontantTransaction(sc.nextDouble());
                                    sc.nextLine();
                                    System.out.print("Nouvelle date (AAAA-MM-JJ) : ");
                                    tModif.setDate_transaction(sc.nextLine());
                                    System.out.print("Nouveau mode : ");
                                    tModif.setMode_paiement(sc.nextLine());
                                    System.out.print("Nouveau statut : ");
                                    tModif.setStatut_transaction(sc.nextLine());

                                    System.out.print("Nouvel ID investissement : ");
                                    tModif.setId_investissement(sc.nextInt());
                                    sc.nextLine();

                                    transactionService.update(tModif);
                                    System.out.println("✅ Transaction modifiée !");
                                } else {
                                    System.out.println("❌ Aucune transaction trouvée avec cet ID !");
                                }
                                break;
                            }
                            case 3: {
                                List<Transaction_Financiere> transactions = transactionService.read();
                                System.out.println("--- Transactions disponibles ---");
                                for (Transaction_Financiere t : transactions) System.out.println(t);

                                System.out.print("ID de la transaction à supprimer : ");
                                int idSuppr = sc.nextInt();
                                sc.nextLine();

                                transactionService.supprimer(idSuppr);
                                System.out.println("✅ Transaction supprimée !");
                                break;
                            }
                            case 4: {
                                List<Transaction_Financiere> transactions = transactionService.read();
                                System.out.println("--- Liste des transactions ---");
                                for (Transaction_Financiere t : transactions) System.out.println(t);
                                break;
                            }
                            default:
                                System.out.println("❌ Choix invalide !");
                        }
                        break;

                    case 0:
                        continuer = false;
                        System.out.println("👋 Au revoir !");
                        break;

                    default:
                        System.out.println("❌ Choix invalide !");
                }
            }

            sc.close();

        } catch (Exception e) {
            System.out.println("❌ Une erreur est survenue !");
            e.printStackTrace();
        }
    }
}
