package entities.GCandidature;

import java.sql.Date;

public class DossierCandidature {
    private int idDossier;
    private String descriptionProjet;
    private String businessPlan;       // varchar(255) → chemin fichier ou nom fichier
    private Date dateCreation;
    private String etat;               // "COMPLET" ou "INCOMPLET"
    private int idCandidature;         // clé étrangère vers Candidature
    private boolean visible;
    private String nomDossier;
    private String nomCandidature;

    // Constructeur par défaut
    public DossierCandidature() {
        this.visible = true;
        this.etat = "INCOMPLET";
    }

    // Constructeur utile (sans id)
    public DossierCandidature(String descriptionProjet, String businessPlan, Date dateCreation,
                              String etat, int idCandidature, boolean visible) {
        this.descriptionProjet = descriptionProjet;
        this.businessPlan = businessPlan;
        this.dateCreation = dateCreation;
        this.etat = etat != null ? etat : "INCOMPLET";
        this.idCandidature = idCandidature;
        this.visible = visible;
    }

    // Getters & Setters
    public int getIdDossier() { return idDossier; }
    public void setIdDossier(int idDossier) { this.idDossier = idDossier; }

    public String getDescriptionProjet() { return descriptionProjet; }
    public void setDescriptionProjet(String descriptionProjet) { this.descriptionProjet = descriptionProjet; }

    public String getBusinessPlan() { return businessPlan; }
    public void setBusinessPlan(String businessPlan) { this.businessPlan = businessPlan; }

    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }

    public String getEtat() { return etat; }
    public void setEtat(String etat) { this.etat = etat; }

    public int getIdCandidature() { return idCandidature; }
    public void setIdCandidature(int idCandidature) { this.idCandidature = idCandidature; }

    public boolean isVisible() { return visible; }
    public void setVisible(boolean visible) { this.visible = visible; }

    public String getNomDossier() { return nomDossier; }
    public void setNomDossier(String nomDossier) { this.nomDossier = nomDossier; }

    public String getNomCandidature() { return nomCandidature; }
    public void setNomCandidature(String nomCandidature) { this.nomCandidature = nomCandidature; }
    @Override
    public String toString() {
        return "DossierCandidature{" +
                "idDossier=" + idDossier +
                ", descriptionProjet='" + descriptionProjet + '\'' +
                ", businessPlan='" + businessPlan + '\'' +
                ", dateCreation=" + dateCreation +
                ", etat='" + etat + '\'' +
                ", idCandidature=" + idCandidature +
                ", visible=" + visible +
                '}';
    }
}