package entities.GCandidature;

import java.sql.Date;

public class Candidature {
    private int idCandidature;
    private Date dateDepot;
    private String statut; // "EN_ATTENTE", "VALIDEE", "REFUSEE"
    private Double score;
    private String commentaire;
    private int idStartup;
    private boolean visible;
    private String nomCandidature;
    private String nomStartup;
    private String emailContact; // ✅ NOUVEAU — email de la startup pour les notifications

    public Candidature() {}

    public Candidature(Date dateDepot, String statut, Double score, String commentaire,
                       int idStartup, boolean visible) {
        this.dateDepot   = dateDepot;
        this.statut      = statut;
        this.score       = score;
        this.commentaire = commentaire;
        this.idStartup   = idStartup;
        this.visible     = visible;
    }

    // ── Getters / Setters ────────────────────────────────────────

    public int getIdCandidature()                  { return idCandidature; }
    public void setIdCandidature(int id)           { this.idCandidature = id; }

    public Date getDateDepot()                     { return dateDepot; }
    public void setDateDepot(Date dateDepot)       { this.dateDepot = dateDepot; }

    public String getStatut()                      { return statut; }
    public void setStatut(String statut)           { this.statut = statut; }

    public Double getScore()                       { return score; }
    public void setScore(Double score)             { this.score = score; }

    public String getCommentaire()                 { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public int getIdStartup()                      { return idStartup; }
    public void setIdStartup(int idStartup)        { this.idStartup = idStartup; }

    public String getNomCandidature()              { return nomCandidature; }
    public void setNomCandidature(String nom)      { this.nomCandidature = nom; }

    public String getNomStartup()                  { return nomStartup; }
    public void setNomStartup(String nom)          { this.nomStartup = nom; }

    public boolean isVisible()                     { return visible; }
    public void setVisible(boolean visible)        { this.visible = visible; }

    // ✅ NOUVEAU
    public String getEmailContact()                { return emailContact; }
    public void setEmailContact(String email)      { this.emailContact = email; }

    @Override
    public String toString() {
        return "Candidature{id=" + idCandidature + ", statut='" + statut
                + "', nom='" + nomCandidature + "', startup='" + nomStartup + "'}";
    }
}