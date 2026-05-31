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
    @FXML private Label lblSellerName;

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
        
        String sellerName = (auction.getSeller() != null && auction.getSeller().getUsername() != null)
                ? auction.getSeller().getUsername() : "Unknown";
        
        // Add mocked reputation display
        double mockReputation = 4.5 + Math.random() * 0.5; // Mock seller reputation
        String sellerReputation = String.format(java.util.Locale.US, "%.1f \u2605", mockReputation);
        lblSellerName.setText("Seller: " + sellerName + " (" + sellerReputation + ")");
        
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
        
        Node source = (Node) event.getSource();
        source.setDisable(true);

        new com.frontendauction.service.UserProfileService().getCurrentUser()
                .thenAccept(user -> {
                    javafx.application.Platform.runLater(() -> {
                        source.setDisable(false);
                        boolean isSeller = user != null && user.getAuctionList() != null && user.getAuctionList().contains(auction.getId());
                        
                        // Mock Reputation Check
                        double mockReputation = Math.random() < 0.3 ? 2.5 : 4.8; // 30% chance of low reputation for demo
                        boolean isHighValue = auction.getStartingPrice() != null && auction.getStartingPrice() > 5000000;
                        if (!isSeller && isHighValue && mockReputation < 3.0) {
                            javafx.scene.control.Alert alert = new javafx.scene.control.Alert(javafx.scene.control.Alert.AlertType.WARNING);
                            alert.setTitle("Tín nhiệm thấp");
                            alert.setHeaderText("Không thể tham gia phiên đấu giá");
                            alert.setContentText(String.format("Điểm tín nhiệm của bạn (%.1f \u2605) quá thấp để tham gia các phiên đấu giá có giá trị cao (trên 5,000,000 VND).", mockReputation));
                            alert.showAndWait();
                        } else {
                            navigateToAuction(event, isSeller);
                        }
                    });
                })
                .exceptionally(ex -> {
                    javafx.application.Platform.runLater(() -> {
                        source.setDisable(false);
                        navigateToAuction(event, false);
                    });
                    return null;
                });
    }

    private void navigateToAuction(MouseEvent event, boolean isSeller) {
        try {
            String fxmlFile = isSeller ? "/com/frontendauction/seller-controlpanel.fxml" : "/com/frontendauction/live-auction.fxml";
            FXMLLoader loader = new FXMLLoader(
                    Objects.requireNonNull(getClass().getResource(fxmlFile)));
            Parent root = loader.load();

            LiveAuctionController controller = loader.getController();
            controller.setAuctionId(auction.getId());

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            AppWindow.applyScene(stage, root);
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
