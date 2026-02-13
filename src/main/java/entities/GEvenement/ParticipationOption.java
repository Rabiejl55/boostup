package entities.GEvenement;

/**
 * Option affichée dans la ChoiceBox des Feedbacks.
 * On affiche un libellé lisible (événement + investisseur), tout en gardant l'id_participation.
 */
public class ParticipationOption {
    private final int idParticipation;
    private final String label;

    public ParticipationOption(int idParticipation, String label) {
        this.idParticipation = idParticipation;
        this.label = label;
    }

    public int getIdParticipation() {
        return idParticipation;
    }

    public String getLabel() {
        return label;
    }

    @Override
    public String toString() {
        return label;
    }
}

