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
import java.util.concurrent.atomic.AtomicLong;

public class AuctionCardController {

    @FXML private HBox cardContainer;
    @FXML private Label lblAuctionName;
    @FXML private Label lblAuctionStatus;
    @FXML private Label lblSellerName;

    private LiveAuctionModel.AuctionDetail auction;
    private final com.frontendauction.service.ReviewService reviewService = new com.frontendauction.service.ReviewService();
    private final AtomicLong ratingRequestSeq = new AtomicLong();

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

        refreshSellerRating(auction.getId(), sellerName);
        
        if ("CLOSED".equalsIgnoreCase(status)) {
            cardContainer.setOpacity(0.6);
            cardContainer.setStyle("-fx-padding: 20 24; -fx-cursor: default;");
        } else {
            cardContainer.setStyle("-fx-padding: 20 24; -fx-cursor: hand;");
        }
    }

    private void refreshSellerRating(String auctionId, String sellerName) {
        if (sellerName == null || sellerName.isBlank() || "Unknown".equalsIgnoreCase(sellerName)) {
            return;
        }

        long requestId = ratingRequestSeq.incrementAndGet();
        reviewService.getReviewsForUser(sellerName)
                .thenAccept(reviews -> javafx.application.Platform.runLater(() -> {
                    if (requestId != ratingRequestSeq.get()) {
                        return;
                    }
                    if (auction == null || !Objects.equals(auction.getId(), auctionId)) {
                        return;
                    }

                    Double average = reviewService.averageRating(reviews, sellerName);
                    if (average != null) {
                        lblSellerName.setText("Seller: " + sellerName + " (" + String.format("%.1f \u2605", average) + ")");
                    } else {
                        lblSellerName.setText("Seller: " + sellerName + " (- \u2605)");
                    }
                }))
                .exceptionally(exception -> {
                    System.err.println("Failed to fetch reviews for seller " + sellerName + ": " + exception.getMessage());
                    return null;
                });
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
