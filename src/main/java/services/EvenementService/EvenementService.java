package services.EvenementService;

import entities.GEvenement.Evenement;
import services.IService;
import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

public class EvenementService implements IService<Evenement> {

    private Connection connection;

    public EvenementService() {
        connection = MyDatabase.getInstance().getConnection();
    }

    private Connection conn() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = MyDatabase.getInstance().getConnection();
            }
        } catch (SQLException ignored) {
            connection = MyDatabase.getInstance().getConnection();
        }
        return connection;
    }

    @Override
    public void ajouter(Evenement evenement) throws SQLException {
        Connection c = conn();
        // Tentative avec colonne image (nouveau schéma)
        String reqWithImage = "INSERT INTO evenement (titre, type, date_evenement, lieu, description, capacite_max, image) " +
                "VALUES (?, ?, ?, ?, ?, ?, ?)";

        try (PreparedStatement ps = c.prepareStatement(reqWithImage)) {
            ps.setString(1, evenement.getTitre());
            ps.setString(2, evenement.getType());
            ps.setDate(3, evenement.getDateEvenement());
            ps.setString(4, evenement.getLieu());
            ps.setString(5, evenement.getDescription());
            ps.setInt(6, evenement.getCapaciteMax());
            ps.setString(7, evenement.getImage());
            ps.executeUpdate();
        } catch (SQLException ex) {
            // Compatibilité avec anciens schémas sans colonne image
            if (!isUnknownImageColumnError(ex)) {
                throw ex;
            }
            String reqLegacy = "INSERT INTO evenement (titre, type, date_evenement, lieu, description, capacite_max) " +
                    "VALUES (?, ?, ?, ?, ?, ?)";
            try (PreparedStatement ps = c.prepareStatement(reqLegacy)) {
                ps.setString(1, evenement.getTitre());
                ps.setString(2, evenement.getType());
                ps.setDate(3, evenement.getDateEvenement());
                ps.setString(4, evenement.getLieu());
                ps.setString(5, evenement.getDescription());
                ps.setInt(6, evenement.getCapaciteMax());
                ps.executeUpdate();
            }
        }
    }

    /**
     * Suppression "soft" : on archive au lieu de supprimer.
     * Objectif: l'événement disparaît du dashboard mais reste visible (grisé) côté front.
     */
    @Override
    public void supprimer(int id) throws SQLException {
        Connection c = conn();

        // On tente d'archiver (si colonne existe). Si la colonne n'existe pas => fallback DELETE.
        try {
            String req = "UPDATE evenement SET archived = 1 WHERE id_evenement = ?";
            PreparedStatement ps = c.prepareStatement(req);
            ps.setInt(1, id);
            ps.executeUpdate();
        } catch (SQLException ex) {
            // fallback: ancien comportement
            String req = "DELETE FROM evenement WHERE id_evenement = ?";
            PreparedStatement ps = c.prepareStatement(req);
            ps.setInt(1, id);
            ps.executeUpdate();
        }
    }

    @Override
    public void update(Evenement evenement) throws SQLException {
        Connection c = conn();
        String reqWithImage = "UPDATE evenement SET titre = ?, type = ?, date_evenement = ?, " +
                "lieu = ?, description = ?, capacite_max = ?, image = ? WHERE id_evenement = ?";

        try (PreparedStatement ps = c.prepareStatement(reqWithImage)) {
            ps.setString(1, evenement.getTitre());
            ps.setString(2, evenement.getType());
            ps.setDate(3, evenement.getDateEvenement());
            ps.setString(4, evenement.getLieu());
            ps.setString(5, evenement.getDescription());
            ps.setInt(6, evenement.getCapaciteMax());
            ps.setString(7, evenement.getImage());
            ps.setInt(8, evenement.getId());
            ps.executeUpdate();
        } catch (SQLException ex) {
            // Compatibilité avec anciens schémas sans colonne image
            if (!isUnknownImageColumnError(ex)) {
                throw ex;
            }
            String reqLegacy = "UPDATE evenement SET titre = ?, type = ?, date_evenement = ?, " +
                    "lieu = ?, description = ?, capacite_max = ? WHERE id_evenement = ?";
            try (PreparedStatement ps = c.prepareStatement(reqLegacy)) {
                ps.setString(1, evenement.getTitre());
                ps.setString(2, evenement.getType());
                ps.setDate(3, evenement.getDateEvenement());
                ps.setString(4, evenement.getLieu());
                ps.setString(5, evenement.getDescription());
                ps.setInt(6, evenement.getCapaciteMax());
                ps.setInt(7, evenement.getId());
                ps.executeUpdate();
            }
        }
    }

    @Override
    public List<Evenement> read() throws SQLException {
        Connection c = conn();
        List<Evenement> evenements = new ArrayList<>();
        String req = "SELECT * FROM evenement";

        Statement st = c.createStatement();
        ResultSet rs = st.executeQuery(req);

        while (rs.next()) {
            Evenement e = new Evenement();
            e.setId(rs.getInt("id_evenement"));
            e.setTitre(rs.getString("titre"));
            e.setType(rs.getString("type"));
            e.setDateEvenement(rs.getDate("date_evenement"));
            e.setLieu(rs.getString("lieu"));
            e.setDescription(rs.getString("description"));
            e.setCapaciteMax(rs.getInt("capacite_max"));

            try {
                e.setImage(rs.getString("image"));
            } catch (SQLException ignored) {
            }

            // colonne archived (optionnelle)
            try {
                e.setArchived(rs.getInt("archived") == 1);
            } catch (SQLException ignored) {
                e.setArchived(false);
            }

            evenements.add(e);
        }
        return evenements;
    }

    /**
     * Détecte l'erreur MySQL/MariaDB "Unknown column 'image' in 'field list'"
     * pour pouvoir basculer sur une requête compatible avec l'ancien schéma.
     */
    private boolean isUnknownImageColumnError(SQLException ex) {
        String msg = ex.getMessage();
        if (msg == null) return false;
        msg = msg.toLowerCase();
        return msg.contains("unknown column") && msg.contains("image");
    }

    /**
     * Lecture des événements non archivés (dashboard).
     */
    public List<Evenement> readActifs() throws SQLException {
        Connection c = conn();
        List<Evenement> evenements = new ArrayList<>();

        // Si la colonne n'existe pas, on retombe sur read().
        String req = "SELECT * FROM evenement WHERE archived = 0";
        try (Statement st = c.createStatement(); ResultSet rs = st.executeQuery(req)) {
            while (rs.next()) {
                Evenement e = new Evenement();
                e.setId(rs.getInt("id_evenement"));
                e.setTitre(rs.getString("titre"));
                e.setType(rs.getString("type"));
                e.setDateEvenement(rs.getDate("date_evenement"));
                e.setLieu(rs.getString("lieu"));
                e.setDescription(rs.getString("description"));
                e.setCapaciteMax(rs.getInt("capacite_max"));
                try { e.setImage(rs.getString("image")); } catch (SQLException ignored) {}
                evenements.add(e);
            }
        } catch (SQLException ex) {
            // colonne pas encore ajoutée
            return read();
        }

        return evenements;
    }

    /**
     * Vérifie si un événement est archivé.
     */
    public boolean isArchived(int id) {
        try {
            Connection c = conn();
            String req = "SELECT archived FROM evenement WHERE id_evenement = ?";
            try (PreparedStatement ps = c.prepareStatement(req)) {
                ps.setInt(1, id);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) return false;
                    return rs.getInt(1) == 1;
                }
            }
        } catch (SQLException ex) {
            return false;
        }
    }

    /**
     * Returns events whose date is exactly 2 days from today (for reminder notifications).
     * Uses active (non-archived) events only.
     */
    public List<Evenement> getEventsInExactlyTwoDays() {
        LocalDate today = LocalDate.now();
        LocalDate inTwoDays = today.plusDays(2);

        System.out.println("\n📊 DEBUG getEventsInExactlyTwoDays:");
        System.out.println("  Aujourd'hui : " + today);
        System.out.println("  Dans 2 jours : " + inTwoDays);

        List<Evenement> result = new ArrayList<>();
        try {
            List<Evenement> all = readActifs();
            System.out.println("  Nombre total d'événements actifs : " + all.size());

            for (Evenement e : all) {
                if (e.getDateEvenement() != null) {
                    LocalDate eventDate = e.getDateEvenement().toLocalDate();
                    System.out.println("  - " + e.getTitre() + " : " + eventDate + " (égal ? " + eventDate.equals(inTwoDays) + ")");

                    if (eventDate.equals(inTwoDays)) {
                        result.add(e);
                        System.out.println("    ✅ AJOUTÉ aux notifications !");
                    }
                }
            }

            System.out.println("  Total événements dans 2 jours : " + result.size() + "\n");

        } catch (SQLException ex) {
            System.err.println("❌ Erreur SQL dans getEventsInExactlyTwoDays : " + ex.getMessage());
            ex.printStackTrace();
        }
        return result;
    }
}