package entities;

/**
 * Énumération des rôles utilisateurs dans BoostUp
 */
public enum Role_enum {
    ADMIN("Administrateur"),
    USER("Utilisateur"),
    INVESTISSEUR("Investisseur"),
    STARTUP("Startup");

    private final String displayName;

    Role_enum(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String toString() {
        return displayName;
    }
}

