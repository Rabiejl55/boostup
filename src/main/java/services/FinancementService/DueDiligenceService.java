package services.FinancementService;

import utils.MyDatabase;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class DueDiligenceService {

    public enum DocType {
        PITCH_DECK, BUSINESS_PLAN, KBIS, FINANCIALS, OTHER
    }

    public static class ProjetDocument {
        public int idDoc;
        public int idProjet;
        public String docType;
        public String fileName;
        public String filePath;
        public Timestamp uploadedAt;
        public Integer uploadedBy;
    }

    public static class CheckItem {
        public int idItem;
        public int idProjet;
        public String itemKey;
        public String label;
        public boolean done;
        public Timestamp updatedAt;
        public Integer updatedBy;
    }

    // ====== Documents ======

    public List<ProjetDocument> getDocuments(int idProjet) throws Exception {
        String sql = """
                SELECT id_doc, id_projet, doc_type, file_name, file_path, uploaded_at, uploaded_by
                FROM projet_document
                WHERE id_projet=?
                ORDER BY uploaded_at DESC
                """;
        List<ProjetDocument> out = new ArrayList<>();

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    ProjetDocument d = new ProjetDocument();
                    d.idDoc = rs.getInt("id_doc");
                    d.idProjet = rs.getInt("id_projet");
                    d.docType = rs.getString("doc_type");
                    d.fileName = rs.getString("file_name");
                    d.filePath = rs.getString("file_path");
                    d.uploadedAt = rs.getTimestamp("uploaded_at");
                    int up = rs.getInt("uploaded_by");
                    d.uploadedBy = rs.wasNull() ? null : up;
                    out.add(d);
                }
            }
        }
        return out;
    }

    public void addDocument(int idProjet, DocType type, String fileName, String filePath, Integer uploadedBy) throws Exception {
        String sql = """
                INSERT INTO projet_document (id_projet, doc_type, file_name, file_path, uploaded_by)
                VALUES (?,?,?,?,?)
                """;
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            ps.setString(2, type.name());
            ps.setString(3, fileName);
            ps.setString(4, filePath);
            if (uploadedBy == null) ps.setNull(5, Types.INTEGER);
            else ps.setInt(5, uploadedBy);
            ps.executeUpdate();
        }
    }

    public void deleteDocument(int idDoc) throws Exception {
        String sql = "DELETE FROM projet_document WHERE id_doc=?";
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idDoc);
            ps.executeUpdate();
        }
    }

    // ====== Checklist ======

    /** Initialise une checklist standard si elle n’existe pas (idempotent). */
    public void ensureDefaultChecklist(int idProjet) throws Exception {
        List<CheckItem> existing = getChecklist(idProjet);
        if (!existing.isEmpty()) return;

        addChecklistItem(idProjet, "DECK_OK", "Pitch deck OK", false, null);
        addChecklistItem(idProjet, "BUSINESS_MODEL_OK", "Business model clair", false, null);
        addChecklistItem(idProjet, "TRACTION_OK", "Traction / marché validé", false, null);
        addChecklistItem(idProjet, "FINANCIALS_OK", "Financiers cohérents", false, null);
        addChecklistItem(idProjet, "LEGAL_OK", "Documents légaux OK", false, null);
    }

    public List<CheckItem> getChecklist(int idProjet) throws Exception {
        String sql = """
                SELECT id_item, id_projet, item_key, label, is_done, updated_at, updated_by
                FROM projet_check_item
                WHERE id_projet=?
                ORDER BY id_item ASC
                """;
        List<CheckItem> out = new ArrayList<>();

        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    CheckItem it = new CheckItem();
                    it.idItem = rs.getInt("id_item");
                    it.idProjet = rs.getInt("id_projet");
                    it.itemKey = rs.getString("item_key");
                    it.label = rs.getString("label");
                    it.done = rs.getInt("is_done") == 1;
                    it.updatedAt = rs.getTimestamp("updated_at");
                    int u = rs.getInt("updated_by");
                    it.updatedBy = rs.wasNull() ? null : u;
                    out.add(it);
                }
            }
        }
        return out;
    }

    private void addChecklistItem(int idProjet, String key, String label, boolean done, Integer updatedBy) throws Exception {
        String sql = """
                INSERT INTO projet_check_item (id_projet, item_key, label, is_done, updated_by)
                VALUES (?,?,?,?,?)
                """;
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            ps.setString(2, key);
            ps.setString(3, label);
            ps.setInt(4, done ? 1 : 0);
            if (updatedBy == null) ps.setNull(5, Types.INTEGER);
            else ps.setInt(5, updatedBy);
            ps.executeUpdate();
        }
    }

    public void setChecklistDone(int idItem, boolean done, Integer updatedBy) throws Exception {
        String sql = """
                UPDATE projet_check_item
                SET is_done=?, updated_by=?, updated_at=?
                WHERE id_item=?
                """;
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, done ? 1 : 0);
            if (updatedBy == null) ps.setNull(2, Types.INTEGER);
            else ps.setInt(2, updatedBy);
            ps.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            ps.setInt(4, idItem);
            ps.executeUpdate();
        }
    }

    // ====== Score maturité ======
    /** Score (0-100) basé sur checklist + présence des docs clés. */
    public int computeMaturityScore(int idProjet) throws Exception {
        ensureDefaultChecklist(idProjet);

        List<CheckItem> items = getChecklist(idProjet);
        List<ProjetDocument> docs = getDocuments(idProjet);

        int checklistPoints = 0;
        int totalChecklist = items.size();

        int done = 0;
        for (CheckItem it : items) if (it.done) done++;

        // checklist = 70 points
        if (totalChecklist > 0) {
            checklistPoints = (int) Math.round((done * 70.0) / totalChecklist);
        }

        // docs = 30 points (docs clés)
        boolean hasDeck = docs.stream().anyMatch(d -> "PITCH_DECK".equalsIgnoreCase(d.docType));
        boolean hasBP = docs.stream().anyMatch(d -> "BUSINESS_PLAN".equalsIgnoreCase(d.docType));
        boolean hasFin = docs.stream().anyMatch(d -> "FINANCIALS".equalsIgnoreCase(d.docType));

        int docsPoints = 0;
        if (hasDeck) docsPoints += 10;
        if (hasBP) docsPoints += 10;
        if (hasFin) docsPoints += 10;

        return Math.min(100, checklistPoints + docsPoints);
    }
}