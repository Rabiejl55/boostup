package controllers;

import javafx.fxml.FXML;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;

public class MeetingController {

    @FXML
    private WebView webView;

    private String roomName;

    public void setRoomName(String roomName) {
        this.roomName = roomName;
    }

    @FXML
    public void initialize() {

        if (roomName == null) {
            roomName = "DefaultRoom";
        }

        WebEngine engine = webView.getEngine();
        engine.load("https://meet.jit.si/" + roomName);
    }
}