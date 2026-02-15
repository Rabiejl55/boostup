package entities.GFinancement;

import java.util.Objects;

public class Projet {
    private int id_projet;
    private String titre;
    private String description;
    private double budget;
    private String statut;

    public Projet() {}
    public Projet(int id_projet, String titre, String description, double budget, String statut) {
        this.id_projet = id_projet;
        this.titre = titre;
        this.description = description;
        this.budget = budget;
        this.statut = statut;
    }

    public int getId_projet() {
        return id_projet;
    }

    public void setId_projet(int id_projet) {
        this.id_projet = id_projet;
    }

    public String getTitre() {
        return titre;
    }

    public void setTitre(String titre) {
        this.titre = titre;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public double getBudget() {
        return budget;
    }

    public void setBudget(double budget) {
        this.budget = budget;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    @Override
    public String toString() {
        return "Projet{" +
                "id_projet=" + id_projet +
                ", titre='" + titre + '\'' +
                ", description='" + description + '\'' +
                ", budget=" + budget +
                ", statut='" + statut + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Projet projet = (Projet) o;
        return id_projet == projet.id_projet && Double.compare(budget, projet.budget) == 0 && Objects.equals(titre, projet.titre) && Objects.equals(description, projet.description) && Objects.equals(statut, projet.statut);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id_projet, titre, description, budget, statut);
    }
}
