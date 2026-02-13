package entities.GEvenement;

import javafx.beans.property.*;

import java.sql.Date;

public class EvenementFX {
    // Properties JavaFX
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty titre = new SimpleStringProperty();
    private final StringProperty type = new SimpleStringProperty();
    private final ObjectProperty<Date> dateEvenement = new SimpleObjectProperty<>();
    private final StringProperty lieu = new SimpleStringProperty();
    private final StringProperty description = new SimpleStringProperty();
    private final IntegerProperty capaciteMax = new SimpleIntegerProperty();
    private final StringProperty image = new SimpleStringProperty();

    // ================= CONSTRUCTEURS =================

    // Constructeur vide (pour JavaFX TableView)
    public EvenementFX() {
    }

    // Constructeur avec paramètres
    public EvenementFX(String titre, String type, Date dateEvenement,
                       String lieu, String description, int capaciteMax) {
        this.titre.set(titre);
        this.type.set(type);
        this.dateEvenement.set(dateEvenement);
        this.lieu.set(lieu);
        this.description.set(description);
        this.capaciteMax.set(capaciteMax);
    }

    public EvenementFX(String titre, String type, Date dateEvenement,
                       String lieu, String description, int capaciteMax, String image) {
        this.titre.set(titre);
        this.type.set(type);
        this.dateEvenement.set(dateEvenement);
        this.lieu.set(lieu);
        this.description.set(description);
        this.capaciteMax.set(capaciteMax);
        this.image.set(image);
    }

    // Constructeur avec ID
    public EvenementFX(int id, String titre, String type, Date dateEvenement,
                       String lieu, String description, int capaciteMax) {
        this.id.set(id);
        this.titre.set(titre);
        this.type.set(type);
        this.dateEvenement.set(dateEvenement);
        this.lieu.set(lieu);
        this.description.set(description);
        this.capaciteMax.set(capaciteMax);
    }

    public EvenementFX(int id, String titre, String type, Date dateEvenement,
                       String lieu, String description, int capaciteMax, String image) {
        this.id.set(id);
        this.titre.set(titre);
        this.type.set(type);
        this.dateEvenement.set(dateEvenement);
        this.lieu.set(lieu);
        this.description.set(description);
        this.capaciteMax.set(capaciteMax);
        this.image.set(image);
    }

    // Constructeur depuis votre Evenement existant (CONVERSION)
    public EvenementFX(Evenement e) {
        if (e != null) {
            this.id.set(e.getId());
            this.titre.set(e.getTitre());
            this.type.set(e.getType());
            this.dateEvenement.set(e.getDateEvenement());
            this.lieu.set(e.getLieu());
            this.description.set(e.getDescription());
            this.capaciteMax.set(e.getCapaciteMax());
            this.image.set(e.getImage());
        }
    }

    // ================= CONVERSION =================

    // Convertir EvenementFX → Evenement (pour vos services)
    public Evenement toEvenement() {
        return new Evenement(
                getId(),
                getTitre(),
                getType(),
                getDateEvenement(),
                getLieu(),
                getDescription(),
                getCapaciteMax(),
                getImage()
        );
    }

    // Convertir Evenement → EvenementFX (méthode statique)
    public static EvenementFX fromEvenement(Evenement e) {
        return new EvenementFX(e);
    }

    // ================= PROPERTIES (pour JavaFX Binding) =================

    public IntegerProperty idProperty() {
        return id;
    }

    public StringProperty titreProperty() {
        return titre;
    }

    public StringProperty typeProperty() {
        return type;
    }

    public ObjectProperty<Date> dateEvenementProperty() {
        return dateEvenement;
    }

    public StringProperty lieuProperty() {
        return lieu;
    }

    public StringProperty descriptionProperty() {
        return description;
    }

    public IntegerProperty capaciteMaxProperty() {
        return capaciteMax;
    }

    public StringProperty imageProperty() { return image; }

    // ================= GETTERS =================

    public int getId() {
        return id.get();
    }

    public String getTitre() {
        return titre.get();
    }

    public String getType() {
        return type.get();
    }

    public Date getDateEvenement() {
        return dateEvenement.get();
    }

    public String getLieu() {
        return lieu.get();
    }

    public String getDescription() {
        return description.get();
    }

    public int getCapaciteMax() {
        return capaciteMax.get();
    }

    public String getImage() { return image.get(); }

    // ================= SETTERS =================

    public void setId(int id) {
        this.id.set(id);
    }

    public void setTitre(String titre) {
        this.titre.set(titre);
    }

    public void setType(String type) {
        this.type.set(type);
    }

    public void setDateEvenement(Date dateEvenement) {
        this.dateEvenement.set(dateEvenement);
    }

    public void setLieu(String lieu) {
        this.lieu.set(lieu);
    }

    public void setDescription(String description) {
        this.description.set(description);
    }

    public void setCapaciteMax(int capaciteMax) {
        this.capaciteMax.set(capaciteMax);
    }

    public void setImage(String image) { this.image.set(image); }

    // ================= MÉTHODES UTILES =================

    @Override
    public String toString() {
        return getTitre() + " (" + getType() + ")";
    }

    // Pour l'affichage dans TableView
    public String getDateFormatted() {
        return getDateEvenement() != null ? getDateEvenement().toString() : "";
    }

    // Pour le filtrage/recherche
    public boolean matchesSearch(String searchText) {
        if (searchText == null || searchText.isEmpty()) return true;

        String lowerSearch = searchText.toLowerCase();
        return (getTitre() != null && getTitre().toLowerCase().contains(lowerSearch)) ||
                (getType() != null && getType().toLowerCase().contains(lowerSearch)) ||
                (getLieu() != null && getLieu().toLowerCase().contains(lowerSearch)) ||
                (getDescription() != null && getDescription().toLowerCase().contains(lowerSearch));
    }
}