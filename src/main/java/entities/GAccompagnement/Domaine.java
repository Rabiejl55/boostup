package entities.GAccompagnement;

public class Domaine {

    private int id;
    private String nom;
    private String description;
    private String niveau;
    private String statut;
    private String image;

    // Constructeur vide
    public Domaine() {}

    // Constructeur complet avec id
    public Domaine(int id, String nom, String description, String niveau, String statut, String image) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.niveau = niveau;
        this.statut = statut;
        this.image = image;
    }

    // Constructeur sans id
    public Domaine(String nom, String description, String niveau, String statut, String image) {
        this.nom = nom;
        this.description = description;
        this.niveau = niveau;
        this.statut = statut;
        this.image = image;
    }

    // Constructeur simplifié pour recherche
    public Domaine(int id, String nom, String description, String image) {
        this.id = id;
        this.nom = nom;
        this.description = description;
        this.image = image;
    }

    // GETTERS & SETTERS
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getNiveau() { return niveau; }
    public void setNiveau(String niveau) { this.niveau = niveau; }

    public String getStatut() { return statut; }
    public void setStatut(String statut) { this.statut = statut; }

    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    @Override
    public String toString() {
        return nom; // affichage dans la ComboBox
    }
}