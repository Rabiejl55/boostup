# 📧 GUIDE CONFIGURATION EMAIL - BoostUp

## 🎯 Objectif
Configurer l'envoi automatique d'emails de bienvenue quand un utilisateur s'inscrit à un événement.

---

## ⚠️ ÉTAPE 1 : Créer un compte Gmail dédié (OPTIONNEL)

**Option A - Utiliser ton email perso (rayen.amri@esprit.tn)**
- ✅ Rapide
- ❌ Risque de blocage si trop d'emails

**Option B - Créer un nouveau compte Gmail**
1. Va sur https://accounts.google.com/signup
2. Crée un compte : `boostup.events.tn@gmail.com` (exemple)
3. ✅ Plus professionnel
4. ✅ Pas de risque pour ton compte perso

---

## 🔐 ÉTAPE 2 : Activer l'authentification à 2 facteurs (OBLIGATOIRE)

1. **Connecte-toi à ton compte Gmail**
2. **Va sur** : https://myaccount.google.com/security
3. **Cherche "Validation en deux étapes"**
4. **Clique sur "Activer"**
5. **Suis les instructions** (code SMS/appel)

---

## 🔑 ÉTAPE 3 : Générer un mot de passe d'application

1. **Va sur** : https://myaccount.google.com/apppasswords
2. **Connecte-toi** si demandé
3. **Dans "Sélectionner l'application"** → Choisis **"Autre (nom personnalisé)"**
4. **Tape** : `BoostUp Java App`
5. **Clique sur "Générer"**
6. ✅ **Google te donne un code à 16 caractères** (exemple: `abcd efgh ijkl mnop`)
7. **COPIE CE CODE** (tu ne le reverras plus !)

---

## 💻 ÉTAPE 4 : Configurer le code dans ton projet

### 📂 Ouvre le fichier :
```
C:\Users\Pc\IdeaProjects\boostup\src\main\java\services\EmailService.java
```

### ✏️ Modifie ces 2 lignes (lignes 22-23) :

**AVANT :**
```java
private static final String FROM_EMAIL = "boostup.events.tn@gmail.com";
private static final String FROM_PASSWORD = "VOTRE_MOT_DE_PASSE_APP";
```

**APRÈS :**
```java
private static final String FROM_EMAIL = "TON_EMAIL@gmail.com";
private static final String FROM_PASSWORD = "abcd efgh ijkl mnop"; // Le code de 16 caractères
```

**⚠️ IMPORTANT :**
- Remplace `TON_EMAIL@gmail.com` par ton vrai email
- Remplace `abcd efgh ijkl mnop` par le code Google (garde les espaces ou enlève-les)

---

## 🧪 ÉTAPE 5 : Tester l'envoi d'email

### 1️⃣ Recharge Maven
1. **Ouvre IntelliJ**
2. **Clic droit sur `pom.xml`**
3. **Maven → Reload Project**

### 2️⃣ Lance le test
1. **Ouvre le fichier** : `services/TestEmail.java`
2. **Clique sur le bouton ▶️ vert** à côté de `public static void main`
3. **Regarde la console**

### 3️⃣ Vérifie ton email
- **Ouvre** : https://mail.google.com
- **Vérifie la boîte de réception** de `rayen.amri@esprit.tn`
- **Si rien** → Vérifie le dossier **SPAM**

---

## ✅ ÉTAPE 6 : Test dans l'application réelle

1. **Lance MainApp**
2. **Connecte-toi** avec `admin / user`
3. **Va sur un événement** dans le front
4. **Clique sur "S'inscrire"**
5. **Remplis le formulaire**
6. **Valide**

➡️ **Tu dois recevoir 2 choses :**
- Une alerte : "🎉 Félicitations !"
- Un popup : "Email envoyé ✅"
- Un email dans `rayen.amri@esprit.tn`

---

## ❓ PROBLÈMES FRÉQUENTS

### ❌ "Authentication failed"
→ Vérifie que tu as activé la 2FA
→ Vérifie que le mot de passe d'application est correct

### ❌ "Connection timed out"
→ Vérifie ta connexion Internet
→ Désactive temporairement ton antivirus/firewall

### ❌ "Email not sent"
→ Vérifie que `FROM_EMAIL` et `FROM_PASSWORD` sont corrects dans EmailService.java

### ❌ Je ne reçois pas l'email
→ Vérifie le dossier SPAM
→ Attends 1-2 minutes (parfois retardé)
→ Vérifie que l'email destinataire est correct

---

## 🎉 RÉSULTAT FINAL

Quand tout marche, tu auras :
- ✅ Email HTML magnifique aux couleurs BoostUp
- ✅ Envoi automatique à chaque inscription
- ✅ Détails complets de l'événement
- ✅ Message professionnel

---

## 💡 PROCHAINES ÉTAPES (optionnel)

Tu pourras ensuite ajouter :
- 📧 Email de rappel (2 jours avant l'événement)
- 📧 Email d'annulation (si événement supprimé)
- 📧 Email de confirmation avec QR code
- 📧 Newsletter pour tous les événements

---

**Besoin d'aide ? Appelle-moi dans le chat ! 🚀**

