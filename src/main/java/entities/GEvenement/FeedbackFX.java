package entities.GEvenement;

import javafx.beans.property.*;

import java.sql.Date;

public class FeedbackFX {
    private final IntegerProperty id = new SimpleIntegerProperty();
    private final StringProperty commentaire = new SimpleStringProperty();
    private final IntegerProperty note = new SimpleIntegerProperty();
    private final ObjectProperty<Date> dateFeedback = new SimpleObjectProperty<>();
    private final IntegerProperty idParticipation = new SimpleIntegerProperty();

    // Nouveau: libellé lisible pour la participation (ex: "Event X - Investisseur Y")
    private final StringProperty participationLabel = new SimpleStringProperty();

    // Constructeur par défaut
    public FeedbackFX() {
    }

    // Constructeur depuis Feedback
    public FeedbackFX(Feedback f) {
        if (f != null) {
            this.id.set(f.getId());
            this.commentaire.set(f.getCommentaire());
            this.note.set(f.getNote());
            this.dateFeedback.set(f.getDateFeedback());
            this.idParticipation.set(f.getIdParticipation());
        }
    }

    // Convertir en Feedback
    public Feedback toFeedback() {
        return new Feedback(
                getId(),
                getCommentaire(),
                getNote(),
                getDateFeedback(),
                getIdParticipation()
        );
    }

    // Properties
    public IntegerProperty idProperty() { return id; }
    public StringProperty commentaireProperty() { return commentaire; }
    public IntegerProperty noteProperty() { return note; }
    public ObjectProperty<Date> dateFeedbackProperty() { return dateFeedback; }
    public IntegerProperty idParticipationProperty() { return idParticipation; }
    public StringProperty participationLabelProperty() { return participationLabel; }

    // Getters/Setters
    public int getId() { return id.get(); }
    public void setId(int id) { this.id.set(id); }

    public String getCommentaire() { return commentaire.get(); }
    public void setCommentaire(String commentaire) { this.commentaire.set(commentaire); }

    public int getNote() { return note.get(); }
    public void setNote(int note) { this.note.set(note); }

    public Date getDateFeedback() { return dateFeedback.get(); }
    public void setDateFeedback(Date dateFeedback) { this.dateFeedback.set(dateFeedback); }

    public int getIdParticipation() { return idParticipation.get(); }
    public void setIdParticipation(int idParticipation) { this.idParticipation.set(idParticipation); }

    public String getParticipationLabel() { return participationLabel.get(); }
    public void setParticipationLabel(String label) { this.participationLabel.set(label); }
}