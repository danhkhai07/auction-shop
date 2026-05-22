package com.frontendauction;

import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public final class AppWindow {

    public static final double WIDTH = 1280.0;
    public static final double HEIGHT = 720.0;

    private AppWindow() {
    }

    public static Scene createScene(Parent root) {
        return new Scene(root, WIDTH, HEIGHT);
    }

    public static void applyScene(Stage stage, Parent root) {
        double width = stage.getWidth() > 0 ? stage.getWidth() : WIDTH;
        double height = stage.getHeight() > 0 ? stage.getHeight() : HEIGHT;
        stage.setScene(new Scene(root, width, height));
    }
}
