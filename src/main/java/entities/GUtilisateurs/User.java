package entities.GUtilisateurs;

import entities.Role_enum;

import java.util.Date;
import java.util.Objects;

public class User {
    private int id;
    private String nom;
    private String email;
    private String MDP;
    private Role_enum role;
    private boolean active;
    private Date dateCreation;

    // Constructeurs
    public User() {}

    public User(String nom, String email, String MDP, Role_enum role) {
        this.nom = nom;
        this.email = email;
        this.MDP = MDP;
        this.role = role;
        this.active = true;
        this.dateCreation = new Date();
    }

    // Constructeur avec String pour rôle (conversion automatique)
    public User(String nom, String email, String MDP, String role) {
        this(nom, email, MDP, Role_enum.fromString(role));
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getNom() { return nom; }
    public void setNom(String nom) { this.nom = nom; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getMDP() { return MDP; }
    public void setMDP(String MDP) { this.MDP = MDP; }

    public Role_enum getRole() { return role; }
    public void setRole(Role_enum role) { this.role = role; }

    // Setter avec String (conversion automatique)
    public void setRole(String role) {
        this.role = Role_enum.fromString(role);
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", active=" + active +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id && active == user.active && Objects.equals(nom, user.nom) && Objects.equals(email, user.email) && Objects.equals(MDP, user.MDP) && role == user.role && Objects.equals(dateCreation, user.dateCreation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nom, email, MDP, role, active, dateCreation);
    }
}