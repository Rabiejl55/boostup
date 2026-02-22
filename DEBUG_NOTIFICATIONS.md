# 🔧 GUIDE DE DÉBOGAGE - NOTIFICATIONS BOOSTUP

## ✅ Notification Windows Test : SUCCÈS !

Le script de test PowerShell a fonctionné ! Cela signifie que :
- ✅ Windows peut afficher des notifications
- ✅ PowerShell fonctionne correctement
- ✅ Le système de notifications est opérationnel

---

## 🔍 ÉTAPES DE DÉBOGAGE

### Étape 1 : Vérifier la base de données

Exécute ce script SQL pour vérifier tes événements :

```bash
mysql -u root -p boostup < debug_notifications.sql
```

**OU** dans MySQL Workbench / phpMyAdmin :

```sql
USE boostup;

-- Vérifier les événements le 23 février 2026
SELECT 
    id_evenement,
    titre,
    date_evenement,
    archived,
    DATEDIFF(date_evenement, CURDATE()) AS jours_restants
FROM evenement
WHERE date_evenement = '2026-02-23' AND archived = 0;
```

**Résultat attendu** : Tu dois voir tes événements avec `jours_restants = 2`

---

### Étape 2 : Lancer l'application avec logs

```bash
cd C:\Users\Pc\IdeaProjects\boostup
mvn clean javafx:run
```

**OU** dans IntelliJ :
```
Clic droit sur MainApp.java → Run 'MainApp.main()'
```

**Regarde la console !** Tu verras maintenant :

```
🔔 === VÉRIFICATION DES NOTIFICATIONS ===
📅 Nombre d'événements dans 2 jours : X
✅ Événements trouvés :
  - Titre événement 1 le 2026-02-23
  - Titre événement 2 le 2026-02-23
🪟 Affichage des notifications Windows natives...
✅ Notifications envoyées avec succès !
```

---

### Étape 3 : Analyser les logs

#### ✅ Si tu vois "Nombre d'événements dans 2 jours : 0"

**Problème** : Aucun événement trouvé dans la base de données

**Solution** :
```sql
USE boostup;

-- Ajouter un événement test le 23 février
INSERT INTO evenement (titre, type, date_evenement, lieu, description, capacite_max, archived)
VALUES (
    'TEST NOTIF 23 FEV',
    'Test',
    '2026-02-23',
    'Paris',
    'Test notification',
    100,
    0
);
```

Relance l'app !

---

#### ✅ Si tu vois "Nombre d'événements dans 2 jours : X" (X > 0)

**Mais aucune notification n'apparaît** → Le problème est dans l'affichage

**Solution 1** : Vérifie les notifications Windows
- `Paramètres Windows` → `Système` → `Notifications`
- Assure-toi qu'elles sont **activées**

**Solution 2** : Teste manuellement PowerShell
```bash
powershell -ExecutionPolicy Bypass -File test_toast_windows.ps1
```

Une notification doit apparaître !

---

#### ❌ Si tu vois une erreur dans la console

**Erreur SQL** :
```
❌ Erreur SQL dans getEventsInExactlyTwoDays : ...
```
→ Problème de connexion à la base de données
→ Vérifie que MySQL est lancé et que la base `boostup` existe

**Erreur PowerShell** :
```
❌ ERREUR lors de la vérification des notifications : ...
```
→ Copie l'erreur et envoie-la moi

---

## 🧪 TESTS RAPIDES

### Test 1 : Notification Windows directe
```bash
powershell -ExecutionPolicy Bypass -File test_toast_windows.ps1
```
**Résultat attendu** : Une notification apparaît en bas à droite

---

### Test 2 : Vérifier les événements en base
```sql
SELECT COUNT(*) FROM evenement WHERE date_evenement = '2026-02-23' AND archived = 0;
```
**Résultat attendu** : Nombre > 0

---

### Test 3 : Forcer les notifications (tous les événements)

**Modifie temporairement** `MainApp.java` ligne 69 :

**AVANT** :
```java
List<Evenement> inTwoDays = evenementService.getEventsInExactlyTwoDays();
```

**APRÈS** :
```java
List<Evenement> inTwoDays = evenementService.read(); // TOUS les événements
```

Relance l'app → Tu devrais voir des notifications pour TOUS les événements !

**⚠️ N'oublie pas de remettre le code d'origine après !**

---

## 🐛 PROBLÈMES COURANTS

### 1. "Aucun événement trouvé dans exactement 2 jours"

**Cause** : La date en base n'est pas exactement `2026-02-23`

**Vérification** :
```sql
SELECT DISTINCT date_evenement FROM evenement ORDER BY date_evenement;
```

**Solution** : Assure-toi que la date est **exactement** `2026-02-23` (format SQL : `YYYY-MM-DD`)

---

### 2. "Notifications envoyées avec succès" mais rien n'apparaît

**Cause** : Notifications Windows désactivées

**Solution** :
1. Ouvre `Paramètres Windows`
2. Va dans `Système` → `Notifications`
3. Active les notifications
4. Relance l'app

---

### 3. Erreur PowerShell

**Cause** : Politique d'exécution stricte

**Solution** : Le système bascule automatiquement en mode JavaFX (popup dans l'app)

---

## 📊 LOGS DE DEBUG

Maintenant, quand tu lances l'app, tu verras dans la console :

```
📊 DEBUG getEventsInExactlyTwoDays:
  Aujourd'hui : 2026-02-21
  Dans 2 jours : 2026-02-23
  Nombre total d'événements actifs : X
  - Événement 1 : 2026-02-23 (égal ? true)
    ✅ AJOUTÉ aux notifications !
  - Événement 2 : 2026-02-25 (égal ? false)
  Total événements dans 2 jours : 1

🔔 === VÉRIFICATION DES NOTIFICATIONS ===
📅 Nombre d'événements dans 2 jours : 1
✅ Événements trouvés :
  - Événement 1 le 2026-02-23
🪟 Affichage des notifications Windows natives...
✅ Notifications envoyées avec succès !
```

---

## ✨ CHECKLIST COMPLÈTE

- [ ] MySQL est lancé
- [ ] La base `boostup` existe
- [ ] Il y a au moins 1 événement avec `date_evenement = '2026-02-23'`
- [ ] L'événement a `archived = 0` (non archivé)
- [ ] Les notifications Windows sont activées
- [ ] L'app est lancée depuis `MainApp.java` (pas `Main.java`)
- [ ] La console affiche les logs de débogage

---

## 🚀 COMMANDES UTILES

### Lancer l'app avec logs visibles
```bash
mvn clean javafx:run
```

### Vérifier les événements
```bash
mysql -u root -p boostup < debug_notifications.sql
```

### Tester notification Windows
```bash
powershell -ExecutionPolicy Bypass -File test_toast_windows.ps1
```

### Ajouter un événement test
```sql
USE boostup;
INSERT INTO evenement (titre, type, date_evenement, lieu, description, capacite_max, archived)
VALUES ('TEST 23 FEV', 'Test', '2026-02-23', 'Paris', 'Test', 100, 0);
```

---

## 📞 SI ÇA NE FONCTIONNE TOUJOURS PAS

**Copie et envoie-moi** :

1. Les logs de la console (tout ce qui s'affiche quand tu lances l'app)
2. Le résultat de cette requête SQL :
```sql
SELECT * FROM evenement WHERE date_evenement = '2026-02-23';
```
3. Le résultat de :
```bash
powershell -ExecutionPolicy Bypass -File test_toast_windows.ps1
```

---

**Bon courage mon pote ! 💪**

Les notifications **FONCTIONNENT** (le test PowerShell l'a prouvé), donc le problème est probablement :
- Pas d'événements dans la base avec la bonne date
- OU événements archivés (`archived = 1`)
- OU notifications Windows désactivées

Lance l'app maintenant et **regarde la console** pour voir les logs ! 🔍

