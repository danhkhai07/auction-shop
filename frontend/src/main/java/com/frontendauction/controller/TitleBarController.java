package com.frontendauction.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class TitleBarController {
    @FXML private HBox titleBarContainer;

    private double xOffset = 0;
    private double yOffset = 0;

    @FXML
    public void initialize() {
        // Thiết lập sự kiện kéo thả để di chuyển cửa sổ ứng dụng
        titleBarContainer.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        titleBarContainer.setOnMouseDragged(event -> {
            Stage stage = (Stage) titleBarContainer.getScene().getWindow();
            if (!stage.isFullScreen()) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });
    }

    @FXML
    private void handleMinimize(ActionEvent event) {
        // Lấy Stage hiện tại từ event và thu nhỏ
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void handleFullScreen(ActionEvent event) {
        // Lấy Stage hiện tại từ event và bật/tắt Full Screen
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        if (stage.isFullScreen()) {
            stage.setFullScreen(false);
        } else {
            stage.setFullScreen(true);
            stage.setFullScreenExitHint(""); // Tắt dòng thông báo "Press ESC"
        }
    }

    @FXML
    private void handleClose(ActionEvent event) {
        // Thoát ứng dụng
        Platform.exit();
        System.exit(0);
    }
}
