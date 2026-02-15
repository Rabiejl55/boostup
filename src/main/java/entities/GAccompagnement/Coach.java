package entities.GAccompagnement;

public class Coach {
    private int idCoach;
    private String nom;
    private String prenom;
    private String email;
    private String telephone;
    private String imagecoach; // nouvel attribut

    // Constructeur vide
    public Coach() {
    }

    // Constructeur sans id (pour insertion)
    public Coach(String nom, String prenom, String email, String telephone, String imagecoach) {
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.imagecoach = imagecoach;
    }

    // Constructeur avec id (pour récupération depuis DB)
    public Coach(int idCoach, String nom, String prenom, String email, String telephone, String imagecoach) {
        this.idCoach = idCoach;
        this.nom = nom;
        this.prenom = prenom;
        this.email = email;
        this.telephone = telephone;
        this.imagecoach = imagecoach;
    }

    // Getters et Setters
    public int getIdCoach() {
        return idCoach;
    }

    public void setIdCoach(int idCoach) {
        this.idCoach = idCoach;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    public String getImagecoach() {
        return imagecoach;
    }

    public void setImagecoach(String imagecoach) {
        this.imagecoach = imagecoach;
    }

    @Override
    public String toString() {
        return "Coach{" +
                "idCoach=" + idCoach +
                ", nom='" + nom + '\'' +
                ", prenom='" + prenom + '\'' +
                ", email='" + email + '\'' +
                ", telephone='" + telephone + '\'' +
                ", imagecoach='" + imagecoach + '\'' +
                '}';
    }

}
