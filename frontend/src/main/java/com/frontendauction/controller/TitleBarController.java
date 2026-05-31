package com.frontendauction.controller;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Cursor;
import javafx.scene.Scene;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

public class TitleBarController {
    @FXML private HBox titleBarContainer;

    private double xOffset = 0;
    private double yOffset = 0;

    private static final double RESIZE_MARGIN = 6;

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

        // Enable edge resize after scene is ready
        Platform.runLater(() -> {
            Scene scene = titleBarContainer.getScene();
            if (scene != null) {
                enableEdgeResize(scene);
            }
        });
    }

    private void enableEdgeResize(Scene scene) {
        Stage stage = (Stage) scene.getWindow();
        final double[] dragStart = new double[4]; // startX, startY, stageW, stageH
        final String[] resizeDir = {""};

        scene.setOnMouseMoved(event -> {
            if (stage.isMaximized() || stage.isFullScreen()) {
                scene.setCursor(Cursor.DEFAULT);
                return;
            }
            double x = event.getSceneX();
            double y = event.getSceneY();
            double w = scene.getWidth();
            double h = scene.getHeight();

            boolean left = x < RESIZE_MARGIN;
            boolean right = x > w - RESIZE_MARGIN;
            boolean top = y < RESIZE_MARGIN;
            boolean bottom = y > h - RESIZE_MARGIN;

            if (right && bottom) scene.setCursor(Cursor.SE_RESIZE);
            else if (left && bottom) scene.setCursor(Cursor.SW_RESIZE);
            else if (right && top) scene.setCursor(Cursor.NE_RESIZE);
            else if (left && top) scene.setCursor(Cursor.NW_RESIZE);
            else if (right) scene.setCursor(Cursor.E_RESIZE);
            else if (left) scene.setCursor(Cursor.W_RESIZE);
            else if (bottom) scene.setCursor(Cursor.S_RESIZE);
            else if (top) scene.setCursor(Cursor.N_RESIZE);
            else scene.setCursor(Cursor.DEFAULT);
        });

        scene.setOnMousePressed(event -> {
            if (stage.isMaximized() || stage.isFullScreen()) return;
            double x = event.getSceneX();
            double y = event.getSceneY();
            double w = scene.getWidth();
            double h = scene.getHeight();

            boolean left = x < RESIZE_MARGIN;
            boolean right = x > w - RESIZE_MARGIN;
            boolean top = y < RESIZE_MARGIN;
            boolean bottom = y > h - RESIZE_MARGIN;

            StringBuilder dir = new StringBuilder();
            if (top) dir.append("N");
            if (bottom) dir.append("S");
            if (left) dir.append("W");
            if (right) dir.append("E");
            resizeDir[0] = dir.toString();

            dragStart[0] = event.getScreenX();
            dragStart[1] = event.getScreenY();
            dragStart[2] = stage.getWidth();
            dragStart[3] = stage.getHeight();
        });

        scene.setOnMouseDragged(event -> {
            if (resizeDir[0].isEmpty() || stage.isMaximized() || stage.isFullScreen()) return;
            double dx = event.getScreenX() - dragStart[0];
            double dy = event.getScreenY() - dragStart[1];
            double minW = 800;
            double minH = 500;

            if (resizeDir[0].contains("E")) {
                stage.setWidth(Math.max(minW, dragStart[2] + dx));
            }
            if (resizeDir[0].contains("S")) {
                stage.setHeight(Math.max(minH, dragStart[3] + dy));
            }
            if (resizeDir[0].contains("W")) {
                double newW = Math.max(minW, dragStart[2] - dx);
                if (newW != stage.getWidth()) {
                    stage.setX(stage.getX() + (stage.getWidth() - newW));
                    stage.setWidth(newW);
                }
            }
            if (resizeDir[0].contains("N")) {
                double newH = Math.max(minH, dragStart[3] - dy);
                if (newH != stage.getHeight()) {
                    stage.setY(stage.getY() + (stage.getHeight() - newH));
                    stage.setHeight(newH);
                }
            }
        });

        scene.setOnMouseReleased(event -> resizeDir[0] = "");
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
