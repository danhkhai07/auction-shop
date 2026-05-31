package com.frontendauction.controller;

import com.frontendauction.AppWindow;
import com.frontendauction.model.AuctionEventData;
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
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;

import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.CompletableFuture;

public class LiveAuctionController {

    private static final DateTimeFormatter DISPLAY_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    @FXML private AnchorPane rootPane;
    @FXML private Button btnBack;
    @FXML private Label lblProductName;
    @FXML private Label lblSeller;
    @FXML private Label lblLiveBalance;

    @FXML private Label lblDescription;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTimeLeft;

    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private ListView<LiveAuctionModel.BidEntry> lvBidHistory;

    private final LiveAuctionService auctionService = new LiveAuctionService();
    private final com.frontendauction.service.UserProfileService userProfileService = new com.frontendauction.service.UserProfileService();
    private final com.frontendauction.service.ReviewService reviewService = new com.frontendauction.service.ReviewService();

    private String currentAuctionId;
    private String currentSellerName;
    private boolean initialLoadDone;
    private boolean hasPlacedBid;
    private long timeLeftSeconds;
    private Timeline countdownTimeline;

    protected String getCurrentAuctionId() {
        return currentAuctionId;
    }

    protected long getTimeLeftSeconds() {
        return timeLeftSeconds;
    }

    @FXML
    public void initialize() {
        if (btnPlaceBid != null) {
            btnPlaceBid.setOnAction(event -> handlePlaceBid());
        }

        if (priceChart != null) {
            priceChart.setAnimated(false);
            priceChart.setLegendVisible(false);
            if (priceChart.getYAxis() instanceof javafx.scene.chart.NumberAxis) {
                javafx.scene.chart.NumberAxis yAxis = (javafx.scene.chart.NumberAxis) priceChart.getYAxis();
                yAxis.setTickLabelFormatter(new javafx.util.StringConverter<Number>() {
                    @Override
                    public String toString(Number object) {
                        return String.format(Locale.US, "%,d", object.longValue());
                    }
                    @Override
                    public Number fromString(String string) {
                        return 0;
                    }
                });
            }
        }
        


        Platform.runLater(() -> {
            if (!initialLoadDone) {
                loadAuctionData();
            }
        });
    }

    public void setAuctionId(String auctionId) {
        currentAuctionId = normalizeAuctionId(auctionId);
        if (initialLoadDone || (rootPane != null && rootPane.getScene() != null)) {
            loadAuctionData();
        }
    }

    protected void loadAuctionData() {
        initialLoadDone = true;
        setLoadingState(true);

        resolveAuctionDetail()
                .thenAccept(auction -> Platform.runLater(() -> {
                    setLoadingState(false);
                    updateAuctionUI(auction);
                }))
                .exceptionally(exception -> {
                    Platform.runLater(() -> {
                        setLoadingState(false);
                        showAuctionUnavailable(resolveErrorMessage(exception));
                    });
                    return null;
                });
    }

    private CompletableFuture<LiveAuctionModel.AuctionDetail> resolveAuctionDetail() {
        if (currentAuctionId != null && !currentAuctionId.isBlank()) {
            return auctionService.getAuctionDetails(currentAuctionId).thenCompose(this::requireAuction);
        }

        return auctionService.getActiveAuctions()
                .thenCompose(auctions -> {
                    if (auctions == null || auctions.isEmpty()) {
                        return CompletableFuture.failedFuture(
                                new IllegalStateException("No active auctions are available.")
                        );
                    }

                    currentAuctionId = normalizeAuctionId(auctions.get(0).getId());
                    return auctionService.getAuctionDetails(currentAuctionId).thenCompose(this::requireAuction);
                });
    }

    private CompletableFuture<LiveAuctionModel.AuctionDetail> requireAuction(LiveAuctionModel.AuctionDetail auction) {
        if (auction == null) {
            return CompletableFuture.failedFuture(new IllegalStateException("Auction data is unavailable."));
        }
        return CompletableFuture.completedFuture(auction);
    }

    private void updateAuctionUI(LiveAuctionModel.AuctionDetail auction) {
        currentAuctionId = normalizeAuctionId(auction.getId());

        if (lblProductName != null) {
            lblProductName.setText(fallback(auction.getName(), "Unnamed auction"));
        }

        currentSellerName = (auction.getSeller() != null && auction.getSeller().getUsername() != null)
                ? auction.getSeller().getUsername() : "Unknown";

        if (lblSeller != null) {
            lblSeller.setText("Seller: " + currentSellerName + " | Status: " + formatStatus(auction.getStatus())
                    + " | Auction ID: " + fallback(auction.getId(), "-"));
        }

        // Load real seller reputation from API
        if (!"Unknown".equals(currentSellerName)) {
            reviewService.getReviewsForUser(currentSellerName)
                .thenAccept(reviews -> Platform.runLater(() -> {
                    double sum = 0;
                    int count = 0;
                    for (com.frontendauction.model.ReviewModel r : reviews) {
                        if (r.getTargetUser() != null && r.getTargetUser().equalsIgnoreCase(currentSellerName)) {
                            sum += r.getStars();
                            count++;
                        }
                    }
                    String repStr = count > 0 ? String.format(Locale.US, " (%.1f \u2605)", sum / count) : " (- \u2605)";
                    if (lblSeller != null) {
                        lblSeller.setText("Seller: " + currentSellerName + repStr + " | Status: " + formatStatus(auction.getStatus())
                                + " | Auction ID: " + fallback(auction.getId(), "-"));
                    }
                }));
        }

        if (lblDescription != null) {
            lblDescription.setText(buildDescription(auction));
        }

        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(formatCurrency(auction.getCurrentPrice()));
        }

        timeLeftSeconds = auction.getRemainingSeconds();
        updateBidHistoryUI(auction);
        updateBidAvailability(auction);
        startCountdown();
        connectSseStream();
        loadWalletBalance();
    }

    private void loadWalletBalance() {
        if (!TokenStore.hasToken()) {
            if (lblLiveBalance != null) lblLiveBalance.setText("-");
            return;
        }
        userProfileService.getBalance()
                .thenAccept(balanceResponse -> Platform.runLater(() -> {
                    if (lblLiveBalance != null) {
                        lblLiveBalance.setText(formatCurrency(balanceResponse.getBalance()));
                    }
                }))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        if (lblLiveBalance != null) lblLiveBalance.setText("Error");
                    });
                    return null;
                });
    }

    private void connectSseStream() {
        if (currentAuctionId == null || currentAuctionId.isBlank()) return;

        auctionService.connectToEventStream(
                currentAuctionId,
                event -> Platform.runLater(() -> handleSseEvent(event)),
                () -> System.err.println("SSE stream connection failed for auction: " + currentAuctionId)
        );
    }

    private void handleSseEvent(AuctionEventData event) {
        if (event == null || event.getType() == null) return;

        String type = event.getType();
        System.out.println("[SSE] Event received: " + type);

        switch (type) {
            case "BID_PLACED" -> {
                // Update current price
                if (event.getCurrentHighestPrice() != null && lblCurrentPrice != null) {
                    lblCurrentPrice.setText(formatCurrency(event.getCurrentHighestPrice()));
                }
                // Reload full auction data to refresh bid history + chart
                if (currentAuctionId != null) {
                    auctionService.getAuctionDetails(currentAuctionId)
                            .thenAccept(auction -> Platform.runLater(() -> {
                                updateBidHistoryUI(auction);
                                updateBidAvailability(auction);
                            }))
                            .exceptionally(ex -> { ex.printStackTrace(); return null; });
                }

            }
            case "AUCTION_FINISHED", "AUCTION_CANCELLED" -> {
                timeLeftSeconds = 0;
                updateTimeLabel();
                disableBidControls();
                stopCountdown();
                if (lblSeller != null) {
                    // Load real seller reputation
                    final String sellerName = fallback(currentSellerName, "Unknown");
                    if (!"Unknown".equals(sellerName)) {
                        reviewService.getReviewsForUser(sellerName)
                            .thenAccept(reviews -> Platform.runLater(() -> {
                                double sum = 0;
                                int count = 0;
                                for (com.frontendauction.model.ReviewModel r : reviews) {
                                    if (r.getTargetUser() != null && r.getTargetUser().equalsIgnoreCase(sellerName)) {
                                        sum += r.getStars();
                                        count++;
                                    }
                                }
                                String repStr = count > 0 ? String.format(Locale.US, " (%.1f \u2605)", sum / count) : " (- \u2605)";
                                lblSeller.setText("Seller: " + sellerName + repStr + " | Status: " + type.replace("AUCTION_", "") + " | Auction ID: " + fallback(currentAuctionId, "-"));
                            }));
                    } else {
                        lblSeller.setText("Seller: " + sellerName + " | Status: " + type.replace("AUCTION_", "") + " | Auction ID: " + fallback(currentAuctionId, "-"));
                    }
                }
                
                if ("AUCTION_FINISHED".equals(type) && event.getCurrentHighestBidder() != null && event.getFinalPrice() != null) {
                    String winnerName = event.getCurrentHighestBidder().getUsername();
                    Double finalPrice = event.getFinalPrice();
                    
                    // Trigger rating popup if current user has bid in this auction
                    userProfileService.getCurrentUser().thenAccept(user -> Platform.runLater(() -> {
                        if (user == null) return;
                        boolean isBidder = hasPlacedBid;
                        
                        showSuccess("Phiên đấu giá đã kết thúc!\nNgười chiến thắng: " + winnerName + "\nVới giá: " + formatCurrency(finalPrice));
                        
                        if (isBidder) {
                            showRatingDialog(currentSellerName, "Người bán");
                        }
                    })).exceptionally(e -> {
                        Platform.runLater(() -> showSuccess("Phiên đấu giá đã kết thúc!\nNgười chiến thắng: " + winnerName + "\nVới giá: " + formatCurrency(finalPrice)));
                        return null;
                    });
                } else if ("AUCTION_FINISHED".equals(type)) {
                    showSuccess("Phiên đấu giá đã kết thúc!\nKhông có người chiến thắng (Chưa có ai đặt giá).");
                }
            }
            case "ANTI_SNIPE_AUCTION_EXTENDED", "AUCTION_EXTENDED" -> {
                // Recalculate remaining time from new endTime
                if (event.getEndTime() != null) {
                    try {
                        String normalized = event.getEndTime().replace("Z", "");
                        int tzIndex = normalized.lastIndexOf('+');
                        if (tzIndex > 10) normalized = normalized.substring(0, tzIndex);
                        int minusTzIndex = normalized.lastIndexOf('-');
                        if (minusTzIndex > 10) normalized = normalized.substring(0, minusTzIndex);
                        LocalDateTime end = LocalDateTime.parse(normalized);
                        long seconds = java.time.temporal.ChronoUnit.SECONDS.between(LocalDateTime.now(), end);
                        timeLeftSeconds = Math.max(0, seconds);
                        updateTimeLabel();
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
            case "AUCTION_STARTED", "AUCTION_UNPAUSED" -> {
                // Reload full data to get updated status
                loadAuctionData();
            }
            case "AUCTION_PAUSED" -> {
                disableBidControls();
                if (lblSeller != null) {
                    lblSeller.setText("Seller: " + fallback(currentSellerName, "Unknown") + " | Status: PAUSED | Auction ID: " + fallback(currentAuctionId, "-"));
                }
            }
        }
    }



    private String buildDescription(LiveAuctionModel.AuctionDetail auction) {
        StringBuilder builder = new StringBuilder();
        if (auction.getDescription() != null && !auction.getDescription().isBlank()) {
            builder.append(auction.getDescription()).append("\n\n");
        }
        builder.append("Starting price: ").append(formatCurrency(auction.getStartingPrice())).append("\n");
        builder.append("End time: ").append(formatDateTime(auction.getEndTime())).append("\n");
        builder.append("Bid count: ")
                .append(auction.getBidHistory() == null ? 0 : auction.getBidHistory().size());
        return builder.toString();
    }

    private void updateBidHistoryUI(LiveAuctionModel.AuctionDetail auction) {
        List<LiveAuctionModel.BidEntry> bids = auction != null ? auction.getBidHistory() : null;
        List<LiveAuctionModel.BidEntry> safeBids = bids == null ? List.of() : bids;

        if (lvBidHistory != null) {
            ObservableList<LiveAuctionModel.BidEntry> observableBids = FXCollections.observableArrayList(safeBids);
            lvBidHistory.setItems(observableBids);
        }

        if (priceChart != null) {
            priceChart.getData().clear();
            if (auction == null && safeBids.isEmpty()) {
                return;
            }

            XYChart.Series<String, Number> series = new XYChart.Series<>();

            if (auction != null && auction.getStartingPrice() != null) {
                String startTimeStr = formatBidTime(auction.getStartTime());
                if (startTimeStr.equals("-")) {
                    startTimeStr = "Start";
                }
                series.getData().add(new XYChart.Data<>(startTimeStr, auction.getStartingPrice()));
            }

            for (LiveAuctionModel.BidEntry bid : safeBids) {
                if (bid.getBidAmount() == null) {
                    continue;
                }
                series.getData().add(new XYChart.Data<>(formatBidTime(bid.getTimestamp()), bid.getBidAmount()));
            }
            priceChart.getData().add(series);
        }
    }

    private void handlePlaceBid() {
        if (txtBidAmount == null) {
            return;
        }

        if (currentAuctionId == null || currentAuctionId.isBlank()) {
            showError("No auction is selected.");
            return;
        }

        String amountText = txtBidAmount.getText().trim();
        if (amountText.isEmpty()) {
            showError("Please enter a bid amount.");
            return;
        }

        if (!TokenStore.hasToken()) {
            showError("You must login before placing a bid.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountText);
            txtBidAmount.clear();

            if (btnPlaceBid != null) {
                btnPlaceBid.setDisable(true);
                btnPlaceBid.setText("Sending...");
            }

            LiveAuctionModel.BidRequest request = new LiveAuctionModel.BidRequest(amount);
            auctionService.placeBid(currentAuctionId, request)
                    .thenAccept(result -> Platform.runLater(() -> handleBidResult(result)))
                    .exceptionally(exception -> {
                        Platform.runLater(() -> {
                            resetBidButton();
                            showError(resolveErrorMessage(exception));
                        });
                        return null;
                    });
        } catch (NumberFormatException exception) {
            showError("Invalid amount. Please enter a valid number.");
        }
    }

    private void handleBidResult(BidResult result) {
        resetBidButton();

        if (result.success()) {
            hasPlacedBid = true;
            showSuccess("Bid placed successfully.");
            loadAuctionData();
            loadWalletBalance();
            return;
        }

        showError(result.errorMessage());
    }

    protected void startCountdown() {
        stopCountdown();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            if (timeLeftSeconds > 0) {
                timeLeftSeconds--;
                updateTimeLabel();
                return;
            }

            updateTimeLabel();
            disableBidControls();
            countdownTimeline.stop();
            onTimeExpired();
        }));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
        updateTimeLabel();
    }

    protected void onTimeExpired() {
        // To be overridden by subclasses
    }

    protected void stopCountdown() {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
    }

    private void updateTimeLabel() {
        if (lblTimeLeft == null) {
            return;
        }

        long hours = timeLeftSeconds / 3600;
        long minutes = (timeLeftSeconds % 3600) / 60;
        long seconds = timeLeftSeconds % 60;

        if (timeLeftSeconds <= 0) {
            lblTimeLeft.setText("00:00:00");
            return;
        }

        lblTimeLeft.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void updateBidAvailability(LiveAuctionModel.AuctionDetail auction) {
        boolean canBid = canBid(auction);

        if (txtBidAmount != null) {
            txtBidAmount.setDisable(!canBid);
        }


        if (btnPlaceBid != null) {
            btnPlaceBid.setDisable(!canBid);
            btnPlaceBid.setText("Place Bid");
        }
    }

    private boolean canBid(LiveAuctionModel.AuctionDetail auction) {
        return auction != null
                && "RUNNING".equalsIgnoreCase(auction.getStatus())
                && auction.getRemainingSeconds() > 0;
    }

    private void setLoadingState(boolean loading) {
        if (lblProductName != null && loading) {
            lblProductName.setText("Loading auction...");
        }
        if (lblSeller != null && loading) {
            lblSeller.setText("Fetching auction data from API...");
        }
        if (btnPlaceBid != null && loading) {
            btnPlaceBid.setDisable(true);
            btnPlaceBid.setText("Loading...");
        }
        if (txtBidAmount != null && loading) {
            txtBidAmount.setDisable(true);
        }

    }

    private void disableBidControls() {
        if (txtBidAmount != null) {
            txtBidAmount.setDisable(true);
        }

        if (btnPlaceBid != null) {
            btnPlaceBid.setDisable(true);
            btnPlaceBid.setText("Auction ended");
        }
    }

    private void resetBidButton() {
        if (btnPlaceBid != null) {
            btnPlaceBid.setDisable(false);
            btnPlaceBid.setText("Place Bid");
        }
    }

    private void showAuctionUnavailable(String message) {
        stopCountdown();
        currentAuctionId = null;
        timeLeftSeconds = 0;

        if (lblProductName != null) {
            lblProductName.setText("No active auction");
        }
        if (lblSeller != null) {
            lblSeller.setText(message);
        }
        if (lblDescription != null) {
            lblDescription.setText("The API did not return an auction that can be displayed.");
        }
        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText("-");
        }
        if (lblTimeLeft != null) {
            lblTimeLeft.setText("00:00:00");
        }

        updateBidHistoryUI(null);
        disableBidControls();
    }

    private String formatCurrency(Double amount) {
        return amount == null ? "-" : String.format(Locale.US, "%,.0f VND", amount);
    }

    private String formatStatus(String status) {
        return status == null || status.isBlank() ? "Unknown" : status;
    }

    private String formatDateTime(String rawDateTime) {
        if (rawDateTime == null || rawDateTime.isBlank()) {
            return "-";
        }

        try {
            return LocalDateTime.parse(rawDateTime).format(DISPLAY_TIME);
        } catch (Exception ignored) {
            return rawDateTime;
        }
    }

    private String formatBidTime(String timestamp) {
        if (timestamp == null || timestamp.isBlank()) {
            return "-";
        }

        try {
            String normalized = timestamp.replace("Z", "");
            int tzIndex = normalized.lastIndexOf('+');
            if (tzIndex > 10) normalized = normalized.substring(0, tzIndex);
            int minusTzIndex = normalized.lastIndexOf('-');
            if (minusTzIndex > 10) normalized = normalized.substring(0, minusTzIndex);
            
            java.time.LocalDateTime dt = java.time.LocalDateTime.parse(normalized);
            dt = dt.plusHours(7);
            
            return dt.format(java.time.format.DateTimeFormatter.ofPattern("HH:mm:ss"));
        } catch (Exception e) {
            if (!timestamp.contains("T")) {
                return timestamp;
            }
            String value = timestamp.substring(timestamp.indexOf('T') + 1);
            int millisIndex = value.indexOf('.');
            return millisIndex >= 0 ? value.substring(0, millisIndex) : value;
        }
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String normalizeAuctionId(String auctionId) {
        if (auctionId == null) {
            return null;
        }
        String trimmed = auctionId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String resolveErrorMessage(Throwable exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        String message = cause.getMessage();
        return message == null || message.isBlank() ? "Unable to load auction data." : message;
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

    private void showRatingDialog(String targetUser, String role) {
        javafx.scene.control.Dialog<Void> dialog = new javafx.scene.control.Dialog<>();
        dialog.setTitle("Đánh giá " + role);
        dialog.setHeaderText("Vui lòng đánh giá " + targetUser + " cho giao dịch này");

        javafx.scene.control.ButtonType submitButtonType = new javafx.scene.control.ButtonType("Gửi đánh giá", javafx.scene.control.ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(submitButtonType, javafx.scene.control.ButtonType.CANCEL);

        javafx.scene.layout.VBox vbox = new javafx.scene.layout.VBox();
        vbox.setSpacing(10);

        javafx.scene.layout.HBox starBox = new javafx.scene.layout.HBox();
        starBox.setSpacing(5);
        final int[] selectedRating = {5};
        Label[] stars = new Label[5];
        for (int i = 0; i < 5; i++) {
            Label star = new Label("★");
            star.setStyle("-fx-font-size: 32px; -fx-text-fill: gold; -fx-cursor: hand;");
            final int index = i;
            star.setOnMouseClicked(e -> {
                selectedRating[0] = index + 1;
                for (int j = 0; j < 5; j++) {
                    stars[j].setStyle("-fx-font-size: 32px; -fx-text-fill: " + (j <= index ? "gold" : "gray") + "; -fx-cursor: hand;");
                }
            });
            stars[i] = star;
            starBox.getChildren().add(star);
        }

        javafx.scene.control.TextArea commentArea = new javafx.scene.control.TextArea();
        commentArea.setPromptText("Nhập nhận xét của bạn...");
        commentArea.setPrefRowCount(3);

        vbox.getChildren().addAll(new Label("Đánh giá bằng sao:"), starBox, new Label("Nhận xét:"), commentArea);
        dialog.getDialogPane().setContent(vbox);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == submitButtonType) {
                System.out.println("Mock Rating submitted: " + selectedRating[0] + " stars for " + targetUser + ". Comment: " + commentArea.getText());
                
                // Save real rating
                reviewService.submitReview(targetUser, selectedRating[0], commentArea.getText())
                    .thenAccept(v -> {
                        System.out.println("Review submitted successfully for " + targetUser);
                        Platform.runLater(() -> showSuccess("Đánh giá đã được lưu thành công!"));
                    })
                    .exceptionally(e -> {
                        System.err.println("Failed to submit review: " + e.getMessage());
                        e.printStackTrace();
                        Platform.runLater(() -> showError("Lỗi khi gửi đánh giá: " + e.getMessage()));
                        return null;
                    });

                showSuccess("Cảm ơn bạn đã gửi đánh giá (" + selectedRating[0] + " \u2605) cho " + targetUser + "!");
            }
            return null;
        });

        dialog.showAndWait();
    }

    @FXML
    public void handleBackToDashboard(ActionEvent event) {
        stopCountdown();
        auctionService.disconnectEventStream();
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/frontendauction/dashboard.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
            AppWindow.applyScene(stage, root);
            stage.show();
        } catch (IOException exception) {
            exception.printStackTrace();
            showError("Failed to return to Dashboard: " + exception.getMessage());
        }
    }


}
