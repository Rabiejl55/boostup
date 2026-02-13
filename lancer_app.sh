#!/bin/bash
# Script de lancement pour Linux/Mac
# BoostUp - Application de gestion d'événements

echo "==================================="
echo "  🚀 BoostUp - Gestion d'Événements"
echo "==================================="
echo ""

# Vérifier que pom.xml existe
if [ ! -f "pom.xml" ]; then
    echo "❌ Erreur: pom.xml non trouvé!"
    echo "Assurez-vous de lancer ce script depuis la racine du projet."
    exit 1
fi

echo "✅ Projet détecté"
echo ""

# Compilation
echo "📦 Compilation du projet..."
echo ""

mvn clean compile

if [ $? -ne 0 ]; then
    echo ""
    echo "❌ Compilation échouée!"
    exit 1
fi

echo ""
echo "✅ Compilation réussie!"
echo ""

# Lancement de l'application
echo "🎯 Lancement de l'application..."
echo ""

mvn javafx:run

