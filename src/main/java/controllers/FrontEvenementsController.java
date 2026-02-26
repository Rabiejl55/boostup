package controllers;

import entities.GEvenement.Evenement;
import entities.GEvenement.EvenementFX;
import javafx.animation.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.effect.GaussianBlur;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.EvenementService.EvenementService;
import services.RecommendationService;
import services.RecommendationNotificationService;
import utils.MyDatabase;

import java.net.URL;
import java.sql.*;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;
import java.util.ResourceBundle;

public class FrontEvenementsController implements Initializable {

    @FXML private TextField tfRecherche;
    @FXML private ComboBox<String> cbTri;
    @FXML private TilePane tileEvenements;
    @FXML private Label lblStatus;
    @FXML private Button btnCalendrier;

    // root + overlay
    @FXML private BorderPane rootPane;
    @FXML private StackPane overlayRoot;
    @FXML private StackPane modalHost;

    private GaussianBlur blur;

    private final EvenementService es = new EvenementService();
    private final RecommendationService recommendationService = new RecommendationService();
    private final ObservableList<EvenementFX> data = FXCollections.observableArrayList();
    private final FilteredList<EvenementFX> filtered = new FilteredList<>(data, e -> true);

    private static final String FALLBACK_IMAGE = "/images/logo.png";

    private final Map<Integer, Integer> bestNoteByEventId = new HashMap<>();

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTri();
        setupSearch();
        setupCalendarButtonMicroAnimations();
        load();
        render();

        // 🤖 Envoyer les notifications de recommandations IA (asynchrone)
        sendRecommendationNotificationsAsync();
    }

    private void setupTri() {
        if (cbTri == null) return;
        cbTri.setItems(FXCollections.observableArrayList("Date", "Note"));
        cbTri.getSelectionModel().select("Date");
        cbTri.valueProperty().addListener((obs, o, n) -> render());
    }

    private void setupSearch() {
        if (tfRecherche == null) return;
        tfRecherche.textProperty().addListener((obs, o, n) -> {
            String q = n == null ? "" : n.trim().toLowerCase();
            filtered.setPredicate(e -> {
                if (q.isEmpty()) return true;
                String titre = e.getTitre() == null ? "" : e.getTitre().toLowerCase();
                String type = e.getType() == null ? "" : e.getType().toLowerCase();
                String lieu = e.getLieu() == null ? "" : e.getLieu().toLowerCase();
                return titre.contains(q) || type.contains(q) || lieu.contains(q);
            });
            render();
        });
    }

    private void load() {
        data.clear();
        loadBestNotes();

        try {
            for (Evenement e : es.read()) {
                data.add(new EvenementFX(e));
            }
            setStatus(data.isEmpty() ? "Aucun événement." : (data.size() + " événement(s)"));
        } catch (SQLException ex) {
            setStatus("Erreur chargement: " + ex.getMessage());
        }
    }

    private void loadBestNotes() {
        bestNoteByEventId.clear();
        String sql = "SELECT p.id_evenement AS id_evenement, MAX(f.note) AS best_note " +
                "FROM feedback f " +
                "LEFT JOIN participation p ON f.id_participation = p.id_participation " +
                "GROUP BY p.id_evenement";

        Connection c = null;
        Statement st = null;
        ResultSet rs = null;
        try {
            c = MyDatabase.getInstance().getConnection();
            st = c.createStatement();
            rs = st.executeQuery(sql);
            while (rs.next()) {
                int idEvent = rs.getInt("id_evenement");
                if (rs.wasNull()) continue;
                bestNoteByEventId.put(idEvent, rs.getInt("best_note"));
            }
        } catch (SQLException e) {
            System.err.println("⚠️ Impossible de charger les notes: " + e.getMessage());
        } finally {
            try { if (rs != null) rs.close(); } catch (SQLException ignored) {}
            try { if (st != null) st.close(); } catch (SQLException ignored) {}
        }
    }

    private void render() {
        if (tileEvenements == null) return;
        tileEvenements.getChildren().clear();

        var list = FXCollections.observableArrayList(filtered);
        applySort(list);

        for (EvenementFX e : list) {
            tileEvenements.getChildren().add(buildCard(e));
        }
    }

    private void applySort(ObservableList<EvenementFX> list) {
        String tri = cbTri == null ? null : cbTri.getValue();
        if (tri == null) tri = "Date";

        switch (tri.toLowerCase()) {
            case "note":
                list.sort(Comparator
                        .comparingInt((EvenementFX e) -> bestNoteByEventId.getOrDefault(e.getId(), Integer.MIN_VALUE))
                        .reversed()
                        .thenComparing(EvenementFX::getTitre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER))
                );
                break;
            case "date":
            default:
                list.sort(Comparator.comparing(EvenementFX::getDateEvenement, Comparator.nullsLast(Comparator.naturalOrder())));
                break;
        }
    }

    private Node buildCard(EvenementFX e) {
        VBox card = new VBox();
        card.getStyleClass().add("event-card");
        card.setPrefWidth(280);
        card.setMinWidth(240);
        card.setMaxWidth(320);

        boolean archived = e.isArchived() || es.isArchived(e.getId());

        // Image 16:9 (+ badge note en overlay)
        ImageView img = new ImageView(loadImageFor(e));
        img.setPreserveRatio(false);
        img.setFitWidth(280);
        img.setFitHeight(158);
        img.getStyleClass().add("event-image");

        StackPane imageStack = new StackPane(img);
        imageStack.getStyleClass().add("event-image-stack");

        // 🤖 Badge de recommandation IA (en haut à droite)
        if (!archived && recommendationService.isRecommended(e.getId())) {
            Label recoBadge = buildRecommendationBadge(e);
            StackPane.setAlignment(recoBadge, Pos.TOP_RIGHT);
            imageStack.getChildren().add(recoBadge);
        }

        // Badge note (en haut à gauche si tri par note)
        if (isTriNote()) {
            Label badge = buildNoteBadge(e);
            if (badge != null) {
                StackPane.setAlignment(badge, Pos.TOP_LEFT);
                imageStack.getChildren().add(badge);
            }
        }

        // Si archivé: overlay X + style grisé
        if (archived) {
            Label x = new Label("✖");
            x.getStyleClass().add("event-archived-x");
            StackPane.setAlignment(x, Pos.CENTER);
            imageStack.getChildren().add(x);
            card.getStyleClass().add("event-archived");
        }

        VBox content = new VBox(6);
        content.setPadding(new javafx.geometry.Insets(12, 12, 12, 12));

        Label title = new Label(e.getTitre() == null ? "(Sans titre)" : e.getTitre());
        title.getStyleClass().add("event-title");
        title.setWrapText(true);

        String date = e.getDateEvenement() == null ? "" : e.getDateEvenement().toLocalDate().toString();
        Label dateLbl = new Label("📅 " + date);
        dateLbl.getStyleClass().add("event-date");

        Label desc = new Label(shorten(e.getDescription(), 90));
        desc.getStyleClass().add("event-desc");
        desc.setWrapText(true);

        content.getChildren().addAll(title, dateLbl, desc);

        card.getChildren().addAll(imageStack, content);

        if (!archived) {
            setupHover(card);
            // Click -> premium details
            card.setOnMouseClicked(ev -> openPremiumDetail(e, card));
        } else {
            // non cliquable (mais visible)
            card.setOnMouseClicked(null);
            card.setOnMouseEntered(null);
            card.setOnMouseExited(null);
            card.setMouseTransparent(true);
            card.setOpacity(0.55);
        }

        return card;
    }

    private void setupHover(Region card) {
        ScaleTransition in = new ScaleTransition(Duration.millis(220), card);
        in.setToX(1.05);
        in.setToY(1.05);
        in.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition out = new ScaleTransition(Duration.millis(220), card);
        out.setToX(1.0);
        out.setToY(1.0);
        out.setInterpolator(Interpolator.EASE_BOTH);

        card.setOnMouseEntered(ev -> in.playFromStart());
        card.setOnMouseExited(ev -> out.playFromStart());

        DropShadow ds = new DropShadow(18, Color.color(0.44, 0.26, 0.76, 0.35));
        ds.setOffsetY(10);
        ds.setOffsetX(0);
        card.setEffect(ds);
    }

    private void openPremiumDetail(EvenementFX e, Region card) {
        if (overlayRoot == null || modalHost == null || rootPane == null) return;

        // Zoom-in sur la carte (1.08)
        ScaleTransition zoom = new ScaleTransition(Duration.millis(340), card);
        zoom.setToX(1.08);
        zoom.setToY(1.08);
        zoom.setInterpolator(Interpolator.EASE_BOTH);

        zoom.setOnFinished(ev -> {
            showOverlay(e);
            // reset carte
            card.setScaleX(1.0);
            card.setScaleY(1.0);
        });
        zoom.playFromStart();
    }

    private void showOverlay(EvenementFX e) {
        // blur background
        if (blur == null) blur = new GaussianBlur(16);
        rootPane.setEffect(blur);

        overlayRoot.setManaged(true);
        overlayRoot.setVisible(true);
        overlayRoot.setOpacity(0);

        modalHost.getChildren().clear();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/EventDetailPremiumView.fxml"));
            Node modal = loader.load();

            EventDetailPremiumController controller = loader.getController();
            Integer best = bestNoteByEventId.get(e.getId());
            controller.setEvenement(e, best);
            controller.setOnClose(this::hideOverlay);

            modalHost.getChildren().add(modal);

            // apparition modal
            FadeTransition ft = new FadeTransition(Duration.millis(260), overlayRoot);
            ft.setFromValue(0);
            ft.setToValue(1);
            ft.setInterpolator(Interpolator.EASE_BOTH);
            ft.play();

            // petite animation d'entrée de la modal
            modal.setScaleX(0.97);
            modal.setScaleY(0.97);
            ScaleTransition st = new ScaleTransition(Duration.millis(260), modal);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_BOTH);
            st.play();

        } catch (Exception ex) {
            ex.printStackTrace();
            setStatus("Impossible d'ouvrir le détail: " + ex.getMessage());
            hideOverlay();
        }
    }

    private void hideOverlay() {
        if (overlayRoot == null || rootPane == null) return;

        FadeTransition ft = new FadeTransition(Duration.millis(200), overlayRoot);
        ft.setFromValue(overlayRoot.getOpacity());
        ft.setToValue(0);
        ft.setInterpolator(Interpolator.EASE_BOTH);
        ft.setOnFinished(ev -> {
            overlayRoot.setVisible(false);
            overlayRoot.setManaged(false);
            overlayRoot.setOpacity(1);
            if (modalHost != null) modalHost.getChildren().clear();
            rootPane.setEffect(null);
        });
        ft.play();
    }

    @FXML
    private void handleOverlayBackdrop() {
        hideOverlay();
    }

    private boolean isTriNote() {
        return cbTri != null && cbTri.getValue() != null && cbTri.getValue().trim().equalsIgnoreCase("Note");
    }

    private Label buildNoteBadge(EvenementFX e) {
        if (e == null) return null;

        Integer best = bestNoteByEventId.get(e.getId());
        if (best == null) {
            Label badge = new Label("⭐ —");
            badge.getStyleClass().addAll("badge-note", "badge-note-na");
            return badge;
        }

        int note = best;
        String text;
        String style;

        switch (note) {
            case 5:
                text = "⭐ 5";
                style = "badge-note badge-note-5";
                break;
            case 4:
                text = "⭐ 4";
                style = "badge-note badge-note-4";
                break;
            case 3:
                text = "👍 3";
                style = "badge-note badge-note-3";
                break;
            case 2:
                text = "👍 2";
                style = "badge-note badge-note-2";
                break;
            case 1:
            default:
                text = "👎 1";
                style = "badge-note badge-note-1";
                break;
        }

        Label badge = new Label(text);
        for (String cls : style.split(" ")) {
            badge.getStyleClass().add(cls);
        }
        return badge;
    }

    private Image loadImageFor(EvenementFX e) {
        String value = e == null ? null : e.getImage();
        value = value == null ? "" : value.trim();

        if (value.startsWith("http://") || value.startsWith("https://")) {
            try {
                return new Image(value, true);
            } catch (Exception ignored) {
            }
        }

        if (value.startsWith("/")) {
            URL url = getClass().getResource(value);
            if (url != null) {
                return new Image(url.toExternalForm(), true);
            }
        }

        URL url = getClass().getResource(FALLBACK_IMAGE);
        return url == null ? null : new Image(url.toExternalForm(), true);
    }

    private static String shorten(String s, int max) {
        if (s == null) return "";
        String t = s.trim();
        if (t.length() <= max) return t;
        return t.substring(0, Math.max(0, max - 1)) + "…";
    }

    private void setStatus(String s) {
        if (lblStatus != null) {
            lblStatus.setText(s == null ? "" : s);
        }
    }

    @FXML
    private void handleLogout() {
        try {
            URL fxml = getClass().getResource("/login.fxml");
            if (fxml == null) {
                setStatus("FXML introuvable: /login.fxml");
                return;
            }

            Parent root = FXMLLoader.load(fxml);
            Stage stage = (Stage) tileEvenements.getScene().getWindow();
            stage.setScene(new Scene(root, 900, 600));
            stage.setTitle("BoostUp - Connexion");
        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Impossible de revenir au login: " + e.getMessage());
        }
    }

    private void setupCalendarButtonMicroAnimations() {
        if (btnCalendrier == null) return;

        Duration d = Duration.millis(300);

        btnCalendrier.setOnMouseEntered(e -> {
            ScaleTransition st = new ScaleTransition(d, btnCalendrier);
            st.setToX(1.03);
            st.setToY(1.03);
            st.setInterpolator(Interpolator.EASE_BOTH);

            TranslateTransition tt = new TranslateTransition(d, btnCalendrier);
            tt.setToY(-2);
            tt.setInterpolator(Interpolator.EASE_BOTH);

            new ParallelTransition(st, tt).play();
        });

        btnCalendrier.setOnMouseExited(e -> {
            ScaleTransition st = new ScaleTransition(d, btnCalendrier);
            st.setToX(1.0);
            st.setToY(1.0);
            st.setInterpolator(Interpolator.EASE_BOTH);

            TranslateTransition tt = new TranslateTransition(d, btnCalendrier);
            tt.setToY(0);
            tt.setInterpolator(Interpolator.EASE_BOTH);

            new ParallelTransition(st, tt).play();
        });

        btnCalendrier.setOnMousePressed(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(120), btnCalendrier);
            st.setToX(0.98);
            st.setToY(0.98);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });

        btnCalendrier.setOnMouseReleased(e -> {
            ScaleTransition st = new ScaleTransition(Duration.millis(160), btnCalendrier);
            st.setToX(1.03);
            st.setToY(1.03);
            st.setInterpolator(Interpolator.EASE_OUT);
            st.play();
        });
    }

    @FXML
    private void handleOpenCalendar() {
        try {
            openCalendarOverlay();
        } catch (Exception ex) {
            ex.printStackTrace();
            setStatus("Impossible d'ouvrir le calendrier: " + ex.getMessage());
        }
    }

    private void openCalendarOverlay() throws Exception {
        if (overlayRoot == null || modalHost == null || rootPane == null) return;

        // blur background
        if (blur == null) blur = new GaussianBlur(16);
        rootPane.setEffect(blur);

        overlayRoot.setManaged(true);
        overlayRoot.setVisible(true);
        overlayRoot.setOpacity(0);
        modalHost.getChildren().clear();

        FXMLLoader loader = new FXMLLoader(getClass().getResource("/CalendarView.fxml"));
        Node cal = loader.load();

        CalendarController controller = loader.getController();
        controller.setOnClose(this::hideOverlay);
        controller.setEvents(new java.util.ArrayList<>(data));
        controller.setOnOpenDetail(ev -> {
            // ferme le calendrier puis ouvre la modal détail
            modalHost.getChildren().clear();
            showOverlay(ev);
        });

        modalHost.getChildren().add(cal);

        // apparition overlay
        FadeTransition ft = new FadeTransition(Duration.millis(260), overlayRoot);
        ft.setFromValue(0);
        ft.setToValue(1);
        ft.setInterpolator(Interpolator.EASE_BOTH);

        // slide up + petit zoom
        cal.setOpacity(0);
        cal.setTranslateY(24);
        cal.setScaleX(0.985);
        cal.setScaleY(0.985);

        FadeTransition cft = new FadeTransition(Duration.millis(260), cal);
        cft.setToValue(1);

        TranslateTransition tt = new TranslateTransition(Duration.millis(320), cal);
        tt.setToY(0);
        tt.setInterpolator(Interpolator.EASE_BOTH);

        ScaleTransition st = new ScaleTransition(Duration.millis(320), cal);
        st.setToX(1.0);
        st.setToY(1.0);
        st.setInterpolator(Interpolator.EASE_BOTH);

        new ParallelTransition(ft, cft, tt, st).play();
    }

    @FXML
    private void handleUtilisateurs() {
        setStatus("Section Utilisateurs : non implémentée dans ce module.");
    }

    @FXML
    private void handleAccompagnement() {
        setStatus("Section Accompagnement : non implémentée dans ce module.");
    }

    @FXML
    private void handleFinancement() {
        setStatus("Section Financement : non implémentée dans ce module.");
    }

    @FXML
    private void handleCandidatures() {
        setStatus("Section Candidatures : non implémentée dans ce module.");
    }

    @FXML
    private void handleEvenements() {
        // On est déjà sur l'écran événements front
        setStatus("Événements");
    }

    @FXML
    private void handleGoHome() {
        try {
            URL fxml = getClass().getResource("/HomePage.fxml");
            if (fxml == null) {
                setStatus("FXML introuvable: /HomePage.fxml");
                return;
            }

            Parent root = FXMLLoader.load(fxml);
            Stage stage = (Stage) tileEvenements.getScene().getWindow();
            Scene scene = stage.getScene();
            if (scene == null) {
                stage.setScene(new Scene(root, 1000, 650));
            } else {
                scene.setRoot(root);
            }
            stage.setTitle("BoostUp - Accueil");
        } catch (Exception e) {
            e.printStackTrace();
            setStatus("Impossible d'ouvrir l'accueil: " + e.getMessage());
        }
    }

    // ════════════════════════════════════════════════════════
    // 🤖 RECOMMANDATION IA
    // ════════════════════════════════════════════════════════

    /**
     * Envoie les notifications de recommandations IA de manière asynchrone
     */
    private void sendRecommendationNotificationsAsync() {
        new Thread(() -> {
            try {
                // Attendre 2 secondes que l'interface se charge complètement
                Thread.sleep(2000);

                // Envoyer les notifications
                RecommendationNotificationService.getInstance().sendRecommendationNotifications();

            } catch (Exception e) {
                System.err.println("⚠️ Erreur envoi notifications recommandations : " + e.getMessage());
            }
        }).start();
    }

    /**
     * Crée le badge "Recommandé pour vous" avec animation
     */
    private Label buildRecommendationBadge(EvenementFX e) {
        double score = recommendationService.getRecommendationScore(e.getId());

        Label badge = new Label();
        badge.setMaxWidth(150);
        badge.setWrapText(true);
        badge.setAlignment(Pos.CENTER);

        // Style selon le score
        if (score >= 0.8) {
            badge.setText("⭐⭐⭐ Pour vous");
            badge.setStyle("-fx-background-color: linear-gradient(to right, #667eea, #764ba2); " +
                          "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; " +
                          "-fx-padding: 6 12; -fx-background-radius: 15; " +
                          "-fx-effect: dropshadow(gaussian, rgba(102,126,234,0.5), 8, 0, 0, 2);");
        } else if (score >= 0.6) {
            badge.setText("💜 Recommandé");
            badge.setStyle("-fx-background-color: linear-gradient(to right, #e63956, #ff6b9d); " +
                          "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; " +
                          "-fx-padding: 6 12; -fx-background-radius: 15; " +
                          "-fx-effect: dropshadow(gaussian, rgba(230,57,86,0.5), 8, 0, 0, 2);");
        } else {
            badge.setText("✨ Suggéré");
            badge.setStyle("-fx-background-color: linear-gradient(to right, #1b2a4a, #2d1b4e); " +
                          "-fx-text-fill: white; -fx-font-size: 10px; -fx-font-weight: bold; " +
                          "-fx-padding: 6 12; -fx-background-radius: 15; " +
                          "-fx-effect: dropshadow(gaussian, rgba(27,42,74,0.5), 8, 0, 0, 2);");
        }

        badge.setTranslateX(-8);
        badge.setTranslateY(8);

        // Animation pulsante
        ScaleTransition pulse = new ScaleTransition(Duration.millis(1000), badge);
        pulse.setFromX(1.0);
        pulse.setFromY(1.0);
        pulse.setToX(1.1);
        pulse.setToY(1.1);
        pulse.setCycleCount(Animation.INDEFINITE);
        pulse.setAutoReverse(true);
        pulse.setInterpolator(Interpolator.EASE_BOTH);
        pulse.play();

        return badge;
    }

}
