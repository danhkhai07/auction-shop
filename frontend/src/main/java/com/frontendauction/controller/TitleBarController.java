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
        titleBarContainer.setOnMousePressed(event -> {
            xOffset = event.getSceneX();
            yOffset = event.getSceneY();
        });

        titleBarContainer.setOnMouseDragged(event -> {
            Stage stage = (Stage) titleBarContainer.getScene().getWindow();
            if (!stage.isFullScreen() && !stage.isMaximized()) {
                stage.setX(event.getScreenX() - xOffset);
                stage.setY(event.getScreenY() - yOffset);
            }
        });
    }

    @FXML
    private void handleMinimize(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setIconified(true);
    }

    @FXML
    private void handleFullScreen(ActionEvent event) {
        Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        stage.setMaximized(!stage.isMaximized());
    }

    @FXML
    private void handleClose(ActionEvent event) {
        Platform.exit();
        System.exit(0);
    }
}
