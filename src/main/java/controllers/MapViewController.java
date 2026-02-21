package controllers;
import javafx.scene.web.WebView;
import javafx.scene.web.WebEngine;
import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.stage.Stage;
import java.util.List;
import javafx.scene.control.Button;
import entities.GAccompagnement.Session;
import utils.GeocodeUtil;
public class MapViewController {

    @FXML
    private void handleCloseMap(ActionEvent event) {
        // Récupère la fenêtre du bouton cliqué et ferme la fenêtre
        Stage stage = (Stage) ((Button) event.getSource()).getScene().getWindow();
        stage.close();
    }
    @FXML
    private WebView mapWebView;

    private WebEngine webEngine;

    @FXML
    public void initialize() {
        webEngine = mapWebView.getEngine();
        // Charge le HTML de la carte (Leaflet)
        webEngine.load(getClass().getResource("/fxml/mapview.html").toExternalForm());
    }

    // Méthode pour afficher les sessions
    public void loadSessions(List<Session> sessionsList) {
        for (Session s : sessionsList) {
            double[] coords = GeocodeUtil.getCoordinates(s.getLieu());
            if (coords != null) {
                String js = "L.marker([" + coords[0] + "," + coords[1] + "])"
                        + ".addTo(map)"
                        + ".bindPopup('" + s.getTypeSession() + " - " + s.getLieu() + "');";
                webEngine.executeScript(js);
            }
        }
    }
}