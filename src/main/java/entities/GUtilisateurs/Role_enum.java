package entities;

public enum Role_enum {
    ADMIN,
    INVESTISSEUR,
    STARTUP;

    // Méthode pour convertir String en Role_enum (sécurisée)
    public static Role_enum fromString(String role) {
        if (role == null) {
            throw new IllegalArgumentException("Le rôle ne peut pas être null");
        }

        try {
            return Role_enum.valueOf(role.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Rôle invalide: " + role +
                    ". Rôles valides: ADMIN, INVESTISSEUR, STARTUP");
        }
    }

    // Méthode pour vérifier si une chaîne est un rôle valide
    public static boolean isValidRole(String role) {
        if (role == null) return false;

        for (Role_enum r : Role_enum.values()) {
            if (r.name().equalsIgnoreCase(role)) {
                return true;
            }
        }
        return false;
    }
}