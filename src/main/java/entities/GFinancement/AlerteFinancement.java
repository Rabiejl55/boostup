package entities.GFinancement;

public class AlerteFinancement {
    private final String type;     // ex: OBJECTIF_ATTEINT, RISQUE, PAIEMENT_EN_ATTENTE
    private final String niveau;   // INFO, WARN, CRITICAL
    private final String message;

    public AlerteFinancement(String type, String niveau, String message) {
        this.type = type;
        this.niveau = niveau;
        this.message = message;
    }

    public String getType() { return type; }
    public String getNiveau() { return niveau; }
    public String getMessage() { return message; }

    @Override
    public String toString() {
        return "[" + niveau + "] " + message;
    }
}