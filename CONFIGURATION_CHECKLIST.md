# Configuration de l'Application Interactive - BoostUp

## ✅ Vérification des Fichiers

### Fichiers Créés:
- ✅ `src/main/resources/EvenementView.fxml` - Interface FXML
- ✅ `README_APPLICATION.md` - Guide d'utilisation
- ✅ `lancer_app.bat` - Script de lancement Windows

### Fichiers Modifiés:
- ✅ `src/main/java/main/Main.java` - Application JavaFX

### Fichiers Existants Utilisés:
- ✅ `src/main/java/controllers/EvenementController.java`
- ✅ `src/main/java/entities/GEvenement/Evenement.java`
- ✅ `src/main/java/entities/GEvenement/EvenementFX.java`
- ✅ `src/main/java/services/EvenementService/EvenementService.java`
- ✅ `pom.xml` (avec dépendances JavaFX)

---

## 🔧 Configuration Requise

### Java:
- Version: **17 ou supérieure**
- Vérifier: `java -version`

### Maven:
- Version: **3.6 ou supérieure**
- Dépendances JavaFX: **17.0.10** (déjà configurées dans pom.xml)

### Base de Données:
- SGBD: **MySQL 8.0+**
- Base: **boostup**
- Table: **evenement** (avec colonnes: id, titre, type, dateEvenement, lieu, description, capaciteMax)

---

## 📝 Structure FXML Vérifié

### Contrôleur: `controllers.EvenementController`

### Champs FXML Requis (tous présents dans EvenementView.fxml):

#### Formulaire:
- ✅ `tfTitre` (TextField) - Titre de l'événement
- ✅ `tfType` (TextField) - Type d'événement
- ✅ `dpDate` (DatePicker) - Date de l'événement
- ✅ `tfLieu` (TextField) - Lieu de l'événement
- ✅ `taDescription` (TextArea) - Description
- ✅ `tfCapacite` (TextField) - Capacité maximale
- ✅ `tfRecherche` (TextField) - Barre de recherche

#### Table:
- ✅ `tableEvenements` (TableView) - Tableau principal
- ✅ `colId` (TableColumn) - Colonne ID
- ✅ `colTitre` (TableColumn) - Colonne Titre
- ✅ `colType` (TableColumn) - Colonne Type
- ✅ `colDate` (TableColumn) - Colonne Date
- ✅ `colLieu` (TableColumn) - Colonne Lieu
- ✅ `colCapacite` (TableColumn) - Colonne Capacité

#### Boutons:
- ✅ `btnAjouter` (Button) - Ajouter événement
- ✅ `btnModifier` (Button) - Modifier événement
- ✅ `btnSupprimer` (Button) - Supprimer événement
- ✅ `btnActualiser` (Button) - Actualiser liste

#### Autres:
- ✅ `statusLabel` (Label) - Affichage du statut

---

## 🎯 Points de Synchronisation BD

### EvenementService utilisée:
```
EvenementService es = new EvenementService();

Méthodes appelées:
- es.ajouter(Evenement) → INSERT
- es.read() → SELECT *
- es.update(Evenement) → UPDATE
- es.supprimer(int id) → DELETE
```

### Flux de Données:
```
Formulaire FXML
      ↓
EvenementController (valide + crée EvenementFX)
      ↓
Conversion EvenementFX → Evenement
      ↓
EvenementService (ajouter/update/supprimer/read)
      ↓
Base de Données MySQL
      ↓
Refresh de la TableView
```

---

## 🔐 Validations Implémentées

✅ **Au niveau du contrôleur:**
- Titre obligatoire
- Type obligatoire
- Date obligatoire
- Lieu obligatoire
- Capacité: nombre entier obligatoire

✅ **Confirmations:**
- Suppression: AlertDialog de confirmation
- Erreurs: AlertDialog d'erreur
- Succès: AlertDialog de succès

---

## 📦 Dépendances Maven Vérifiées

```xml
<!-- MySQL JDBC Driver -->
<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.0.32</version>
</dependency>

<!-- JavaFX Controls -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>17.0.10</version>
</dependency>

<!-- JavaFX FXML -->
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-fxml</artifactId>
    <version>17.0.10</version>
</dependency>
```

---

## 🚀 Étapes de Lancement

### Option 1: IDE IntelliJ
```
1. Fichier → Ouvrir → C:\Users\Pc\IdeaProjects\boostup
2. Clic droit sur Main.java
3. Run 'Main.main()'
```

### Option 2: PowerShell/CMD
```
cd C:\Users\Pc\IdeaProjects\boostup
mvn clean compile javafx:run
```

### Option 3: Script Batch
```
Double-cliquer sur: lancer_app.bat
```

---

## ✨ Fonctionnalités Implémentées

| Opération | Statut | Contrôleur | Service |
|-----------|--------|-----------|---------|
| Ajouter | ✅ | handleAjouter() | ajouter() |
| Lire | ✅ | refreshTable() | read() |
| Modifier | ✅ | handleModifier() | update() |
| Supprimer | ✅ | handleSupprimer() | supprimer() |
| Rechercher | ✅ | setupSearchFilter() | N/A (local) |

---

## 🎨 Thème de l'Interface

- Fonte: Segoe UI, 11px
- Couleurs des boutons:
  - Ajouter: Vert (#27ae60)
  - Modifier: Bleu (#3498db)
  - Supprimer: Rouge (#e74c3c)
  - Actualiser: Gris (#95a5a6)
- Arrière-plan: Blanc et gris clair
- Bordures: Gris (#bdc3c7)

---

## 🔍 Contrôles et Événements

### Contrôles MouseClick:
- ✅ Sélection dans la table
- ✅ Bouton Ajouter
- ✅ Bouton Modifier
- ✅ Bouton Supprimer
- ✅ Bouton Actualiser

### Contrôles Texte:
- ✅ Saisie dans les champs du formulaire
- ✅ Recherche en temps réel

### Contrôles DatePicker:
- ✅ Sélection de date

---

## 🛠️ Fichier de Configuration pom.xml

```xml
<build>
    <plugins>
        <plugin>
            <groupId>org.openjfx</groupId>
            <artifactId>javafx-maven-plugin</artifactId>
            <version>0.0.8</version>
            <configuration>
                <mainClass>main.Main</mainClass>
            </configuration>
        </plugin>
    </plugins>
</build>
```

---

## ✅ Checklist Final

- ✅ Fichier Main.java modifié
- ✅ Fichier EvenementView.fxml créé
- ✅ Contrôleur EvenementController vérifié
- ✅ Entités Evenement et EvenementFX disponibles
- ✅ Service EvenementService opérationnel
- ✅ Dépendances Maven correctes
- ✅ Scripts de lancement créés
- ✅ Documentation complète

---

## 📞 Points de Contact

Pour toute issue:

1. **Vérifier la BD MySQL**: `show tables;` dans boostup
2. **Vérifier les imports**: Tous les imports sont à jour
3. **Vérifier le FXML**: fx:id doivent matcher les @FXML
4. **Vérifier Maven**: mvn clean compile

---

**🎉 L'application est prête au lancement! 🚀**

Lancez-la avec l'une des méthodes ci-dessus.

