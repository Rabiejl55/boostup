package controllers;

import entities.GAccompagnement.Session;
import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import services.AccompagnementService.SessionService;

import java.sql.SQLException;
import java.util.List;

public class MapController {

    @FXML
    private WebView webView;

    private WebEngine engine;

    private SessionService service = new SessionService();

    @FXML
    public void initialize() {
        engine = webView.getEngine();
        engine.load(getClass().getResource("/fxml/mapview.html").toExternalForm());

        engine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            if (newState == Worker.State.SUCCEEDED) {
                try {
                    loadSessionsOnMap();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
        });
    }

    private void loadSessionsOnMap() throws SQLException {
        List<Session> sessions = service.afficherAll();

        // Effacer les markers existants
        engine.executeScript("if(window.clearMarkers) clearMarkers();");

        for (Session session : sessions) {
            if (session.getLatitude() != null && session.getLongitude() != null) {
                String popup = session.getLieu()
                        .replace("'", "\\'")
                        .replace("\"", "\\\"") + "<br>Date: " + session.getDateSession();

                engine.executeScript(
                        "addMarker(" +
                                session.getLatitude() + "," +
                                session.getLongitude() + ",'" +
                                popup + "')"
                );
            }
        }
    }

    @FXML
    private void handleOpenMap() {
        try {
            loadSessionsOnMap();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}