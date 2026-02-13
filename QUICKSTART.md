# 🚀 DÉMARRAGE RAPIDE - 5 MINUTES

## ⚡ Les 3 Étapes pour Lancer l'Application

### 1️⃣ Ouvrir le Projet
```
IntelliJ IDEA → File → Open → C:\Users\Pc\IdeaProjects\boostup
```

### 2️⃣ Attendre la Synchronisation Maven
- IntelliJ va télécharger les dépendances JavaFX automatiquement
- ⏱️ Attend 1-2 minutes

### 3️⃣ Lancer l'Application
```
Clic droit sur: src/main/java/main/Main.java
→ Run 'Main.main()'

OU

Bouton ▶️ Run (haut droit)
```

---

## 🎯 Interface qui Devrait Apparaître

```
┌─────────────────────────────────────────────────┐
│  📅 Gestion des Événements - BoostUp            │
│  🔍 Rechercher: [                      ]        │
├──────────────────┬──────────────────────────────┤
│ 📝 FORMULAIRE    │ 📊 TABLEAU DES ÉVÉNEMENTS    │
│                  │                              │
│ Titre:   [     ] │ ┌────────────────────────┐   │
│ Type:    [     ] │ │ ID│Titre│Type│Date│...│   │
│ Date:    [     ] │ │─────────────────────────│   │
│ Lieu:    [     ] │ │ 1 │PIDEV│... │2026│... │  │
│ Desc:    [     ] │ │──────────────────────────┘  │
│ Capacité:[     ] │                              │
│                  │ ✓ événement(s) chargé(s)    │
│ [➕ Ajouter]    │                              │
│ [✏️  Modifier]  │                              │
│ [🗑️  Supprimer]│                              │
│ [🔄 Actualiser]│                              │
└──────────────────┴──────────────────────────────┘
```

---

## 📝 Premier Test: Ajouter un Événement

### Remplir le Formulaire:
- **Titre**: "PIDEV 2026"
- **Type**: "Workshop"
- **Date**: Sélectionner une date
- **Lieu**: "ESPRIT Bloc M"
- **Description**: "Atelier de développement"
- **Capacité**: "50"

### Cliquer: ➕ Ajouter Événement

### ✅ Résultat Attendu:
- Message "Événement ajouté avec succès!"
- L'événement apparaît dans le tableau
- Le formulaire se vide

---

## 🔧 Si Ça Ne Marche Pas

### ❌ Erreur "Cannot load resource"
```
→ Vérifier que EvenementView.fxml existe dans:
  src/main/resources/
```

### ❌ Erreur "Database connection failed"
```
→ Vérifier que MySQL est lancé
→ Vérifier que base "boostup" existe
→ Vérifier les paramètres de connexion dans MyDatabase.java
```

### ❌ Erreur "tfRecherche is null"
```
→ Le fichier FXML a un problème
→ Régénérer depuis le fichier fourni
```

### ❌ Aucun événement ne s'affiche
```
→ La table "evenement" est peut-être vide
→ Cliquer sur "🔄 Actualiser"
→ Ou ajouter un nouvel événement
```

---

## ✨ Opérations Rapides à Tester

### 1. Ajouter (➕)
```
Remplir formulaire → Cliquer Ajouter
```

### 2. Modifier (✏️)
```
Cliquer sur un événement → Modifier champs → Cliquer Modifier
```

### 3. Supprimer (🗑️)
```
Cliquer sur un événement → Cliquer Supprimer → Confirmer
```

### 4. Rechercher (🔍)
```
Taper dans la barre → Table se filtre automatiquement
```

---

## 📊 Points Clés de Synchronisation BD

✅ **Chaque action modifie automatiquement la BD:**
- Ajouter → INSERT dans table evenement
- Modifier → UPDATE dans table evenement
- Supprimer → DELETE dans table evenement
- Actualiser → SELECT * depuis table evenement

✅ **La table se met à jour en temps réel**

---

## 🎓 Architecture Simplifiée

```
Main.java (lance)
    ↓
EvenementView.fxml (affiche interface)
    ↓
EvenementController.java (gère clics)
    ↓
EvenementService (modifie BD)
    ↓
MySQL boostup (stocke données)
```

---

## 💾 Données Persistantes

✅ **Les données ne disparaissent pas quand on ferme l'app**
- Stockées dans MySQL
- Chargées automatiquement au redémarrage

---

## 📞 En Cas de Problème

1. Fermer l'application
2. Relancer par IntelliJ
3. Si ça ne marche toujours pas:
   - Vérifier Maven Build: Build → Build Project
   - Invalider caches: File → Invalidate Caches

---

**✅ C'est prêt! Lancez l'application maintenant! 🚀**

Questions? Consultez README_APPLICATION.md pour plus de détails.

