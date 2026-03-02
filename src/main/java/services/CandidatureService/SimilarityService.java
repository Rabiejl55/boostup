package services.CandidatureService;

import entities.GCandidature.Candidature;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service de détection de candidatures similaires.
 * Algorithme : TF-IDF + Cosine Similarity (sans dépendance externe).
 * Compare les champs : nomCandidature + nomStartup + commentaire.
 */
public class SimilarityService {

    // ── Stopwords français à ignorer ──────────────────────────────
    private static final Set<String> STOPWORDS = Set.of(
            "le","la","les","de","du","des","un","une","et","en","à","au","aux",
            "est","sont","pour","par","sur","dans","avec","que","qui","se","ce",
            "il","elle","ils","elles","nous","vous","je","tu","mon","ma","mes",
            "notre","votre","leur","leurs","pas","plus","très","aussi","mais",
            "ou","si","car","donc","or","ni","ne","y","on","tout","tous","bien",
            "cette","cet","ces","son","sa","ses","qu","l","d","j","m","n","s","c"
    );

    // ─── API PRINCIPALE ───────────────────────────────────────────

    /**
     * Résultat d'une comparaison entre deux candidatures.
     */
    public static class SimilarityResult {
        public final Candidature candidatureA;
        public final Candidature candidatureB;
        public final double score; // 0.0 → 1.0
        public final int pourcentage; // 0 → 100

        public SimilarityResult(Candidature a, Candidature b, double score) {
            this.candidatureA = a;
            this.candidatureB = b;
            this.score        = score;
            this.pourcentage  = (int) Math.round(score * 100);
        }

        public String getNiveau() {
            if (pourcentage >= 90) return "CRITIQUE";
            if (pourcentage >= 75) return "ÉLEVÉ";
            if (pourcentage >= 50) return "MODÉRÉ";
            return "FAIBLE";
        }

        public String getCouleur() {
            return switch (getNiveau()) {
                case "CRITIQUE" -> "#ef4444";
                case "ÉLEVÉ"    -> "#f59e0b";
                case "MODÉRÉ"   -> "#3b82f6";
                default          -> "#10b981";
            };
        }
    }

    /**
     * Analyse toutes les candidatures et retourne les paires
     * dont le score de similarité dépasse le seuil.
     *
     * @param candidatures  liste des candidatures à comparer
     * @param seuilPourcent seuil configurable (ex: 70 pour 70%)
     * @return liste triée par score décroissant
     */
    public List<SimilarityResult> detecterSimilarites(
            List<Candidature> candidatures, int seuilPourcent) {

        double seuil = seuilPourcent / 100.0;
        List<SimilarityResult> resultats = new ArrayList<>();

        // Pré-calcul des vecteurs TF-IDF pour chaque candidature
        List<String> corpus = candidatures.stream()
                .map(this::extraireTexte)
                .collect(Collectors.toList());

        // Vocabulaire global
        Set<String> vocab = new LinkedHashSet<>();
        List<Map<String, Integer>> tfs = new ArrayList<>();
        for (String texte : corpus) {
            Map<String, Integer> tf = calculerTF(texte);
            tfs.add(tf);
            vocab.addAll(tf.keySet());
        }

        List<String> vocabList = new ArrayList<>(vocab);
        Map<String, Double> idf = calculerIDF(tfs, vocabList);

        // Vecteurs TF-IDF
        List<double[]> vecteurs = new ArrayList<>();
        for (Map<String, Integer> tf : tfs) {
            vecteurs.add(construireVecteur(tf, idf, vocabList));
        }

        // Comparaison de toutes les paires (n*(n-1)/2)
        for (int i = 0; i < candidatures.size(); i++) {
            for (int j = i + 1; j < candidatures.size(); j++) {
                double sim = cosineSimilarity(vecteurs.get(i), vecteurs.get(j));
                if (sim >= seuil) {
                    resultats.add(new SimilarityResult(
                            candidatures.get(i),
                            candidatures.get(j),
                            sim));
                }
            }
        }

        // Tri par score décroissant
        resultats.sort((a, b) -> Double.compare(b.score, a.score));
        return resultats;
    }

    /**
     * Calcule le score de similarité entre deux candidatures.
     * Utile pour une comparaison ponctuelle.
     */
    public double calculerSimilarite(Candidature a, Candidature b) {
        return detecterSimilarites(List.of(a, b), 0)
                .stream().findFirst()
                .map(r -> r.score)
                .orElse(0.0);
    }

    // ─── EXTRACTION TEXTE ─────────────────────────────────────────

    private String extraireTexte(Candidature c) {
        // On combine les champs textuels significatifs
        // Le commentaire a plus de poids (répété 2x)
        return String.join(" ",
                nvl(c.getNomCandidature()),
                nvl(c.getNomStartup()),
                nvl(c.getCommentaire()),
                nvl(c.getCommentaire()) // poids x2
        ).toLowerCase();
    }

    // ─── TF (Term Frequency) ──────────────────────────────────────

    private Map<String, Integer> calculerTF(String texte) {
        Map<String, Integer> tf = new HashMap<>();
        String[] mots = tokeniser(texte);
        for (String mot : mots) {
            if (!STOPWORDS.contains(mot) && mot.length() > 2) {
                tf.merge(mot, 1, Integer::sum);
            }
        }
        return tf;
    }

    private String[] tokeniser(String texte) {
        return texte.replaceAll("[^a-zàâäéèêëîïôùûüç\\s]", " ")
                .trim()
                .split("\\s+");
    }

    // ─── IDF (Inverse Document Frequency) ────────────────────────

    private Map<String, Double> calculerIDF(
            List<Map<String, Integer>> tfs, List<String> vocab) {
        int n = tfs.size();
        Map<String, Double> idf = new HashMap<>();
        for (String terme : vocab) {
            long docs = tfs.stream().filter(tf -> tf.containsKey(terme)).count();
            idf.put(terme, Math.log((double)(n + 1) / (docs + 1)) + 1.0);
        }
        return idf;
    }

    // ─── VECTEUR TF-IDF ───────────────────────────────────────────

    private double[] construireVecteur(
            Map<String, Integer> tf, Map<String, Double> idf, List<String> vocab) {
        double[] v = new double[vocab.size()];
        for (int i = 0; i < vocab.size(); i++) {
            String terme = vocab.get(i);
            int freq = tf.getOrDefault(terme, 0);
            double idfVal = idf.getOrDefault(terme, 1.0);
            v[i] = freq * idfVal;
        }
        return v;
    }

    // ─── COSINE SIMILARITY ────────────────────────────────────────

    private double cosineSimilarity(double[] a, double[] b) {
        double dot = 0, normA = 0, normB = 0;
        for (int i = 0; i < a.length; i++) {
            dot   += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        if (normA == 0 || normB == 0) return 0.0;
        return dot / (Math.sqrt(normA) * Math.sqrt(normB));
    }

    private String nvl(String s) { return s == null ? "" : s; }
}