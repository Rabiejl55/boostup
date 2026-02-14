package entities.GEvenement;

import java.sql.Date;
import java.util.Objects;

public class Participation {  // Note: Il y a une faute de frappe "Particpation" au lieu de "Participation"
    private int id;
    private String nomStartup;
    private String nomInvestisseur;
    private boolean presence;
    private Date dateInscription;
    private int idEvenement;

    // Constructeurs
    public Participation() {}

    public Participation(String nomStartup, String nomInvestisseur, boolean presence, Date dateInscription, int idEvenement) {
        this.nomStartup = nomStartup;
        this.nomInvestisseur = nomInvestisseur;
        this.presence = presence;
        this.dateInscription = dateInscription;
        this.idEvenement = idEvenement;
    }

    public Participation(int id, String nomStartup, String nomInvestisseur, boolean presence, Date dateInscription, int idEvenement) {
        this.id = id;
        this.nomStartup = nomStartup;
        this.nomInvestisseur = nomInvestisseur;
        this.presence = presence;
        this.dateInscription = dateInscription;
        this.idEvenement = idEvenement;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNomStartup() { return nomStartup; }
    public void setNomStartup(String nomStartup) { this.nomStartup = nomStartup; }

    public String getNomInvestisseur() { return nomInvestisseur; }
    public void setNomInvestisseur(String nomInvestisseur) { this.nomInvestisseur = nomInvestisseur; }

    public boolean isPresence() { return presence; }
    public void setPresence(boolean presence) { this.presence = presence; }

    public Date getDateInscription() { return dateInscription; }
    public void setDateInscription(Date dateInscription) { this.dateInscription = dateInscription; }

    public int getIdEvenement() { return idEvenement; }
    public void setIdEvenement(int idEvenement) { this.idEvenement = idEvenement; }

    @Override
    public String toString() {
        return "Particpation{" +
                "id=" + id +
                ", nomStartup='" + nomStartup + '\'' +
                ", nomInvestisseur='" + nomInvestisseur + '\'' +
                ", presence=" + presence +
                ", dateInscription=" + dateInscription +
                ", idEvenement=" + idEvenement +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Participation that = (Participation) o;
        return id == that.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}