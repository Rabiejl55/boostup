@echo off
REM Script de lancement de l'application BoostUp
REM Gestion des Événements Interactive

echo ===================================
echo   🚀 BoostUp - Gestion d'Événements
echo ===================================
echo.

REM Vérifier que le répertoire du projet existe
if not exist "pom.xml" (
    echo ❌ Erreur: pom.xml non trouvé!
    echo Assurez-vous de lancer ce script depuis la racine du projet.
    pause
    exit /b 1
)

echo ✅ Projet détecté
echo.

REM Compilation du projet
echo 📦 Compilation du projet...
echo.

REM Vérifier si Maven est installé globalement
where mvn >nul 2>nul
if %errorlevel% neq 0 (
    echo ⚠️  Maven n'a pas été trouvé dans le PATH
    echo Tentative d'utilisation de Maven Wrapper...

    if exist "mvnw.cmd" (
        mvnw.cmd clean compile
    ) else (
        echo ❌ Erreur: Maven n'est pas disponible
        echo Veuillez installer Maven ou utiliser un IDE comme IntelliJ IDEA
        pause
        exit /b 1
    )
) else (
    mvn clean compile
)

if %errorlevel% neq 0 (
    echo ❌ Compilation échouée!
    pause
    exit /b 1
)

echo.
echo ✅ Compilation réussie!
echo.

REM Lancer l'application
echo 🎯 Lancement de l'application...
echo.

if exist "mvnw.cmd" (
    mvnw.cmd javafx:run
) else (
    where mvn >nul 2>nul
    if %errorlevel% equ 0 (
        mvn javafx:run
    ) else (
        echo ❌ Erreur: Impossible de lancer l'application
        pause
        exit /b 1
    )
)

pause

