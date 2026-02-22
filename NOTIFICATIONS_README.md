# 🔔 GUIDE RAPIDE - NOTIFICATIONS D'ÉVÉNEMENTS

## 🚀 Lancement Rapide

### 1️⃣ Ajouter un événement de test (dans 2 jours)

Ouvre MySQL Workbench ou phpMyAdmin et exécute :

```sql
USE boostup;

INSERT INTO evenement (titre, type, date_evenement, lieu, description, capacite_max, archived)
VALUES (
    'Test Notification',
    'Conférence',
    DATE_ADD(CURDATE(), INTERVAL 2 DAY),
    'Paris, Station F',
    'Test de notification',
    100,
    0
);
```

**OU** exécute le script fourni :
```bash
mysql -u root -p boostup < src/main/resources/scripts/test_notifications.sql
```

### 2️⃣ Lancer l'application

```bash
mvn clean javafx:run
```

**OU** depuis IntelliJ :
```
Clic droit sur MainApp.java → Run 'MainApp.main()'
```

### 3️⃣ Attendre la notification

Environ **1 seconde** après l'affichage de la page login, tu devrais voir :

**Sur Windows 10/11** :  
Une notification apparaît en bas à droite de ton écran dans le Centre de notifications Windows ! 🎉

**Sur Linux/Mac** :  
Une belle popup JavaFX moderne apparaît en bas à droite avec animations ! ✨

---

## 📋 Ce qui s'affiche

```
╔═══════════════════════════════════════╗
║  🎉 Test Notification             ✕  ║
║                                       ║
║  📅 Date: 23/02/2026                  ║
║  📍 Lieu: Paris, Station F            ║
║  ⏳ Rappel: Cet événement aura        ║
║     lieu dans 2 jours.                ║
╚═══════════════════════════════════════╝
```

---

## ⚙️ Comment ça marche ?

1. **Au démarrage** de l'application (`MainApp.java`)
2. **Après 800ms**, un thread se lance
3. Il vérifie tous les événements **non archivés**
4. Il trouve ceux dont la date = **aujourd'hui + 2 jours**
5. Il affiche une **notification système** pour chacun

---

## 🧪 Tester avec plusieurs événements

```sql
INSERT INTO evenement (titre, type, date_evenement, lieu, description, capacite_max, archived)
VALUES 
    ('Workshop IA', 'Atelier', DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'Lyon', 'IA pratique', 50, 0),
    ('Pitch Night', 'Pitch', DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'Marseille', 'Présentation startups', 150, 0),
    ('Hackathon', 'Hackathon', DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'Paris', '48h de code', 200, 0);
```

Relance l'app → **3 notifications** apparaissent ! 🎊

---

## 🎨 Apparence

### Windows (Toast Natif)
- Apparaît dans le **Centre de notifications** Windows
- Son de notification par défaut
- Persiste dans l'historique des notifications

### JavaFX (Popup Custom)
- **Gradient** bleu nuit (#1b2a4a) → violet (#2d1b4e)
- **Bordure** rouge vif (#e63956)
- **Animation** slide-in depuis le bas
- **Auto-close** après 5 secondes avec fade-out
- **Bouton fermer** (✕) pour fermer manuellement

---

## 📁 Fichiers Concernés

| Fichier | Description |
|---------|-------------|
| `main/MainApp.java` | Lance le système de notifications au démarrage |
| `utils/WindowsToastHelper.java` | Gère les notifications Windows natives (PowerShell) |
| `utils/EventReminderToast.java` | Gère les notifications JavaFX stylées (fallback) |
| `services/EvenementService.java` | Méthode `getEventsInExactlyTwoDays()` |
| `scripts/test_notifications.sql` | Script SQL pour tester rapidement |

---

## 🐛 Dépannage

### ❌ Aucune notification n'apparaît

**Vérifications** :

1. Y a-t-il un événement avec `date_evenement` = aujourd'hui + 2 jours ?
```sql
SELECT * FROM evenement 
WHERE date_evenement = DATE_ADD(CURDATE(), INTERVAL 2 DAY) 
  AND archived = 0;
```

2. Vérifie la console (IntelliJ ou terminal) pour les erreurs

3. Sur Windows, vérifie que les notifications sont activées :
   - `Paramètres` → `Système` → `Notifications` → Activé

### ❌ Erreur PowerShell (Windows)

Si PowerShell est bloqué par la politique d'exécution :
- Le système bascule **automatiquement** en mode JavaFX
- Tu verras quand même une notification (mais JavaFX au lieu de native)

### ⚠️ Les notifications se chevauchent

C'est normal si tu as plusieurs événements ! Elles s'affichent avec un petit **espacement automatique**.

---

## 🎯 Modifier le comportement

### Changer le nombre de jours avant l'événement

Dans `EvenementService.java`, ligne 180 :
```java
LocalDate inTwoDays = LocalDate.now().plusDays(2); // Changer 2 en X
```

### Changer le délai au démarrage

Dans `MainApp.java`, ligne 56 :
```java
PauseTransition delay = new PauseTransition(Duration.millis(800)); // Changer 800ms
```

### Changer la durée d'affichage

Dans `EventReminderToast.java`, ligne 139 :
```java
new javafx.animation.PauseTransition(Duration.seconds(5)); // Changer 5 secondes
```

---

## ✨ Résumé

✅ Notifications **système Windows natives** (toast)  
✅ Fallback **JavaFX moderne** pour Linux/Mac  
✅ Détection **automatique** au démarrage  
✅ Événements dans **exactement 2 jours**  
✅ Design **professionnel** et **moderne**  
✅ Animations **fluides** (slide-in, fade-out)  
✅ **Auto-fermeture** après 5 secondes  
✅ Bouton **fermer** (✕)  

---

**Profite bien de ton système de notifications ! 🎉**

Bisous mon pote ! 💜

