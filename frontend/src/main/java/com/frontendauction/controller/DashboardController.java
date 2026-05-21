package com.frontendauction.controller;

import com.frontendauction.AppWindow;
import com.frontendauction.service.TokenStore;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class DashboardController {

    @FXML
    public void goToLiveAuction(Event event) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/com/frontendauction/live-auction.fxml")));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Parent root = loader.load();
        AppWindow.applyScene(stage, root);
        stage.show();
    }

    @FXML
    public void goToProductManagement(Event event) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/com/frontendauction/product-management.fxml")));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Parent root = loader.load();
        AppWindow.applyScene(stage, root);
        stage.show();
    }

    @FXML
    public void logout(Event event) throws IOException {
        TokenStore.clear();
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/com/frontendauction/login.fxml")));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Parent root = loader.load();
        AppWindow.applyScene(stage, root);
        stage.show();
    }

    @FXML
    public void goToProfile(Event event) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/com/frontendauction/profile.fxml")));
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        Parent root = loader.load();
        AppWindow.applyScene(stage, root);
        stage.show();
    }
}
