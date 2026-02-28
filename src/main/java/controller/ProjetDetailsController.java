package controller;

import javafx.animation.PauseTransition;
import javafx.application.HostServices;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;
import javafx.util.Duration;
import services.FinancementService.DueDiligenceService;
import services.FinancementService.DueDiligenceService.CheckItem;
import services.FinancementService.DueDiligenceService.ProjetDocument;
import utils.MyDatabase;

import java.awt.Desktop;
import java.io.File;
import java.sql.*;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Locale;

public class ProjetDetailsController {

    // Header
    @FXML private Label lblTitle;
    @FXML private Label lblSub;
    @FXML private Label lblDesc;

    @FXML private ProgressBar pbMaturity;
    @FXML private Label lblMaturity;

    @FXML private Label lblToast;

    // KPI
    @FXML private Label lblBudget;
    @FXML private Label lblLeve;
    @FXML private Label lblReste;

    // Documents
    @FXML private ComboBox<String> cbDocType;
    @FXML private TableView<ProjetDocument> tableDocs;
    @FXML private TableColumn<ProjetDocument, String> colDocType;
    @FXML private TableColumn<ProjetDocument, String> colDocName;
    @FXML private TableColumn<ProjetDocument, String> colDocDate;
    @FXML private TableColumn<ProjetDocument, ProjetDocument> colDocActions;
    @FXML private Label lblDocsHint;

    // Checklist
    @FXML private ListView<CheckItem> lvChecklist;

    // Historique investissements
    @FXML private TableView<InvRow> tableInv;
    @FXML private TableColumn<InvRow, String> colInvDate;
    @FXML private TableColumn<InvRow, Double> colInvMontant;
    @FXML private TableColumn<InvRow, String> colInvStatut;
    @FXML private TableColumn<InvRow, String> colInvUser;

    private final DueDiligenceService dd = new DueDiligenceService();
    private final DecimalFormat money = new DecimalFormat("#,##0.00");

    private int idProjet;
    private int currentUserId;
    private Runnable onBack;
    private HostServices hostServices;
    public void setHostServices(HostServices hs) { this.hostServices = hs; }

    /** ✅ appelé par l’écran parent avant affichage */
    /** Appelé par l’écran parent avant affichage */
    public void init(int idProjet, int currentUserId, Runnable onBack) {
        this.idProjet = idProjet;
        // si tu veux vraiment utiliser le user passé, remplace CURRENT_USER_ID final par une variable
        this.onBack = onBack;
        reloadAllAsync();
    }

    @FXML
    public void initialize() {
        if (cbDocType != null) {
            cbDocType.setItems(FXCollections.observableArrayList(
                    "PITCH_DECK", "BUSINESS_PLAN", "KBIS", "FINANCIALS", "OTHER"
            ));
            cbDocType.getSelectionModel().selectFirst();
        }

        setupDocsTable();
        setupChecklistCell();
        setupInvTable();
    }

    // ========================= UI actions =========================

    @FXML
    private void goBack() {
        if (onBack != null) onBack.run();
    }

    @FXML
    private void uploadDocument() {
        if (cbDocType == null) return;

        FileChooser fc = new FileChooser();
        fc.setTitle("Choisir un document");
        fc.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("PDF", "*.pdf"),
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"),
                new FileChooser.ExtensionFilter("Documents", "*.doc", "*.docx", "*.ppt", "*.pptx", "*.xls", "*.xlsx"),
                new FileChooser.ExtensionFilter("Tous fichiers", "*.*")
        );

        File f = fc.showOpenDialog(cbDocType.getScene().getWindow());
        if (f == null) return;

        String type = cbDocType.getValue() == null ? "OTHER" : cbDocType.getValue();

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                dd.addDocument(idProjet, DueDiligenceService.DocType.valueOf(type),
                        f.getName(), f.getAbsolutePath(), currentUserId);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            toast("✅ Document ajouté", "toastSuccess");
            reloadDocsAndScoreAsync();
        });
        task.setOnFailed(e -> toast("❌ " + safeErr(task.getException()), "toastError"));

        new Thread(task, "dd-upload").start();
    }

    // ========================= Setup components =========================

    private void setupDocsTable() {
        if (tableDocs == null) return;

        colDocType.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().docType));
        colDocName.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().fileName));
        colDocDate.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(
                c.getValue().uploadedAt == null ? "" : c.getValue().uploadedAt.toString()
        ));

        colDocActions.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        colDocActions.setCellFactory(col -> new TableCell<>() {
            @Override protected void updateItem(ProjetDocument item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); return; }

                Button open = new Button("Ouvrir");
                open.getStyleClass().add("secondary-action");

                Button del = new Button("Supprimer");
                del.getStyleClass().add("danger-action");

                open.setOnAction(e -> openFile(item.filePath));
                del.setOnAction(e -> deleteDocAsync(item.idDoc));

                setGraphic(new HBox(8, open, del));
            }
        });

        tableDocs.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    private void setupChecklistCell() {
        if (lvChecklist == null) return;

        lvChecklist.setCellFactory(lv -> new ListCell<>() {
            @Override protected void updateItem(CheckItem item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) { setGraphic(null); setText(null); return; }

                CheckBox cb = new CheckBox(item.label);
                cb.setSelected(item.done);
                cb.selectedProperty().addListener((obs, old, val) -> setChecklistDoneAsync(item.idItem, val));
                setGraphic(cb);
            }
        });
    }

    private void setupInvTable() {
        if (tableInv == null) return;

        colInvDate.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().date));
        colInvMontant.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().montant));
        colInvStatut.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().statut));
        colInvUser.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue().userLabel));

        tableInv.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY);
    }

    // ========================= Loaders =========================

    private void reloadAllAsync() {
        toast("Loading…", "toastInfo");

        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                dd.ensureDefaultChecklist(idProjet);
                return null;
            }
        };

        task.setOnSucceeded(e -> {
            reloadProjectInfoAsync();
            reloadDocsAndScoreAsync();
            reloadChecklistAsync();
            reloadInvestHistoryAsync();
        });

        task.setOnFailed(e -> toast("❌ " + safeErr(task.getException()), "toastError"));

        new Thread(task, "dd-init").start();
    }

    private void reloadProjectInfoAsync() {
        Task<ProjectRow> task = new Task<>() {
            @Override protected ProjectRow call() throws Exception {
                return fetchProjet(idProjet);
            }
        };
        task.setOnSucceeded(e -> {
            ProjectRow p = task.getValue();
            if (p == null) return;

            if (lblTitle != null) lblTitle.setText("PROJ-" + p.id + " • " + (p.titre == null ? "Projet" : p.titre));
            if (lblSub != null) lblSub.setText((p.statut == null ? "—" : p.statut.toUpperCase(Locale.ROOT))
                    + " • Budget " + money.format(p.budget) + " DT");
            if (lblDesc != null) lblDesc.setText(p.description == null ? "" : p.description);

            loadKpiAsync();
        });
        task.setOnFailed(e -> toast("❌ " + safeErr(task.getException()), "toastError"));
        new Thread(task, "dd-projet").start();
    }

    private void loadKpiAsync() {
        Task<KpiRow> task = new Task<>() {
            @Override protected KpiRow call() throws Exception {
                return fetchKpis(idProjet);
            }
        };
        task.setOnSucceeded(e -> {
            KpiRow k = task.getValue();
            if (lblBudget != null) lblBudget.setText(money.format(k.budget) + " DT");
            if (lblLeve != null) lblLeve.setText(money.format(k.totalLeve) + " DT");
            if (lblReste != null) lblReste.setText(money.format(Math.max(0, k.budget - k.totalLeve)) + " DT");
        });
        task.setOnFailed(e -> toast("❌ " + safeErr(task.getException()), "toastError"));
        new Thread(task, "dd-kpi").start();
    }

    private void reloadDocsAndScoreAsync() {
        Task<DocsScore> task = new Task<>() {
            @Override protected DocsScore call() throws Exception {
                List<ProjetDocument> docs = dd.getDocuments(idProjet);
                int score = dd.computeMaturityScore(idProjet);
                DocsScore ds = new DocsScore();
                ds.docs = docs;
                ds.score = score;
                return ds;
            }
        };

        task.setOnSucceeded(e -> {
            DocsScore ds = task.getValue();
            if (tableDocs != null) tableDocs.getItems().setAll(ds.docs);

            if (pbMaturity != null) pbMaturity.setProgress(ds.score / 100.0);
            if (lblMaturity != null) lblMaturity.setText(ds.score + "/100");

            toast("", "toastInfo");
        });

        task.setOnFailed(e -> toast("❌ " + safeErr(task.getException()), "toastError"));
        new Thread(task, "dd-docs-score").start();
    }

    private void reloadChecklistAsync() {
        Task<List<CheckItem>> task = new Task<>() {
            @Override protected List<CheckItem> call() throws Exception {
                return dd.getChecklist(idProjet);
            }
        };
        task.setOnSucceeded(e -> {
            if (lvChecklist != null) lvChecklist.getItems().setAll(task.getValue());
        });
        task.setOnFailed(e -> toast("❌ " + safeErr(task.getException()), "toastError"));
        new Thread(task, "dd-checklist").start();
    }

    private void reloadInvestHistoryAsync() {
        Task<List<InvRow>> task = new Task<>() {
            @Override protected List<InvRow> call() throws Exception {
                return fetchInvestissements(idProjet);
            }
        };
        task.setOnSucceeded(e -> {
            if (tableInv != null) tableInv.getItems().setAll(task.getValue());
        });
        task.setOnFailed(e -> toast("❌ " + safeErr(task.getException()), "toastError"));
        new Thread(task, "dd-history").start();
    }

    // ========================= Updates =========================

    private void setChecklistDoneAsync(int idItem, boolean done) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                dd.setChecklistDone(idItem, done, currentUserId);
                return null;
            }
        };
        task.setOnSucceeded(e -> reloadDocsAndScoreAsync());
        task.setOnFailed(e -> toast("❌ " + safeErr(task.getException()), "toastError"));
        new Thread(task, "dd-check-update").start();
    }

    private void deleteDocAsync(int idDoc) {
        Task<Void> task = new Task<>() {
            @Override protected Void call() throws Exception {
                dd.deleteDocument(idDoc);
                return null;
            }
        };
        task.setOnSucceeded(e -> {
            toast("✅ Document supprimé", "toastSuccess");
            reloadDocsAndScoreAsync();
        });
        task.setOnFailed(e -> toast("❌ " + safeErr(task.getException()), "toastError"));
        new Thread(task, "dd-doc-del").start();
    }

    // ========================= DB helpers =========================

    private ProjectRow fetchProjet(int idProjet) throws Exception {
        String sql = "SELECT id_projet, titre, description, budget, statut FROM projet WHERE id_projet=?";
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                if (!rs.next()) return null;
                ProjectRow p = new ProjectRow();
                p.id = rs.getInt("id_projet");
                p.titre = rs.getString("titre");
                p.description = rs.getString("description");
                p.budget = rs.getDouble("budget");
                p.statut = rs.getString("statut");
                return p;
            }
        }
    }

    private KpiRow fetchKpis(int idProjet) throws Exception {
        KpiRow k = new KpiRow();

        String sqlBudget = "SELECT budget FROM projet WHERE id_projet=?";
        String sqlLeve =
                "SELECT COALESCE(SUM(montantInvestissement),0) total " +
                        "FROM investissement WHERE id_projet=? AND UPPER(statut)='FINANCE'";

        try (Connection c = MyDatabase.getInstance().getConnection()) {
            try (PreparedStatement ps = c.prepareStatement(sqlBudget)) {
                ps.setInt(1, idProjet);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    k.budget = rs.getDouble("budget");
                }
            }
            try (PreparedStatement ps = c.prepareStatement(sqlLeve)) {
                ps.setInt(1, idProjet);
                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    k.totalLeve = rs.getDouble("total");
                }
            }
        }
        return k;
    }

    private List<InvRow> fetchInvestissements(int idProjet) throws Exception {
        String sql =
                "SELECT date_investissement, montantInvestissement, statut, id_user " +
                        "FROM investissement WHERE id_projet=? ORDER BY date_investissement DESC";

        var out = FXCollections.<InvRow>observableArrayList();
        try (Connection c = MyDatabase.getInstance().getConnection();
             PreparedStatement ps = c.prepareStatement(sql)) {
            ps.setInt(1, idProjet);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    InvRow r = new InvRow();
                    Date d = rs.getDate("date_investissement");
                    r.date = (d == null) ? "—" : d.toString();
                    r.montant = rs.getDouble("montantInvestissement");
                    r.statut = rs.getString("statut");
                    int uid = rs.getInt("id_user");
                    r.userLabel = "User #" + uid;
                    out.add(r);
                }
            }
        }
        return out;
    }

    // ========================= File open + toast =========================

    private void openFile(String path) {
        if (path == null || path.isBlank()) return;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(new File(path));
            }
        } catch (Exception e) {
            toast("❌ Impossible d’ouvrir le fichier", "toastError");
        }
    }

    private void toast(String msg, String cssClass) {
        if (lblToast == null) return;

        if (msg == null || msg.isBlank()) {
            lblToast.setVisible(false);
            lblToast.setManaged(false);
            lblToast.setText("");
            return;
        }

        lblToast.getStyleClass().removeAll("toastInfo", "toastSuccess", "toastError");
        lblToast.getStyleClass().add(cssClass);

        lblToast.setText(msg);
        lblToast.setVisible(true);
        lblToast.setManaged(true);

        PauseTransition p = new PauseTransition(Duration.seconds(2.5));
        p.setOnFinished(e -> {
            lblToast.setText("");
            lblToast.setVisible(false);
            lblToast.setManaged(false);
        });
        p.play();
    }

    private String safeErr(Throwable t) {
        return (t == null || t.getMessage() == null) ? "Erreur" : t.getMessage();
    }

    // ========================= Models =========================
    private static class ProjectRow {
        int id;
        String titre;
        String description;
        double budget;
        String statut;
    }
    private static class KpiRow {
        double budget;
        double totalLeve;
    }
    private static class DocsScore {
        List<ProjetDocument> docs;
        int score;
    }
    public static class InvRow {
        String date;
        double montant;
        String statut;
        String userLabel;
    }
}