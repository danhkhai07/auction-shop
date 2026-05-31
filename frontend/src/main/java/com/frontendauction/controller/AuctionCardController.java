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
    private final com.frontendauction.service.ReviewService reviewService = new com.frontendauction.service.ReviewService();

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
        lblSellerName.setText("Seller: " + sellerName);

        if (!"Unknown".equals(sellerName)) {
            reviewService.getReviewsForUser(sellerName)
                .thenAccept(reviews -> javafx.application.Platform.runLater(() -> {
                    double sum = 0;
                    int count = 0;
                    for (com.frontendauction.model.ReviewModel r : reviews) {
                        if (r.getTargetUser() != null && r.getTargetUser().equalsIgnoreCase(sellerName)) {
                            sum += r.getStars();
                            count++;
                        }
                    }
                    if (count > 0) {
                        lblSellerName.setText("Seller: " + sellerName + " (" + String.format("%.1f \u2605", sum / count) + ")");
                    } else {
                        lblSellerName.setText("Seller: " + sellerName + " (- \u2605)");
                    }
                }));
        }
        
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
                        boolean isSeller = user != null && user.getId() != null && auction.getSeller() != null && user.getId().equals(auction.getSeller().getId());
                        navigateToAuction(event, isSeller);
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
