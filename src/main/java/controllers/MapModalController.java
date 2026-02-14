package controllers;

import javafx.concurrent.Worker;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

import java.awt.*;
import java.net.URI;
import java.net.URL;
import java.util.ResourceBundle;

public class MapModalController implements Initializable {

    @FXML private WebView webView;
    @FXML private ProgressIndicator progress;
    @FXML private Label lblSubtitle;
    @FXML private Button btnOpenExternal;

    private Runnable onClose;
    private String externalUrl;

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void setSubtitle(String subtitle) {
        if (lblSubtitle != null) {
            lblSubtitle.setText(subtitle == null ? "" : subtitle);
        }
    }

    public void loadUrl(String url, String externalUrl) {
        this.externalUrl = externalUrl;

        if (btnOpenExternal != null) {
            btnOpenExternal.setDisable(externalUrl == null || externalUrl.isBlank());
        }

        if (webView == null) return;

        WebEngine eng = webView.getEngine();
        if (progress != null) {
            progress.setVisible(true);
        }

        eng.getLoadWorker().stateProperty().addListener((obs, o, n) -> {
            if (n == Worker.State.SUCCEEDED || n == Worker.State.FAILED || n == Worker.State.CANCELLED) {
                if (progress != null) progress.setVisible(false);
            }
        });

        eng.load(url);
    }

    @FXML
    private void handleClose() {
        if (onClose != null) onClose.run();
    }

    @FXML
    private void handleOpenExternal() {
        if (externalUrl == null || externalUrl.isBlank()) return;
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().browse(URI.create(externalUrl));
            }
        } catch (Exception ignored) {
        }
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // nothing
    }
}
