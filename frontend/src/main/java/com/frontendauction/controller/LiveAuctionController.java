package com.frontendauction.controller;

import com.frontendauction.model.BidResult;
import com.frontendauction.model.LiveAuctionModel;
import com.frontendauction.service.LiveAuctionService;
import com.frontendauction.service.TokenStore;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.List;

public class LiveAuctionController {

    @FXML private AnchorPane rootPane;
    @FXML private Button btnBack;
    @FXML private Label lblProductName;
    @FXML private Label lblSeller;
    @FXML private ImageView imgProduct;
    @FXML private Label lblDescription;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTimeLeft;
    @FXML private CheckBox chkAutoBid;
    @FXML private TextField txtMaxAutoBid;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private CategoryAxis timeAxis;
    @FXML private NumberAxis priceAxis;
    @FXML private ListView<LiveAuctionModel.BidEntry> lvBidHistory;

    private final LiveAuctionService auctionService = new LiveAuctionService();

    // TODO: nhận auctionId từ dashboard khi navigate sang màn hình này
    private String currentAuctionId = "1";

    private long timeLeftSeconds = 0;
    private Timeline countdownTimeline;

    @FXML
    public void initialize() {
        // Cập nhật kích thước FXML tự động full với màn hình thực tế
        if (rootPane != null) {
            javafx.geometry.Rectangle2D bounds = javafx.stage.Screen.getPrimary().getVisualBounds();
            rootPane.setPrefWidth(bounds.getWidth());
            rootPane.setPrefHeight(bounds.getHeight());
        }

        if(txtMaxAutoBid != null && chkAutoBid != null) {
            txtMaxAutoBid.disableProperty().bind(chkAutoBid.selectedProperty().not());
        }

        if(btnPlaceBid != null) {
            btnPlaceBid.setOnAction(event -> handlePlaceBid());
        }

        // Cài đặt biểu đồ
        if(priceChart != null) {
            priceChart.setAnimated(false);
            priceChart.setLegendVisible(false);
        }

        loadAuctionData();
    }

    public void setAuctionId(String auctionId) {
        this.currentAuctionId = auctionId;
        loadAuctionData();
    }

    private void loadAuctionData() {
        auctionService.getAuctionDetails(currentAuctionId).thenAccept(auction -> {
            if (auction != null) {
                Platform.runLater(() -> updateAuctionUI(auction));
            } else {
                Platform.runLater(() -> {
                    System.out.println("Warning: Backend is not connected or auction not found.");
                });
            }
        });
    }

    private void updateAuctionUI(LiveAuctionModel.AuctionDetail auction) {
        if (lblProductName != null) {
            lblProductName.setText(auction.getName() != null ? auction.getName() : "Unnamed Auction");
        }
        if (lblSeller != null) {
            lblSeller.setText("Status: " + (auction.getStatus() != null ? auction.getStatus() : "Unknown"));
        }
        if (lblDescription != null) {
            lblDescription.setText("Starting price: " + String.format("%,.0f VNĐ",
                    auction.getStartingPrice() != null ? auction.getStartingPrice() : 0.0));
        }

        // Giá hiện tại
        if (lblCurrentPrice != null && auction.getCurrentPrice() != null) {
            lblCurrentPrice.setText(String.format("%,.0f VNĐ", auction.getCurrentPrice()));
        }

        // Thời gian còn lại
        timeLeftSeconds = auction.getRemainingSeconds();
        startCountdown();

        // Bid history
        updateBidHistoryUI(auction.getBidHistory());
    }

    private void updateBidHistoryUI(List<LiveAuctionModel.BidEntry> bids) {
        if (lvBidHistory != null && bids != null) {
            ObservableList<LiveAuctionModel.BidEntry> observableBids = FXCollections.observableArrayList(bids);
            lvBidHistory.setItems(observableBids);
        }

        // Cập nhật biểu đồ giá
        if (priceChart != null && bids != null && !bids.isEmpty()) {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            for (LiveAuctionModel.BidEntry bid : bids) {
                String label = bid.getTimestamp() != null ? bid.getTimestamp() : "";
                if (label.contains("T")) {
                    label = label.substring(label.indexOf("T") + 1);
                    if (label.contains(".")) label = label.substring(0, label.indexOf("."));
                }
                series.getData().add(new XYChart.Data<>(label, bid.getBidAmount()));
            }
            priceChart.getData().clear();
            priceChart.getData().add(series);
        }
    }

    private void handlePlaceBid() {
        if (txtBidAmount == null) return;
        
        String amountText = txtBidAmount.getText().trim();
        if (amountText.isEmpty()) {
            showError("Please enter an amount!");
            return;
        }

        if (!TokenStore.hasToken()) {
            showError("You must login before placing a bid.");
            return;
        }

        try {
            Double amount = Double.parseDouble(amountText);

            txtBidAmount.clear();
            
            // Tạm khóa nút để tránh spam click
            if (btnPlaceBid != null) {
                btnPlaceBid.setDisable(true);
                btnPlaceBid.setText("Sending...");
            }

            LiveAuctionModel.BidRequest request = new LiveAuctionModel.BidRequest(amount);

            // Gửi dữ liệu qua API
            auctionService.placeBid(currentAuctionId, request).thenAccept(result -> {
                Platform.runLater(() -> {
                    if (btnPlaceBid != null) {
                        btnPlaceBid.setDisable(false);
                        btnPlaceBid.setText("Place Bid");
                    }
                    
                    if (result.success()) {
                        showSuccess("Bid placed successfully!");
                        loadAuctionData();
                    } else {
                        showError(result.errorMessage());
                    }
                });
            });

        } catch (NumberFormatException e) {
            showError("Invalid amount. Please enter a valid number.");
        }
    }

    private void startCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
        
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (timeLeftSeconds > 0) {
                timeLeftSeconds--;
                updateTimeLabel();
            } else {
                if(lblTimeLeft != null) lblTimeLeft.setText("00:00:00 (Ended)");
                countdownTimeline.stop();
                if(btnPlaceBid != null) btnPlaceBid.setDisable(true);
            }
        }));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
        updateTimeLabel();
    }

    private void updateTimeLabel() {
        if(lblTimeLeft == null) return;
        
        long hours = timeLeftSeconds / 3600;
        long minutes = (timeLeftSeconds % 3600) / 60;
        long seconds = timeLeftSeconds % 60;
        lblTimeLeft.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Success");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    @FXML
    public void handleBackToDashboard(ActionEvent event) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/frontendauction/dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to return to Dashboard: " + e.getMessage());
        }
    }
}
