-- =========================================
-- VÉRIFICATION RAPIDE DES ÉVÉNEMENTS
-- Date actuelle : 21 février 2026
-- Date cible : 23 février 2026 (dans 2 jours)
-- =========================================

USE boostup;

-- 1. Vérifier la date système
SELECT CURDATE() AS date_aujourdhui, DATE_ADD(CURDATE(), INTERVAL 2 DAY) AS dans_2_jours;

-- 2. Compter les événements le 23 février 2026
SELECT COUNT(*) AS nombre_evenements_23_fevrier
FROM evenement
WHERE date_evenement = '2026-02-23' AND archived = 0;

-- 3. Lister TOUS les événements le 23 février 2026
SELECT
    id_evenement,
    titre,
    type,
    date_evenement,
    lieu,
    archived,
    DATEDIFF(date_evenement, CURDATE()) AS jours_restants
FROM evenement
WHERE date_evenement = '2026-02-23'
ORDER BY titre;

-- 4. Vérifier s'il y a des événements archivés
SELECT
    COUNT(CASE WHEN archived = 1 THEN 1 END) AS nb_archives,
    COUNT(CASE WHEN archived = 0 THEN 1 END) AS nb_actifs,
    COUNT(*) AS total
FROM evenement
WHERE date_evenement = '2026-02-23';

-- 5. Afficher TOUS les événements (pour debug)
SELECT
    id_evenement,
    titre,
    date_evenement,
    archived,
    DATEDIFF(date_evenement, CURDATE()) AS jours_restants
FROM evenement
ORDER BY date_evenement;

-- =========================================
-- SI AUCUN ÉVÉNEMENT N'APPARAÎT :
-- Ajouter un événement de test le 23 février
-- =========================================

-- Décommenter les lignes suivantes pour ajouter un événement test :

/*
DELETE FROM evenement WHERE titre = 'TEST NOTIFICATION 23 FEV';

INSERT INTO evenement (titre, type, date_evenement, lieu, description, capacite_max, image, archived)
VALUES (
    'TEST NOTIFICATION 23 FEV',
    'Test',
    '2026-02-23',
    'Test Location',
    'Événement de test pour notifications',
    100,
    NULL,
    0
);

-- Vérifier l'ajout
SELECT * FROM evenement WHERE titre = 'TEST NOTIFICATION 23 FEV';
*/

