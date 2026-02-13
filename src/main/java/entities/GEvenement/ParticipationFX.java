package entities.GEvenement;

import javafx.beans.property.*;
import java.sql.Date;

public class ParticipationFX {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty nomStartup = new SimpleStringProperty();
    private final StringProperty nomInvestisseur = new SimpleStringProperty();
    private final BooleanProperty presence = new SimpleBooleanProperty();
    private final ObjectProperty<Date> dateInscription = new SimpleObjectProperty<>();
    private final IntegerProperty idEvenement = new SimpleIntegerProperty();

    // Nouveau: titre/nom de l'événement (pour l'affichage via jointure)
    private final StringProperty evenementTitre = new SimpleStringProperty();

    // Constructeur par défaut
    public ParticipationFX() {
    }

    // Constructeur depuis Participation
    public ParticipationFX(Participation p) {
        if (p != null) {
            this.id.set(p.getId());
            this.nomStartup.set(p.getNomStartup());
            this.nomInvestisseur.set(p.getNomInvestisseur());
            this.presence.set(p.isPresence());
            this.dateInscription.set(p.getDateInscription());
            this.idEvenement.set(p.getIdEvenement());
        }
    }

    // Convertir en Participation
    public Participation toParticipation() {
        return new Participation(
                getId(),
                getNomStartup(),
                getNomInvestisseur(),
                isPresence(),
                getDateInscription(),
                getIdEvenement()
        );
    }

    // Properties
    public IntegerProperty idProperty() { return id; }
    public StringProperty nomStartupProperty() { return nomStartup; }
    public StringProperty nomInvestisseurProperty() { return nomInvestisseur; }
    public BooleanProperty presenceProperty() { return presence; }
    public ObjectProperty<Date> dateInscriptionProperty() { return dateInscription; }
    public IntegerProperty idEvenementProperty() { return idEvenement; }
    public StringProperty evenementTitreProperty() { return evenementTitre; }

    // Getters/Setters
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }

    public String getNomStartup() { return nomStartup.get(); }
    public void setNomStartup(String nomStartup) { this.nomStartup.set(nomStartup); }

    public String getNomInvestisseur() { return nomInvestisseur.get(); }
    public void setNomInvestisseur(String nomInvestisseur) { this.nomInvestisseur.set(nomInvestisseur); }

    public boolean isPresence() { return presence.get(); }
    public void setPresence(boolean presence) { this.presence.set(presence); }

    public Date getDateInscription() { return dateInscription.get(); }
    public void setDateInscription(Date dateInscription) { this.dateInscription.set(dateInscription); }

    public int getIdEvenement() { return idEvenement.get(); }
    public void setIdEvenement(int idEvenement) { this.idEvenement.set(idEvenement); }

    public String getEvenementTitre() { return evenementTitre.get(); }
    public void setEvenementTitre(String titre) { this.evenementTitre.set(titre); }
}