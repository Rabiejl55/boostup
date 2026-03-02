package controllers;

import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;

public class MapViewController {

    @FXML
    private WebView mapWebView;

    @FXML
    public void initialize() {
        // Load whatever map you want here (Google Maps, OpenStreetMap, etc.)
        WebEngine engine = mapWebView.getEngine();
        // Example with OpenStreetMap:
        engine.load("https://www.openstreetmap.org");
        // Or a local HTML file with your custom map logic
        // engine.load(getClass().getResource("/map/index.html").toExternalForm());
    }

    @FXML
    private void handleCloseMap() {
        // Close the pop‑up window
        Stage stage = (Stage) mapWebView.getScene().getWindow();
        stage.close();
    }
}