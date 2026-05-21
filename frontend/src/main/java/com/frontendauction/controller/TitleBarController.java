package com.frontendauction.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class TitleBarController {
    @FXML private HBox titleBarContainer;

    private boolean isMaximized = false;
    private double oldX = 0, oldY = 0, oldWidth = 0, oldHeight = 0;

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

        javafx.stage.Screen screen = javafx.stage.Screen.getPrimary();
        // VisualBounds là kích thước màn hình ĐÃ TRỪ THANH TASKBAR
        javafx.geometry.Rectangle2D visualBounds = screen.getVisualBounds();

        if (isMaximized) {
            // TRẠNG THÁI: Đang to -> Thu nhỏ về kích thước cũ
            stage.setX(oldX);
            stage.setY(oldY);
            stage.setWidth(oldWidth);
            stage.setHeight(oldHeight);

            isMaximized = false;
        } else {
            oldX = stage.getX();
            oldY = stage.getY();
            oldWidth = stage.getWidth();
            oldHeight = stage.getHeight();

            // Ép cửa sổ khít theo kích thước của Visual Bounds
            stage.setX(visualBounds.getMinX());
            stage.setY(visualBounds.getMinY());
            stage.setWidth(visualBounds.getWidth());
            stage.setHeight(visualBounds.getHeight());

            isMaximized = true;
        }
    }

    @FXML
    private void handleClose(ActionEvent event) {
        // Thoát ứng dụng
        Platform.exit();
        System.exit(0);
    }
}
