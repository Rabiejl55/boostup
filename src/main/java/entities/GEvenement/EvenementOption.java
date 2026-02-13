package entities.GEvenement;

/**
 * Petit DTO pour représenter un événement dans une ChoiceBox.
 * On affiche le titre (toString), mais on garde l'id pour l'enregistrement.
 */
public class EvenementOption {
    private final int id;
    private final String titre;

    public EvenementOption(int id, String titre) {
        this.id = id;
        this.titre = titre;
    }

    public int getId() {
        return id;
    }

    public String getTitre() {
        return titre;
    }

    @Override
    public String toString() {
        return titre;
    }
}

