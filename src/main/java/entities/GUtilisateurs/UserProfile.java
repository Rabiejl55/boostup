package entities.GUtilisateurs;

import java.util.Objects;

public class UserProfile {
    private int id;
    private User user; // Association avec User
    private String fullname;
    private String phone;
    private String avatar;

    // Constructeurs
    public UserProfile() {}

    public UserProfile(User user, String fullname, String phone, String avatar) {
        this.user = user;
        this.fullname = fullname;
        this.phone = phone;
        this.avatar = avatar;
    }

    // Getters et Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getFullname() { return fullname; }
    public void setFullname(String fullname) { this.fullname = fullname; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }

    @Override
    public String toString() {
        return "UserProfile{" +
                "id=" + id +
                ", user=" + (user != null ? user.getId() + " - " + user.getNom() : "null") +
                ", fullname='" + fullname + '\'' +
                ", phone='" + phone + '\'' +
                ", avatar='" + avatar + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        UserProfile that = (UserProfile) o;
        return id == that.id && Objects.equals(user, that.user) && Objects.equals(fullname, that.fullname) && Objects.equals(phone, that.phone) && Objects.equals(avatar, that.avatar);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, user, fullname, phone, avatar);
    }
}