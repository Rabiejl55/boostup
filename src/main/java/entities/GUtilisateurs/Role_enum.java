package entities.GUtilisateurs;

import java.util.Objects;

public class Role_enum {
    private String role_name;
    public String getRole_name() {
        return role_name;
    }
    public void setRole_name(String role_name) {
        this.role_name = role_name;
    }
    public Role_enum(String role_name)
    {
        this.role_name = role_name;
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Role_enum roleEnum = (Role_enum) o;
        return Objects.equals(role_name, roleEnum.role_name);
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(role_name);
    }

    @Override
    public String toString() {
        return "role_enum{" +
                "role_name='" + role_name + '\'' +
                '}';
    }
}
