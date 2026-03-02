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

    // Profile fields
    private String fullname;
    private String phone;
    private String avatar;

    // Constructeurs
    public User() {
        this.dateCreation = new Date();
        this.active = true;
    }

    public User(String nom, String email, String MDP, Role_enum role) {
        this();
        this.nom = nom;
        this.email = email;
        this.MDP = MDP;
        this.role = role;
    }

    // Constructor with all fields
    public User(String nom, String email, String MDP, Role_enum role,
                String fullname, String phone, String avatar) {
        this(nom, email, MDP, role);
        this.fullname = fullname;
        this.phone = phone;
        this.avatar = avatar;
    }

    // Constructeur avec String pour rôle
    public User(String nom, String email, String MDP, String role) {
        this(nom, email, MDP, Role_enum.fromString(role));
    }

    // Getters et Setters for core User fields
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

    public void setRole(String role) {
        this.role = Role_enum.fromString(role);
    }

    public boolean isActive() { return active; }
    public void setActive(boolean active) { this.active = active; }

    public Date getDateCreation() { return dateCreation; }
    public void setDateCreation(Date dateCreation) { this.dateCreation = dateCreation; }

    // Getters et Setters for profile fields
    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    // Convenience methods
    public void updateProfile(String fullname, String phone, String avatar) {
        this.fullname = fullname;
        this.phone = phone;
        this.avatar = avatar;
    }

    // Helper to get display name (prioritize fullname, fallback to nom)
    public String getDisplayName() {
        return fullname != null && !fullname.trim().isEmpty() ? fullname : nom;
    }

    @Override
    public String toString() {
        return "User{" +
                "id=" + id +
                ", nom='" + nom + '\'' +
                ", email='" + email + '\'' +
                ", role=" + role +
                ", active=" + active +
                ", fullname='" + fullname + '\'' +
                ", phone='" + phone + '\'' +
                ", avatar='" + avatar + '\'' +
                ", dateCreation=" + dateCreation +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        User user = (User) o;
        return id == user.id &&
                active == user.active &&
                Objects.equals(nom, user.nom) &&
                Objects.equals(email, user.email) &&
                Objects.equals(MDP, user.MDP) &&
                role == user.role &&
                Objects.equals(fullname, user.fullname) &&
                Objects.equals(phone, user.phone) &&
                Objects.equals(avatar, user.avatar) &&
                Objects.equals(dateCreation, user.dateCreation);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, nom, email, MDP, role, active, fullname, phone, avatar, dateCreation);
    }
}