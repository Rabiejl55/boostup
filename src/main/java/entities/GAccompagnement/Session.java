package entities.GAccompagnement;

import java.time.LocalDate;
import java.util.List;

public class Session {

    private int idSession;
    private LocalDate dateSession;
    private int duree;
    private String lieu;
    private String typeSession;
    private String objectif;
    private Coach coach;
    private Domaine domaine;
    private Double latitude;
    private Double longitude;

    // ===== Constructeurs =====
    public Session(int idSession, LocalDate dateSession, int duree,
                   String lieu, String typeSession, String objectif,
                   Coach coach, Domaine domaine) {
        this.idSession = idSession;
        this.dateSession = dateSession;
        this.duree = duree;
        this.lieu = lieu;
        this.typeSession = typeSession;
        this.objectif = objectif;
        this.coach = coach;
        this.domaine = domaine;
    }

    public Session(LocalDate dateSession, int duree,
                   String lieu, String typeSession, String objectif,
                   Coach coach, Domaine domaine) {
        this.dateSession = dateSession;
        this.duree = duree;
        this.lieu = lieu;
        this.typeSession = typeSession;
        this.objectif = objectif;
        this.coach = coach;
        this.domaine = domaine;
    }

    // Constructeurs sans coach (facultatif)
    public Session(int idSession, LocalDate dateSession, int duree,
                   String lieu, String typeSession, String objectif,
                   Domaine domaine) {
        this.idSession = idSession;
        this.dateSession = dateSession;
        this.duree = duree;
        this.lieu = lieu;
        this.typeSession = typeSession;
        this.objectif = objectif;
        this.domaine = domaine;
    }

    public Session(LocalDate dateSession, int duree, String lieu,
                   String typeSession, String objectif,
                   Domaine domaine) {
        this.dateSession = dateSession;
        this.duree = duree;
        this.lieu = lieu;
        this.typeSession = typeSession;
        this.objectif = objectif;
        this.domaine = domaine;
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

    public Domaine getDomaine() { return domaine; }
    public void setDomaine(Domaine domaine) { this.domaine = domaine; }
    public Session() {
    }
    @Override
    public String toString() {
        return objectif; // ou typeSession + " - " + dateSession si tu veux un peu plus d'info
    }
    public void checkNotifications(List<Session> sessions) {
        LocalDate today = LocalDate.now();
        for (Session session : sessions) {
            LocalDate dateSession = session.getDateSession();
            if (dateSession != null && !dateSession.isBefore(today)) {
                // Vérifie si la session est dans 3 jours
                if (today.plusDays(3).equals(dateSession)) {
                    sendNotification(session);
                }
            }
        }
    }

    private void sendNotification(Session session) {
        // Ici tu peux personnaliser le message
        System.out.println("🔔 Rappel : La session '" + session.getTypeSession() +
                "' aura lieu le " + session.getDateSession() +
                " au lieu : " + session.getLieu());
    }
    public Double getLatitude() { return latitude; }
    public void setLatitude(Double latitude) { this.latitude = latitude; }

    public Double getLongitude() { return longitude; }
    public void setLongitude(Double longitude) { this.longitude = longitude; }
}
