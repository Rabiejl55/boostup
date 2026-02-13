# 🚀 DÉMARRAGE DE L'APPLICATION INTERACTIVE

## ✅ Modifications Effectuées

Votre projet **boostup** a été transformé en une application JavaFX interactive complète!

### 📂 Fichiers Créés/Modifiés:

1. ✏️ **Main.java** - Application JavaFX (modifiée)
   - Initialise l'interface graphique
   - Charge le fichier FXML
   
2. ✨ **EvenementView.fxml** - Interface graphique (créée)
   - Formulaire pour ajouter/modifier les événements
   - Tableau pour afficher tous les événements
   - Barre de recherche en temps réel

3. ✅ **EvenementController.java** - Logique métier (existant)
   - Gère les événements utilisateur
   - Synchronise avec la base de données

---

## 🎯 Fonctionnalités

### ➕ Ajouter un événement
- Remplir le formulaire à gauche
- Cliquer sur "➕ Ajouter Événement"
- L'événement s'ajoute automatiquement à la base de données

### ✏️ Modifier un événement
- Cliquer sur un événement dans la table
- Le formulaire se remplit automatiquement
- Modifier les champs
- Cliquer sur "✏️ Modifier"

### 🗑️ Supprimer un événement
- Cliquer sur un événement dans la table
- Cliquer sur "🗑️ Supprimer"
- Confirmer la suppression

### 🔍 Rechercher
- Utiliser la barre "🔍 Rechercher" en haut
- La table se filtre en temps réel

---

## 🏃 Comment Lancer l'Application

### Option 1: Avec IntelliJ IDEA (Recommandé)

```
1. Ouvrir le projet dans IntelliJ IDEA
2. Localiser Main.java dans src/main/java/main/
3. Clic droit sur Main.java
4. Sélectionner "Run 'Main.main()'"
   
   OU

5. Cliquer sur le bouton ▶️ Run en haut à droite
```

### Option 2: Depuis PowerShell/Cmd

```powershell
# Naviguer au dossier du projet
cd C:\Users\Pc\IdeaProjects\boostup

# Compiler le projet
mvn clean compile

# Lancer l'application JavaFX
mvn javafx:run
```

---

## 📋 Checklist Avant le Lancement

- ✅ MySQL est lancé et accessible
- ✅ La base de données "boostup" existe
- ✅ La table "evenement" existe dans la BD
- ✅ Les dépendances Maven sont installées (JavaFX)
- ✅ Java 17 ou supérieur est installé

---

## 🖼️ Aperçu de l'Interface

```
┌─────────────────────────────────────────────────────────┐
│  📅 Gestion des Événements - BoostUp                    │
│  🔍 Rechercher: [________________]                      │
├──────────────────────┬──────────────────────────────────┤
│                      │                                  │
│ 📝 FORMULAIRE        │   📊 LISTE DES ÉVÉNEMENTS       │
│                      │                                  │
│ Titre:     [____]    │  ┌──────────────────────────┐   │
│ Type:      [____]    │  │ ID│Titre│Type│Date│Lieu│  │ │
│ Date:      [____]    │  │──────────────────────────│   │
│ Lieu:      [____]    │  │ 1 │PIDEV│... │ 2026│...│  │ │
│ Description:[____]   │  │ 2 │Dev  │... │ 2026│...│  │ │
│ Capacité:  [____]    │  │ 3 │...  │... │ 2026│...│  │ │
│                      │  └──────────────────────────┘   │
│ [➕ Ajouter]         │                                  │
│ [✏️  Modifier]       │   ✓ 3 événement(s) chargé(s)   │
│ [🗑️  Supprimer]     │                                  │
│ [🔄 Actualiser]     │                                  │
│                      │                                  │
└──────────────────────┴──────────────────────────────────┘
│ ℹ️ Sélectionnez un événement pour modifier  © 2026     │
└──────────────────────────────────────────────────────────┘
```

---

## ✨ Avantages de l'Interface Interactive

| Aspect | Avant | Après |
|--------|--------|--------|
| **Type** | Console/Texte | GUI GraphicAL |
| **Interaction** | Statique | Interactive |
| **Données** | Affichage à la demande | Tableau en temps réel |
| **Ajout** | Code commenté | Bouton direct |
| **Recherche** | Manuelle | Barre de recherche |
| **Modification** | Boucle CRUD | Interface intuitive |
| **Suppression** | Confirmation console | Fenêtre de confirmation |

---

## 🔧 Dépannage

### ❌ "Exception in thread 'main' java.io.IOException: Cannot load resource"
→ Vérifier que `EvenementView.fxml` est dans `src/main/resources/`

### ❌ "NullPointerException: tfRecherche is null"
→ Vérifier que tous les fx:id du FXML correspondent au contrôleur

### ❌ "Cannot load FXML"
→ Vérifier le chemin relatif `/EvenementView.fxml`

### ❌ "Database connection failed"
→ Vérifier que MySQL est lancé et que les paramètres de connexion sont corrects

---

## 📞 Prochaines Étapes

Après avoir vérifié le lancement:

1. ✅ Tester l'ajout d'un nouvel événement
2. ✅ Tester la modification d'un événement
3. ✅ Tester la suppression d'un événement
4. ✅ Tester la recherche/filtrage
5. ✅ Vérifier que les données persistes en base

---

## 🎓 Ressources Utiles

- **JavaFX Documentation**: https://openjfx.io/
- **Maven FXML**: https://gluonhq.com/products/javafx/
- **MySQL Java**: https://dev.mysql.com/downloads/connector/j/

---

**✅ Tout est prêt! Lancez l'application et profitez de votre interface interactive! 🚀**

Pour le lancement, utilisez l'une des méthodes ci-dessus.

