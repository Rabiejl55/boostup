package entities.GUtilisateurs;

import java.time.LocalDateTime;
import java.util.Objects;

public class User {

    private int id;
    private String nom;
    private String email;
    private String mdp;
    private String role;  // Ou vous pourriez utiliser RoleEnum comme type
    private boolean active;
    private LocalDateTime dateCreation;
    private String fullname;  // Nom complet
    private String phone;     // Numéro de téléphone
    private String avatar;    // URL ou chemin de l'avatar

    public User() {
    }

    public User(int id, String nom, String email, String mdp, String role, boolean active, LocalDateTime dateCreation) {
        this.id = id;
        this.nom = nom;
        this.email = email;
        this.mdp = mdp;
        this.role = role;
        this.active = active;
        this.dateCreation = dateCreation;
    }

    public String getMdp() {
        return mdp;
    }

    public void setMdp(String mdp) {
        this.mdp = mdp;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }

    public LocalDateTime getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDateTime dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id && active == user.active && Objects.equals(nom, user.nom) && Objects.equals(email, user.email) && Objects.equals(mdp, user.mdp) && Objects.equals(role, user.role) && Objects.equals(dateCreation, user.dateCreation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nom, email, mdp, role, active, dateCreation);
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", email='" + email + '\'' +
                ", mdp='" + mdp + '\'' +
                ", role='" + role + '\'' +
                ", active=" + active +
                ", dateCreation=" + dateCreation +
                '}';
    }
}