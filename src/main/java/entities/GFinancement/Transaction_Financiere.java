package entities.GFinancement;

import java.util.Objects;

public class Transaction_Financiere {
    private int id_transaction;
    private double montantTransaction;
    private String date_transaction;
    private String mode_paiement;
    private String statut_transaction;
    private int id_investissement;

    public Transaction_Financiere(int id_transaction, double montantTransaction, String date_transaction, String mode_paiement, String statut_transaction, int id_investissement) {
        this.id_transaction = id_transaction;
        this.montantTransaction = montantTransaction;
        this.date_transaction = date_transaction;
        this.mode_paiement = mode_paiement;
        this.statut_transaction = statut_transaction;
        this.id_investissement = id_investissement;
    }

    public int getId_transaction() {
        return id_transaction;
    }

    public void setId_transaction(int id_transaction) {
        this.id_transaction = id_transaction;
    }

    public double getMontantTransaction() {
        return montantTransaction;
    }

    public void setMontantTransaction(double montantTransaction) {
        this.montantTransaction = montantTransaction;
    }

    public String getDate_transaction() {
        return date_transaction;
    }

    public void setDate_transaction(String date_transaction) {
        this.date_transaction = date_transaction;
    }

    public String getMode_paiement() {
        return mode_paiement;
    }

    public void setMode_paiement(String mode_paiement) {
        this.mode_paiement = mode_paiement;
    }

    public String getStatut_transaction() {
        return statut_transaction;
    }

    public void setStatut_transaction(String statut_transaction) {
        this.statut_transaction = statut_transaction;
    }

    public int getId_investissement() {
        return id_investissement;
    }

    public void setId_investissement(int id_investissement) {
        this.id_investissement = id_investissement;
    }

    @Override
    public String toString() {
        return "Transaction_Financiere{" +
                "id_transaction=" + id_transaction +
                ", montantTransaction=" + montantTransaction +
                ", date_transaction='" + date_transaction + '\'' +
                ", mode_paiement='" + mode_paiement + '\'' +
                ", statut_transaction='" + statut_transaction + '\'' +
                ", id_investissement=" + id_investissement +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Transaction_Financiere that = (Transaction_Financiere) o;
        return id_transaction == that.id_transaction && Double.compare(montantTransaction, that.montantTransaction) == 0 && id_investissement == that.id_investissement && Objects.equals(date_transaction, that.date_transaction) && Objects.equals(mode_paiement, that.mode_paiement) && Objects.equals(statut_transaction, that.statut_transaction);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id_transaction, montantTransaction, date_transaction, mode_paiement, statut_transaction, id_investissement);
    }
}
