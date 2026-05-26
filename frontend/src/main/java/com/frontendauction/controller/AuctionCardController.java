package com.frontendauction.controller;

import com.frontendauction.AppWindow;
import com.frontendauction.model.LiveAuctionModel;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.HBox;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Objects;

public class AuctionCardController {

    @FXML private HBox cardContainer;
    @FXML private Label lblAuctionName;
    @FXML private Label lblAuctionStatus;

    private LiveAuctionModel.AuctionDetail auction;

    public void setAuction(LiveAuctionModel.AuctionDetail auction) {
        this.auction = auction;
        String name = auction.getName();
        if (name == null || name.isBlank()) {
            name = "Auction " + auction.getId();
        }
        lblAuctionName.setText(name);
        
        String status = auction.getStatus() != null ? auction.getStatus() : "OPEN";
        lblAuctionStatus.setText("Status: " + status);
        
        if ("CLOSED".equalsIgnoreCase(status)) {
            cardContainer.setOpacity(0.6);
            cardContainer.setStyle("-fx-padding: 20 24; -fx-cursor: default;");
        } else {
            cardContainer.setStyle("-fx-padding: 20 24; -fx-cursor: hand;");
        }
    }

    @FXML
    public void onCardClick(MouseEvent event) throws IOException {
        if (auction == null) return;
        
        String status = auction.getStatus() != null ? auction.getStatus() : "OPEN";
        if ("CLOSED".equalsIgnoreCase(status)) {
            return;
        }
        
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/com/frontendauction/live-auction.fxml")));
        Parent root = loader.load();

        LiveAuctionController controller = loader.getController();
        controller.setAuctionId(auction.getId());

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        AppWindow.applyScene(stage, root);
        stage.show();
    }
}
