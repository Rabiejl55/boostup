# ✅ INSTALLATION ET VÉRIFICATION FINALE

## 🔍 Vérification des Fichiers Créés

### ✅ Fichier 1: Main.java
**Chemin:** `src/main/java/main/Main.java`
**Status:** ✅ Modifié et prêt
**Vérification:**
```java
// Doit contenir:
public class Main extends Application {
    public void start(Stage primaryStage) throws Exception {
        FXMLLoader loader = new FXMLLoader(getClass().getResource("/EvenementView.fxml"));
        // ...
    }
}
```

---

### ✅ Fichier 2: EvenementView.fxml
**Chemin:** `src/main/resources/EvenementView.fxml`
**Status:** ✅ Créé et complet
**Vérification:**
```xml
<!-- Doit contenir ces fx:id -->
<TextField fx:id="tfTitre" ... />
<TextField fx:id="tfType" ... />
<DatePicker fx:id="dpDate" ... />
<TextField fx:id="tfLieu" ... />
<TextArea fx:id="taDescription" ... />
<TextField fx:id="tfCapacite" ... />
<TextField fx:id="tfRecherche" ... />
<TableView fx:id="tableEvenements" ... />
<Button fx:id="btnAjouter" ... />
<Button fx:id="btnModifier" ... />
<Button fx:id="btnSupprimer" ... />
<Button fx:id="btnActualiser" ... />
<Label fx:id="statusLabel" ... />
```

---

### ✅ Fichier 3: Documentation
**Fichiers créés:**
- ✅ README_APPLICATION.md
- ✅ CONFIGURATION_CHECKLIST.md
- ✅ QUICKSTART.md
- ✅ RESUME_FINAL.md
- ✅ APERCU_VISUEL.md
- ✅ INDEX.md
- ✅ INSTALLATION.md (ce fichier)

---

## 🔧 Configuration Système Requise

### Java
```bash
# Vérifier version
java -version

# Sortie attendue:
java version "17.x.x" ou plus récent
```

### Maven
```bash
# Vérifier version
mvn -v

# Sortie attendue:
Apache Maven 3.6.x ou plus récent
```

### MySQL
```bash
# Vérifier que MySQL est lancé
mysql -u root -p

# Créer la base si elle n'existe pas:
CREATE DATABASE boostup;

# Utiliser la base:
USE boostup;

# Vérifier la table:
SHOW TABLES;
DESC evenement;
```

---

## 📝 Structure de la Table MySQL

### Table: evenement
```sql
CREATE TABLE evenement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    dateEvenement DATE NOT NULL,
    lieu VARCHAR(255) NOT NULL,
    description TEXT,
    capaciteMax INT NOT NULL
);
```

### Vérifier la table:
```sql
DESCRIBE evenement;

-- Sortie attendue:
-- Field              | Type         | Null | Key
-- id                 | int          | NO   | PRI
-- titre              | varchar(255) | NO   |
-- type               | varchar(100) | NO   |
-- dateEvenement      | date         | NO   |
-- lieu               | varchar(255) | NO   |
-- description        | text         | YES  |
-- capaciteMax        | int          | NO   |
```

---

## 🚀 Étapes d'Installation Complète

### 1️⃣ Préparation du Système

```bash
# A. Vérifier Java 17+
java -version

# B. Vérifier Maven
mvn -v

# C. Vérifier MySQL (lancer le service)
# Windows: Services > MySQL80 > Start
# Linux: sudo systemctl start mysql
# Mac: brew services start mysql
```

### 2️⃣ Configuration de la Base de Données

```bash
# Connecter à MySQL
mysql -u root -p

# Créer la base de données
CREATE DATABASE IF NOT EXISTS boostup;

# Utiliser la base
USE boostup;

# Créer la table
CREATE TABLE IF NOT EXISTS evenement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    dateEvenement DATE NOT NULL,
    lieu VARCHAR(255) NOT NULL,
    description TEXT,
    capaciteMax INT NOT NULL
);

# Vérifier
SELECT COUNT(*) FROM evenement;

# Quitter
EXIT;
```

### 3️⃣ Configuration du Projet Maven

```bash
# Naviguer au dossier du projet
cd C:\Users\Pc\IdeaProjects\boostup

# Nettoyer le projet
mvn clean

# Télécharger les dépendances
mvn dependency:resolve

# Compiler
mvn compile
```

### 4️⃣ Vérification des Fichiers

```bash
# Vérifier que tous les fichiers existent:
# Main.java
ls src/main/java/main/Main.java

# EvenementView.fxml
ls src/main/resources/EvenementView.fxml

# EvenementController.java
ls src/main/java/controllers/EvenementController.java
```

### 5️⃣ Lancement de l'Application

**Méthode 1: IntelliJ IDEA (Recommandée)**
```
1. Ouvrir le projet dans IntelliJ
2. Attendre la synchronisation Maven
3. Clic droit sur Main.java
4. Run 'Main.main()'
```

**Méthode 2: Terminal/Powershell**
```bash
cd C:\Users\Pc\IdeaProjects\boostup
mvn clean compile javafx:run
```

**Méthode 3: Script Batch**
```bash
Double-cliquer: lancer_app.bat
```

---

## ✅ Checklist de Vérification

### Avant le Lancement:

- [ ] Java 17+ installé
- [ ] Maven 3.6+ installé
- [ ] MySQL lancé et accessible
- [ ] Base de données `boostup` créée
- [ ] Table `evenement` créée avec colonnes correctes
- [ ] Fichier `Main.java` modifié
- [ ] Fichier `EvenementView.fxml` créé dans `src/main/resources/`
- [ ] Fichier `EvenementController.java` existe dans `src/main/java/controllers/`
- [ ] Fichier `pom.xml` avec dépendances JavaFX
- [ ] Pas d'erreurs de compilation (`mvn compile`)

### Lors du Lancement:

- [ ] Fenêtre s'ouvre avec titre "Gestion des Événements - BoostUp"
- [ ] Interface affiche formulaire et tableau
- [ ] Tableau affiche les événements existants
- [ ] Boutons sont actifs/désactifs selon contexte
- [ ] Pas d'erreur dans la console

### Après le Lancement (Tests):

- [ ] Ajouter un événement fonctionne
- [ ] Événement apparaît dans le tableau
- [ ] Sélectionner un événement remplit le formulaire
- [ ] Modifier un événement fonctionne
- [ ] Supprimer un événement fonctionne (avec confirmation)
- [ ] Recherche filtre le tableau
- [ ] Bouton Actualiser recharge la liste
- [ ] Les données persistent après fermeture

---

## 🐛 Dépannage d'Installation

### ❌ "Cannot load resource /EvenementView.fxml"

**Cause:** Fichier FXML au mauvais endroit  
**Solution:**
```
1. Vérifier que le fichier est dans:
   src/main/resources/EvenementView.fxml
   
2. Pas de sous-dossier!
   ✗ src/main/resources/views/EvenementView.fxml
   ✅ src/main/resources/EvenementView.fxml
```

### ❌ "NullPointerException: tfRecherche is null"

**Cause:** fx:id du FXML ne correspond pas au contrôleur  
**Solution:**
```
1. Vérifier le fichier FXML:
   <TextField fx:id="tfRecherche" ... />
   
2. Vérifier le contrôleur:
   @FXML private TextField tfRecherche;
   
3. Si encore problème: régénérer le FXML depuis le fichier fourni
```

### ❌ "Exception in thread 'main' java.sql.SQLException"

**Cause:** MySQL non lancé ou pas accessible  
**Solution:**
```
1. Lancer MySQL:
   - Windows: Services > MySQL > Start
   - Linux: sudo systemctl start mysql
   
2. Vérifier la connexion:
   mysql -u root -p
   
3. Vérifier les paramètres dans MyDatabase.java
```

### ❌ "Connection refused"

**Cause:** MySQL n'écoute pas sur le port 3306  
**Solution:**
```
1. Vérifier le port MySQL:
   mysql -u root -p -h 127.0.0.1 -P 3306
   
2. Vérifier MyDatabase.java pour les paramètres
```

### ❌ "No column 'dateEvenement' in 'boostup'.'evenement'"

**Cause:** Colonne mal nommée dans la table  
**Solution:**
```sql
-- Créer la table correctement:
DROP TABLE IF EXISTS evenement;
CREATE TABLE evenement (
    id INT AUTO_INCREMENT PRIMARY KEY,
    titre VARCHAR(255) NOT NULL,
    type VARCHAR(100) NOT NULL,
    dateEvenement DATE NOT NULL,
    lieu VARCHAR(255) NOT NULL,
    description TEXT,
    capaciteMax INT NOT NULL
);
```

### ❌ Compilation échoue: "Cannot find symbol"

**Cause:** Dépendances Maven non téléchargées  
**Solution:**
```bash
# Forcer le téléchargement
mvn dependency:resolve

# Puis recompiler
mvn clean compile
```

---

## 🔧 Configuration Maven (pom.xml)

### Dépendances Requises:
```xml
<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-controls</artifactId>
    <version>17.0.10</version>
</dependency>

<dependency>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-fxml</artifactId>
    <version>17.0.10</version>
</dependency>

<dependency>
    <groupId>com.mysql</groupId>
    <artifactId>mysql-connector-j</artifactId>
    <version>8.0.32</version>
</dependency>
```

### Plugin Requis:
```xml
<plugin>
    <groupId>org.openjfx</groupId>
    <artifactId>javafx-maven-plugin</artifactId>
    <version>0.0.8</version>
    <configuration>
        <mainClass>main.Main</mainClass>
    </configuration>
</plugin>
```

---

## 🎯 Test Fonctionnel Complet

### Test 1: Ajouter un Événement
```
1. Lancer l'application
2. Remplir le formulaire:
   - Titre: "Test PIDEV"
   - Type: "Workshop"
   - Date: Aujourd'hui + 7 jours
   - Lieu: "ESPRIT"
   - Description: "Test"
   - Capacité: "50"
3. Cliquer "➕ Ajouter Événement"
4. Vérifier: Message de succès + événement dans tableau
5. Vérifier BD: SELECT COUNT(*) FROM evenement;
```

### Test 2: Modifier un Événement
```
1. Cliquer sur l'événement dans le tableau
2. Modifier le titre: "Test PIDEV - Modifié"
3. Cliquer "✏️ Modifier"
4. Vérifier: Tableau mis à jour
5. Vérifier BD: SELECT titre FROM evenement WHERE id = X;
```

### Test 3: Supprimer un Événement
```
1. Cliquer sur l'événement
2. Cliquer "🗑️ Supprimer"
3. Confirmer: OK dans la fenêtre
4. Vérifier: Événement disparu du tableau
5. Vérifier BD: SELECT COUNT(*) FROM evenement;
```

### Test 4: Recherche
```
1. Dans la barre "🔍 Rechercher"
2. Taper: "PIDEV"
3. Vérifier: Tableau filtre (affiche que les PIDEV)
4. Vider la recherche
5. Vérifier: Tous les événements réapparaissent
```

---

## 📊 Commandes Utiles

### Maven:
```bash
# Nettoyer
mvn clean

# Compiler
mvn compile

# Compiler + Exécuter
mvn clean compile javafx:run

# Forcer mise à jour
mvn clean install -U
```

### MySQL:
```bash
# Connexion
mysql -u root -p boostup

# Voir les événements
SELECT * FROM evenement;

# Compter les événements
SELECT COUNT(*) FROM evenement;

# Vider la table (si besoin)
DELETE FROM evenement;
```

### Git (si versionnage):
```bash
# Ajouter tous les fichiers
git add .

# Commit
git commit -m "Ajout interface JavaFX interactive"

# Push
git push origin main
```

---

## 📞 Support et Ressources

### Documentation Interne:
- QUICKSTART.md - Démarrage rapide
- README_APPLICATION.md - Guide complet
- CONFIGURATION_CHECKLIST.md - Vérifications
- RESUME_FINAL.md - Résumé technique

### Ressources Externes:
- JavaFX: https://openjfx.io/
- Maven: https://maven.apache.org/
- MySQL: https://dev.mysql.com/doc/
- Java JDBC: https://docs.oracle.com/en/java/javase/17/

---

## ✨ Après l'Installation

Félicitations! Votre application est installée et fonctionnelle! 🎉

Prochaines étapes:
1. ✅ Tester complètement avec les tests fonctionnels
2. ✅ Consulter la documentation pour amélioration
3. ✅ Envisager les évolutions futures (voir RESUME_FINAL.md)
4. ✅ Partager votre application!

---

**Installation complète et prête à l'emploi! 🚀**

Pour toute question, consultez la documentation fournie.  
Bon développement! 💻

