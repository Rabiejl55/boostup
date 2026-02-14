package controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.net.URL;
import java.util.ResourceBundle;

public class QrCodeModalController implements Initializable {

    @FXML private ImageView imgQr;

    private Runnable onClose;

    public void setOnClose(Runnable onClose) {
        this.onClose = onClose;
    }

    public void setQr(Image image) {
        if (imgQr != null) imgQr.setImage(image);
    }

    // compat anciennes signatures (on ignore texte/subtitle)
    public void setQr(Image image, String subtitle) {
        setQr(image);
    }

    public void setQr(Image image, String subtitle, String payload) {
        setQr(image);
    }

    @Override
    public void initialize(URL location, ResourceBundle resources) {
    }

    @FXML
    private void handleClose() {
        if (onClose != null) onClose.run();
    }
}
