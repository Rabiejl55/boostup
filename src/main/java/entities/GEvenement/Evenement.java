package entities.GEvenement;

import java.sql.Date;
import java.util.Objects;

public class Evenement {
    private int id;
    private String titre;
    private String type;
    private Date dateEvenement;
    private String lieu;
    private String description;
    private int capaciteMax;
    private String image; // URL ou chemin
    private boolean archived; // soft delete

    // Constructeurs
    public Evenement() {}

    public Evenement(String titre, String type, Date dateEvenement, String lieu, String description, int capaciteMax) {
        this.titre = titre;
        this.type = type;
        this.dateEvenement = dateEvenement;
        this.lieu = lieu;
        this.description = description;
        this.capaciteMax = capaciteMax;
    }

    public Evenement(String titre, String type, Date dateEvenement, String lieu, String description, int capaciteMax, String image) {
        this.titre = titre;
        this.type = type;
        this.dateEvenement = dateEvenement;
        this.lieu = lieu;
        this.description = description;
        this.capaciteMax = capaciteMax;
        this.image = image;
    }

    public Evenement(int id, String titre, String type, Date dateEvenement, String lieu, String description, int capaciteMax) {
        this.id = id;
        this.titre = titre;
        this.type = type;
        this.dateEvenement = dateEvenement;
        this.lieu = lieu;
        this.description = description;
        this.capaciteMax = capaciteMax;
    }

    public Evenement(int id, String titre, String type, Date dateEvenement, String lieu, String description, int capaciteMax, String image) {
        this.id = id;
        this.titre = titre;
        this.type = type;
        this.dateEvenement = dateEvenement;
        this.lieu = lieu;
        this.description = description;
        this.capaciteMax = capaciteMax;
        this.image = image;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getTitre() { return titre; }
    public void setTitre(String titre) { this.titre = titre; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public Date getDateEvenement() { return dateEvenement; }
    public void setDateEvenement(Date dateEvenement) { this.dateEvenement = dateEvenement; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public int getCapaciteMax() { return capaciteMax; }
    public void setCapaciteMax(int capaciteMax) { this.capaciteMax = capaciteMax; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }

    public boolean isArchived() { return archived; }
    public void setArchived(boolean archived) { this.archived = archived; }

    @Override
    public String toString() {
        return "Evenement{" +
                "id=" + id +
                ", titre='" + titre + '\'' +
                ", type='" + type + '\'' +
                ", dateEvenement=" + dateEvenement +
                ", lieu='" + lieu + '\'' +
                ", capaciteMax=" + capaciteMax +
                ", image='" + image + '\'' +
                ", archived=" + archived +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Evenement evenement = (Evenement) o;
        return id == evenement.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}