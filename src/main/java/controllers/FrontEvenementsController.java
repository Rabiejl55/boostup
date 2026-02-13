package controllers;

import entities.GEvenement.Evenement;
import entities.GEvenement.EvenementFX;
import javafx.animation.Interpolator;
import javafx.animation.ScaleTransition;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.util.Duration;
import services.EvenementService.EvenementService;

import java.net.URL;
import java.sql.SQLException;
import java.util.Comparator;
import java.util.ResourceBundle;
import javafx.collections.ListChangeListener;

public class FrontEvenementsController implements Initializable {

    @FXML private TextField tfRecherche;
    @FXML private ComboBox<String> cbTri;
    @FXML private TilePane tileEvenements;
    @FXML private Label lblStatus;

    private final EvenementService es = new EvenementService();
    private final ObservableList<EvenementFX> data = FXCollections.observableArrayList();
    private final FilteredList<EvenementFX> filtered = new FilteredList<>(data, e -> true);

    // Placeholder image (si pas d'image en DB)
    private static final String FALLBACK_IMAGE = "/images/logo.png";

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupTri();
        setupSearch();
        load();
        render();

        // Re-render quand filtre change
        filtered.addListener((ListChangeListener<EvenementFX>) c -> render());
    }

    private void setupTri() {
        if (cbTri == null) return;
        cbTri.setItems(FXCollections.observableArrayList("Date", "Popularité", "Prix"));
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
        });
    }

    private void load() {
        data.clear();
        try {
            for (Evenement e : es.read()) {
                data.add(new EvenementFX(e));
            }
            setStatus(data.isEmpty() ? "Aucun événement." : (data.size() + " événement(s)"));
        } catch (SQLException ex) {
            setStatus("Erreur chargement: " + ex.getMessage());
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
            case "popularité":
            case "popularite":
                // Pas de champ en DB pour l'instant -> fallback sur capacité max
                list.sort(Comparator.comparingInt(EvenementFX::getCapaciteMax).reversed());
                break;
            case "prix":
                // Pas de champ prix -> fallback sur titre (stable)
                list.sort(Comparator.comparing(EvenementFX::getTitre, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));
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

        // Image 16:9
        ImageView img = new ImageView(loadImageFor(e));
        img.setPreserveRatio(false);
        img.setFitWidth(280);
        img.setFitHeight(158);
        img.getStyleClass().add("event-image");

        // Contenu
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

        card.getChildren().addAll(img, content);

        // Hover animations
        setupHover(card);
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

        // Light glow drop shadow (subtil)
        DropShadow ds = new DropShadow(18, Color.color(0.44, 0.26, 0.76, 0.35));
        ds.setOffsetY(10);
        ds.setOffsetX(0);
        card.setEffect(ds);
    }

    private Image loadImageFor(EvenementFX e) {
        String value = e == null ? null : e.getImage();
        value = value == null ? "" : value.trim();

        // 1) URL web
        if (value.startsWith("http://") || value.startsWith("https://")) {
            try {
                return new Image(value, true);
            } catch (Exception ignored) {
            }
        }

        // 2) Resource interne: /images/xxx.png
        if (value.startsWith("/")) {
            URL url = getClass().getResource(value);
            if (url != null) {
                return new Image(url.toExternalForm(), true);
            }
        }

        // 3) Fallback
        URL url = getClass().getResource(FALLBACK_IMAGE);
        if (url != null) {
            return new Image(url.toExternalForm(), true);
        }

        // 4) Dernier recours
        return new Image("file:" + System.getProperty("user.dir"));
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
            URL fxml = getClass().getResource("/views/login.fxml");
            if (fxml == null) {
                setStatus("FXML introuvable: /views/login.fxml");
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
}
