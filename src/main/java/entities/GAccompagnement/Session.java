package entities.GAccompagnement;

import java.time.LocalDate;

public class Session {

    private int idSession;
    private LocalDate dateSession;
    private int duree;
    private String lieu;
    private String typeSession;
    private String objectif;
    private Coach coach;

    // ===== Constructeurs =====
    public Session(int idSession, LocalDate dateSession, int duree,
                   String lieu, String typeSession, String objectif, Coach coach) {
        this.idSession = idSession;
        this.dateSession = dateSession;
        this.duree = duree;
        this.lieu = lieu;
        this.typeSession = typeSession;
        this.objectif = objectif;
        this.coach = coach;
    }

    public Session(LocalDate dateSession, int duree,
                   String lieu, String typeSession, String objectif, Coach coach) {
        this.dateSession = dateSession;
        this.duree = duree;
        this.lieu = lieu;
        this.typeSession = typeSession;
        this.objectif = objectif;
        this.coach = coach;
    }

    // Constructeurs sans coach (optionnel)
    public Session(int idSession, LocalDate dateSession, int duree,
                   String lieu, String typeSession, String objectif) {
        this.idSession = idSession;
        this.dateSession = dateSession;
        this.duree = duree;
        this.lieu = lieu;
        this.typeSession = typeSession;
        this.objectif = objectif;
    }

    public Session(LocalDate dateSession, int duree, String lieu,
                   String typeSession, String objectif) {
        this.dateSession = dateSession;
        this.duree = duree;
        this.lieu = lieu;
        this.typeSession = typeSession;
        this.objectif = objectif;
    }

    // ===== Getters & Setters =====
    public int getIdSession() { return idSession; }
    public void setIdSession(int idSession) { this.idSession = idSession; }

    public LocalDate getDateSession() { return dateSession; }
    public void setDateSession(LocalDate dateSession) { this.dateSession = dateSession; }

    public int getDuree() { return duree; }
    public void setDuree(int duree) { this.duree = duree; }

    public String getLieu() { return lieu; }
    public void setLieu(String lieu) { this.lieu = lieu; }

    public String getTypeSession() { return typeSession; }
    public void setTypeSession(String typeSession) { this.typeSession = typeSession; }

    public String getObjectif() { return objectif; }
    public void setObjectif(String objectif) { this.objectif = objectif; }

    public Coach getCoach() { return coach; }
    public void setCoach(Coach coach) { this.coach = coach; }

    @Override
    public String toString() {
        return "Session{" +
                "idSession=" + idSession +
                ", dateSession=" + dateSession +
                ", duree=" + duree +
                ", lieu='" + lieu + '\'' +
                ", typeSession='" + typeSession + '\'' +
                ", objectif='" + objectif + '\'' +
                ", coach=" + (coach != null ? coach.getNom() + " " + coach.getPrenom() : "Aucun") +
                '}';
    }
}
