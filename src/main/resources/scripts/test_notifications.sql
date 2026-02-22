-- =========================================
-- SCRIPT DE TEST - NOTIFICATIONS BOOSTUP
-- =========================================
-- Ce script ajoute un événement test dans exactement 2 jours
-- pour vérifier que les notifications fonctionnent.

USE boostup;

-- Supprimer l'événement de test s'il existe déjà
DELETE FROM evenement WHERE titre = 'Test Notification - Rappel 2 jours';

-- Ajouter un événement dans exactement 2 jours
INSERT INTO evenement (titre, type, date_evenement, lieu, description, capacite_max, image, archived)
VALUES (
    'Test Notification - Rappel 2 jours',
    'Conférence',
    DATE_ADD(CURDATE(), INTERVAL 2 DAY),
    'Paris, Station F',
    'Événement de test pour vérifier le système de notifications automatiques.',
    100,
    'https://images.unsplash.com/photo-1540575467063-178a50c2df87',
    0
);

-- Vérifier que l'événement a bien été ajouté
SELECT
    id_evenement,
    titre,
    type,
    date_evenement,
    DATEDIFF(date_evenement, CURDATE()) AS jours_restants,
    lieu,
    archived
FROM evenement
WHERE titre = 'Test Notification - Rappel 2 jours';

-- =========================================
-- RÉSULTAT ATTENDU
-- =========================================
-- La colonne "jours_restants" doit afficher: 2
-- La colonne "archived" doit afficher: 0
--
-- Maintenant, lancez l'application:
-- mvn clean javafx:run
--
-- Une notification devrait apparaître automatiquement
-- environ 1 seconde après l'affichage de la page login !
-- =========================================

-- BONUS: Ajouter plusieurs événements pour tester l'affichage de plusieurs notifications
INSERT INTO evenement (titre, type, date_evenement, lieu, description, capacite_max, archived)
VALUES
    ('Workshop IA & Innovation', 'Atelier', DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'Lyon Tech Hub', 'Atelier pratique sur l\'intelligence artificielle', 50, 0),
    ('Startup Pitch Night', 'Pitch', DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'Marseille Innovation Center', 'Soirée de présentation de startups', 150, 0);

-- Vérifier tous les événements dans 2 jours
SELECT
    titre,
    date_evenement,
    lieu,
    DATEDIFF(date_evenement, CURDATE()) AS jours_restants
FROM evenement
WHERE date_evenement = DATE_ADD(CURDATE(), INTERVAL 2 DAY)
  AND archived = 0
ORDER BY titre;

-- =========================================
-- NETTOYAGE (après les tests)
-- =========================================
-- Décommenter les lignes suivantes pour supprimer les événements de test:

-- DELETE FROM evenement WHERE titre = 'Test Notification - Rappel 2 jours';
-- DELETE FROM evenement WHERE titre = 'Workshop IA & Innovation';
-- DELETE FROM evenement WHERE titre = 'Startup Pitch Night';

