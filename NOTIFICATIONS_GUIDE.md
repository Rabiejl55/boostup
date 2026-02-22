# 🔔 SYSTÈME DE NOTIFICATIONS - BOOSTUP

## ✅ Fonctionnalités Implémentées

### 🎯 Notification Windows Native
- **Fichier**: `utils/WindowsToastHelper.java`
- **Type**: Toast Windows 10/11 (Centre de notifications)
- **Déclenchement**: Automatique au démarrage de l'application
- **Condition**: Événements dont la date est exactement dans 2 jours

### 🎨 Notification JavaFX Stylée (Fallback)
- **Fichier**: `utils/EventReminderToast.java`
- **Type**: Popup JavaFX personnalisée
- **Style**: Moderne, gradient bleu/violet, animations fluides
- **Animation**: Slide-in depuis le bas à droite
- **Auto-close**: Disparaît après 5 secondes avec fade-out

## 📋 Contenu des Notifications

Chaque notification affiche :
- 🎉 **Titre** de l'événement
- 📅 **Date** exacte (format JJ/MM/AAAA)
- 📍 **Lieu** de l'événement
- ⏳ **Message**: "Rappel : Cet événement aura lieu dans 2 jours."

## 🚀 Comment ça fonctionne ?

### 1. Au démarrage de l'application (`MainApp.java`)
```java
// Attendre 800ms après l'affichage de la fenêtre de login
PauseTransition delay = new PauseTransition(Duration.millis(800));
delay.setOnFinished(e -> {
    // Lancer un thread pour vérifier les événements
    Thread worker = new Thread(() -> {
        EvenementService evenementService = new EvenementService();
        List<Evenement> inTwoDays = evenementService.getEventsInExactlyTwoDays();
        
        if (!inTwoDays.isEmpty()) {
            if (WindowsToastHelper.isWindows()) {
                // Notifications Windows natives
                WindowsToastHelper.showEventReminders(inTwoDays);
            } else {
                // Notifications JavaFX pour Linux/Mac
                for (int i = 0; i < inTwoDays.size(); i++) {
                    new EventReminderToast(inTwoDays.get(i), i);
                }
            }
        }
    });
    worker.start();
});
delay.play();
```

### 2. Récupération des événements (`EvenementService.java`)
```java
public List<Evenement> getEventsInExactlyTwoDays() {
    LocalDate inTwoDays = LocalDate.now().plusDays(2);
    List<Evenement> result = new ArrayList<>();
    List<Evenement> all = readActifs(); // Événements non archivés
    
    for (Evenement e : all) {
        if (e.getDateEvenement().toLocalDate().equals(inTwoDays)) {
            result.add(e);
        }
    }
    return result;
}
```

### 3. Affichage Windows (`WindowsToastHelper.java`)
- Crée un script PowerShell temporaire
- Utilise l'API Windows.UI.Notifications
- Affiche une notification native dans le Centre de notifications Windows
- Son par défaut de Windows

### 4. Affichage JavaFX (`EventReminderToast.java`)
- Crée une fenêtre transparente (Stage)
- Positionne en bas à droite de l'écran
- Animation slide-in depuis le bas
- Bouton fermer (✕)
- Auto-fermeture après 5 secondes

## 🧪 Comment Tester ?

### Option 1: Tester avec des vraies données
1. Dans la base de données `boostup`, ajouter un événement avec une date dans exactement 2 jours :
```sql
INSERT INTO evenement (titre, type, date_evenement, lieu, description, capacite_max, archived)
VALUES ('Test Notification', 'Conférence', DATE_ADD(CURDATE(), INTERVAL 2 DAY), 'Paris', 'Test', 100, 0);
```

2. Lancer l'application :
```bash
mvn clean javafx:run
```

3. Attendre 1 seconde après l'affichage de la page login → la notification devrait apparaître !

### Option 2: Tester manuellement (code temporaire)
Dans `MainApp.java`, remplacer temporairement la ligne :
```java
List<Evenement> inTwoDays = evenementService.getEventsInExactlyTwoDays();
```
par :
```java
List<Evenement> inTwoDays = evenementService.read(); // Tous les événements
```

Cela affichera une notification pour TOUS les événements (pour tester).

## 📸 Apparence des Notifications

### Windows 10/11 (Toast Natif)
```
┌─────────────────────────────────────┐
│  🎉 Startup Pitch Night            │
│                                     │
│  📅 Date: 16/02/2026                │
│  📍 Lieu: Paris, Station F          │
│  ⏳ Rappel: Cet événement aura      │
│     lieu dans 2 jours.              │
└─────────────────────────────────────┘
```

### JavaFX (Popup Custom)
```
╔═══════════════════════════════════════╗
║  🎉 Startup Pitch Night           ✕  ║
║                                       ║
║  📅 Date: 16/02/2026                  ║
║  📍 Lieu: Paris, Station F            ║
║  ⏳ Rappel: Cet événement aura        ║
║     lieu dans 2 jours.                ║
╚═══════════════════════════════════════╝
```
- Gradient bleu nuit (#1b2a4a) → violet (#2d1b4e)
- Bordure rouge (#e63956)
- Ombre portée moderne
- Animation slide-in fluide

## ⚙️ Configuration Technique

### Dépendances
Aucune dépendance externe supplémentaire requise ! Tout utilise :
- **Windows**: PowerShell (intégré à Windows)
- **JavaFX**: Déjà dans le projet (javafx-controls, javafx-graphics)

### Compatibilité
- ✅ **Windows 10/11**: Notifications natives dans le Centre de notifications
- ✅ **Windows 7/8**: Fallback vers JavaFX
- ✅ **Linux/Mac**: Notifications JavaFX personnalisées

## 🎨 Personnalisation

### Modifier le délai avant fermeture
Dans `EventReminderToast.java`, ligne 139 :
```java
javafx.animation.PauseTransition autoClose = new javafx.animation.PauseTransition(Duration.seconds(5));
```
Changer `5` en nombre de secondes souhaité.

### Modifier le délai de démarrage
Dans `MainApp.java`, ligne 56 :
```java
PauseTransition delay = new PauseTransition(Duration.millis(800));
```
Changer `800` en millisecondes souhaitées.

### Modifier la condition (pas seulement 2 jours)
Dans `EvenementService.java`, modifier :
```java
LocalDate inTwoDays = LocalDate.now().plusDays(2);
```
En :
```java
LocalDate target = LocalDate.now().plusDays(X); // X = nombre de jours
```

## 🐛 Dépannage

### La notification ne s'affiche pas
1. Vérifier qu'il existe un événement avec une date exactement dans 2 jours
2. Vérifier que l'événement n'est pas archivé (`archived = 0`)
3. Vérifier la console : des messages d'erreur y apparaissent
4. Sur Windows, vérifier que les notifications sont activées dans les Paramètres système

### Erreur PowerShell (Windows)
Si PowerShell est bloqué, la notification JavaFX s'affichera automatiquement en fallback.

### Plusieurs notifications se chevauchent
C'est normal si plusieurs événements sont dans 2 jours. Le système affiche une notification par événement avec un espacement automatique.

## ✨ Améliorations Futures (Optionnel)

- [ ] Ajouter un son personnalisé
- [ ] Permettre de snooze la notification (rappel dans X minutes)
- [ ] Ajouter un bouton "Voir l'événement" qui ouvre les détails
- [ ] Notifier aussi 1 jour avant, 1 semaine avant
- [ ] Sauvegarder les préférences de notification par utilisateur
- [ ] Badge sur l'icône de l'application (nombre d'événements à venir)

## 📝 Résumé

✅ **Notification système native Windows** : Implémentée  
✅ **Fallback JavaFX stylé** : Implémenté  
✅ **Vérification automatique au démarrage** : Implémenté  
✅ **Détection événements dans 2 jours** : Implémenté  
✅ **Affichage des informations complètes** : Implémenté  
✅ **Animations modernes** : Implémenté  
✅ **Auto-fermeture** : Implémenté  

---

**Tout est prêt ! 🎉**  
Lance l'application et ajoute un événement dans 2 jours pour tester !

