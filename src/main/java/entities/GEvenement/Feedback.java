package entities.GEvenement;

import java.sql.Date;
import java.util.Objects;

public class Feedback {
    private int id;
    private String commentaire;
    private int note;
    private Date dateFeedback;
    private int idParticipation;

    // Constructeurs
    public Feedback() {}

    public Feedback(String commentaire, int note, Date dateFeedback, int idParticipation) {
        this.commentaire = commentaire;
        this.note = note;
        this.dateFeedback = dateFeedback;
        this.idParticipation = idParticipation;
    }

    public Feedback(int id, String commentaire, int note, Date dateFeedback, int idParticipation) {
        this.id = id;
        this.commentaire = commentaire;
        this.note = note;
        this.dateFeedback = dateFeedback;
        this.idParticipation = idParticipation;
    }

    // Getters & Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getCommentaire() { return commentaire; }
    public void setCommentaire(String commentaire) { this.commentaire = commentaire; }

    public int getNote() { return note; }
    public void setNote(int note) { this.note = note; }

    public Date getDateFeedback() { return dateFeedback; }
    public void setDateFeedback(Date dateFeedback) { this.dateFeedback = dateFeedback; }

    public int getIdParticipation() { return idParticipation; }
    public void setIdParticipation(int idParticipation) { this.idParticipation = idParticipation; }

    @Override
    public String toString() {
        return "Feedback{" +
                "id=" + id +
                ", commentaire='" + commentaire + '\'' +
                ", note=" + note +
                ", dateFeedback=" + dateFeedback +
                ", idParticipation=" + idParticipation +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Feedback feedback = (Feedback) o;
        return id == feedback.id;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }
}