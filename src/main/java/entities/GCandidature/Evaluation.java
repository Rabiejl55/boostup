package entities.GCandidature;

public class Evaluation {
    private int idEvaluation;
    private Integer noteInnovation;
    private Integer noteViabilite;
    private Integer noteMarche;
    private Integer noteEquipe;
    private Double noteGlobale;
    private String decision; // "ACCEPTEE" ou "REFUSEE"
    private int idCandidature;
    private boolean visible;
    private String nomEvaluation;
    private String nomCandidature;

    // Constructeur par défaut
    public Evaluation() {
        this.visible = true;
        this.decision = "REFUSEE"; // Default comme en DB
    }

    // Constructeur utile (sans id)
    public Evaluation(Integer noteInnovation, Integer noteViabilite, Integer noteMarche, Integer noteEquipe,
                      int idCandidature, boolean visible) {
        this.noteInnovation = noteInnovation;
        this.noteViabilite = noteViabilite;
        this.noteMarche = noteMarche;
        this.noteEquipe = noteEquipe;
        this.idCandidature = idCandidature;
        this.visible = visible;
        calculateNoteGlobaleAndDecision(); // Calcul auto
    }

    // Méthode pour calculer noteGlobale et decision automatiquement
    public void calculateNoteGlobaleAndDecision() {
        // Moyenne des notes non-null (assumer que toutes sont saisies pour calcul)
        int count = 0;
        double sum = 0.0;
        if (noteInnovation != null) { sum += noteInnovation; count++; }
        if (noteViabilite != null) { sum += noteViabilite; count++; }
        if (noteMarche != null) { sum += noteMarche; count++; }
        if (noteEquipe != null) { sum += noteEquipe; count++; }

        if (count > 0) {
            this.noteGlobale = sum / count;
            this.decision = (noteGlobale >= 7.5) ? "ACCEPTEE" : "REFUSEE";
        } else {
            this.noteGlobale = null;
            this.decision = "REFUSEE";
        }
    }

    // Getters & Setters (avec recalcul si notes changent)
    public int getIdEvaluation() { return idEvaluation; }
    public void setIdEvaluation(int idEvaluation) { this.idEvaluation = idEvaluation; }

    public Integer getNoteInnovation() { return noteInnovation; }
    public void setNoteInnovation(Integer noteInnovation) { this.noteInnovation = noteInnovation; calculateNoteGlobaleAndDecision(); }

    public Integer getNoteViabilite() { return noteViabilite; }
    public void setNoteViabilite(Integer noteViabilite) { this.noteViabilite = noteViabilite; calculateNoteGlobaleAndDecision(); }

    public Integer getNoteMarche() { return noteMarche; }
    public void setNoteMarche(Integer noteMarche) { this.noteMarche = noteMarche; calculateNoteGlobaleAndDecision(); }

    public Integer getNoteEquipe() { return noteEquipe; }
    public void setNoteEquipe(Integer noteEquipe) { this.noteEquipe = noteEquipe; calculateNoteGlobaleAndDecision(); }

    public Double getNoteGlobale() { return noteGlobale; }
    public void setNoteGlobale(Double noteGlobale) { this.noteGlobale = noteGlobale;
    }
    public String getDecision() { return decision; }
    public void setDecision(String decision) { this.decision = decision; }

    public int getIdCandidature() { return idCandidature; }
    public void setIdCandidature(int idCandidature) { this.idCandidature = idCandidature; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public String getNomEvaluation() { return nomEvaluation; }
    public void setNomEvaluation(String nomEvaluation) { this.nomEvaluation = nomEvaluation; }

    public String getNomCandidature() { return nomCandidature; }
    public void setNomCandidature(String nomCandidature) { this.nomCandidature = nomCandidature; }
    @Override
    public String toString() {
        return "Evaluation{" +
                "idEvaluation=" + idEvaluation +
                ", noteInnovation=" + noteInnovation +
                ", noteViabilite=" + noteViabilite +
                ", noteMarche=" + noteMarche +
                ", noteEquipe=" + noteEquipe +
                ", noteGlobale=" + noteGlobale +
                ", decision='" + decision + '\'' +
                ", idCandidature=" + idCandidature +
                ", visible=" + visible +
                '}';
    }
}