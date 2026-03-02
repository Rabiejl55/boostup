package entities.GFinancement;

import java.util.Objects;

public class Investissement {
    private int id_investissement;
    private double montantInvestissement;
    private String statut;
    private String date_investissement;
    private int id_projet;
    private int id_user;

    public Investissement() {}

    public Investissement(int id_investissement, double montantInvestissement, String statut, String date_investissement, int id_projet, int id_user) {
        this.id_investissement = id_investissement;
        this.montantInvestissement = montantInvestissement;
        this.statut = statut;
        this.date_investissement = date_investissement;
        this.id_projet = id_projet;
        this.id_user = id_user;
    }

    public int getId_investissement() {
        return id_investissement;
    }

    public void setId_investissement(int id_investissement) {
        this.id_investissement = id_investissement;
    }

    public double getMontantInvestissement() {
        return montantInvestissement;
    }

    public void setMontantInvestissement(double montant) {
        this.montantInvestissement = montant;
    }

    public String getStatut() {
        return statut;
    }

    public void setStatut(String statut) {
        this.statut = statut;
    }

    public String getDate_investissement() {
        return date_investissement;
    }

    public void setDate_investissement(String date_investissement) {
        this.date_investissement = date_investissement;
    }

    public int getId_projet() {
        return id_projet;
    }

    public void setId_projet(int id_projet) {
        this.id_projet = id_projet;
    }

    public int getId_user() {
        return id_user;
    }

    public void setId_user(int id_user) {
        this.id_user = id_user;
    }

    @Override
    public String toString() {
        return "Investissement{" +
                "id_investissement=" + id_investissement +
                ", montantInvestissement=" + montantInvestissement +
                ", statut='" + statut + '\'' +
                ", date_investissement='" + date_investissement + '\'' +
                ", id_projet=" + id_projet +
                ", id_user=" + id_user +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Investissement that = (Investissement) o;
        return id_investissement == that.id_investissement && Double.compare(montantInvestissement, that.montantInvestissement) == 0 && id_projet == that.id_projet && id_user == that.id_user && Objects.equals(statut, that.statut) && Objects.equals(date_investissement, that.date_investissement);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id_investissement, montantInvestissement, statut, date_investissement, id_projet, id_user);
    }
}
