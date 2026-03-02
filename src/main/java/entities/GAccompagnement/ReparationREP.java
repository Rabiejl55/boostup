package entities.GAccompagnement;

public class ReparationREP {
    private int annee;
    private String filiere;
    private String equipement;
    private String acteur;
    private String regionRR;
    private String depRR;
    private String categorie;
    private int nombreReparations;

    // Constructeur complet
    public ReparationREP(int annee, String filiere, String categorie, int nombre_reparations) {
        this.annee = annee;
        this.filiere = filiere;
        this.categorie = categorie;
        this.nombreReparations = nombre_reparations;
    }

    // Getters
    public int getAnnee() { return annee; }
    public String getFiliere() { return filiere; }
    public String getEquipement() { return equipement; }
    public String getActeur() { return acteur; }
    public String getRegionRR() { return regionRR; }
    public String getDepRR() { return depRR; }
    public String getCategorie() { return categorie; }
    public int getNombreReparations() { return nombreReparations; }
}