package services.api;

public class AiPrompts {

    public static String buildProjectSummaryPrompt(
            String titre,
            String description,
            double budget,
            String statut
    ) {

        return """
        Tu es un analyste financier professionnel.

        Analyse le projet suivant et génère un résumé professionnel structuré.

        Titre : %s
        Description : %s
        Budget : %.2f
        Statut : %s

        Fournis :
        1. Résumé général
        2. Évaluation financière
        3. Recommandation stratégique

        Réponse claire et professionnelle.
        """.formatted(titre, description, budget, statut);
    }
}
