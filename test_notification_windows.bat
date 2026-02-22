@echo off
echo.
echo ========================================
echo   TEST NOTIFICATION WINDOWS - BOOSTUP
echo ========================================
echo.
echo Compilation du fichier de test...
javac -d target\classes -cp "src\main\java" src\main\java\utils\WindowsToastNotifier.java
echo.
echo Lancement du test de notification...
echo.
java -cp "target\classes" utils.WindowsToastNotifier
echo.
echo ========================================
echo   FIN DU TEST
echo ========================================
echo.
echo Verifie ton centre de notifications Windows!
echo (Coin en bas a droite de la barre des taches)
echo.
pause

