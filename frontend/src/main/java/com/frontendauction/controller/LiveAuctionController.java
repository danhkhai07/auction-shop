package com.frontendauction.controller;

import com.frontendauction.AppWindow;
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
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;
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
    @FXML private ImageView imgProduct;
    @FXML private Label lblDescription;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTimeLeft;
    @FXML private CheckBox chkAutoBid;
    @FXML private TextField txtMaxAutoBid;
    @FXML private TextField txtBidAmount;
    @FXML private Button btnPlaceBid;
    @FXML private LineChart<String, Number> priceChart;
    @FXML private ListView<LiveAuctionModel.BidEntry> lvBidHistory;

    private final LiveAuctionService auctionService = new LiveAuctionService();

    private String currentAuctionId;
    private boolean initialLoadDone;
    private long timeLeftSeconds;
    private Timeline countdownTimeline;

    @FXML
    public void initialize() {
        if (txtMaxAutoBid != null && chkAutoBid != null) {
            txtMaxAutoBid.disableProperty().bind(chkAutoBid.selectedProperty().not());
        }

        if (btnPlaceBid != null) {
            btnPlaceBid.setOnAction(event -> handlePlaceBid());
        }

        if (priceChart != null) {
            priceChart.setAnimated(false);
            priceChart.setLegendVisible(false);
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

    private void loadAuctionData() {
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

                    currentAuctionId = normalizeAuctionId(auctions.getFirst().getId());
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

        if (lblSeller != null) {
            lblSeller.setText("Status: " + formatStatus(auction.getStatus())
                    + " | Auction ID: " + fallback(auction.getId(), "-"));
        }

        if (lblDescription != null) {
            lblDescription.setText(buildDescription(auction));
        }

        if (lblCurrentPrice != null) {
            lblCurrentPrice.setText(formatCurrency(auction.getCurrentPrice()));
        }

        timeLeftSeconds = auction.getRemainingSeconds();
        updateBidHistoryUI(auction.getBidHistory());
        updateBidAvailability(auction);
        startCountdown();
    }

    private String buildDescription(LiveAuctionModel.AuctionDetail auction) {
        StringBuilder builder = new StringBuilder();
        builder.append("Starting price: ").append(formatCurrency(auction.getStartingPrice())).append("\n");
        builder.append("End time: ").append(formatDateTime(auction.getEndTime())).append("\n");
        builder.append("Bid count: ")
                .append(auction.getBidHistory() == null ? 0 : auction.getBidHistory().size());
        return builder.toString();
    }

    private void updateBidHistoryUI(List<LiveAuctionModel.BidEntry> bids) {
        List<LiveAuctionModel.BidEntry> safeBids = bids == null ? List.of() : bids;

        if (lvBidHistory != null) {
            ObservableList<LiveAuctionModel.BidEntry> observableBids = FXCollections.observableArrayList(safeBids);
            lvBidHistory.setItems(observableBids);
        }

        if (priceChart != null) {
            priceChart.getData().clear();
            if (safeBids.isEmpty()) {
                return;
            }

            XYChart.Series<String, Number> series = new XYChart.Series<>();
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
            showSuccess("Bid placed successfully.");
            loadAuctionData();
            return;
        }

        showError(result.errorMessage());
    }

    private void startCountdown() {
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
        }));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
        updateTimeLabel();
    }

    private void stopCountdown() {
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

        if (chkAutoBid != null) {
            chkAutoBid.setDisable(!canBid);
            if (!canBid) {
                chkAutoBid.setSelected(false);
            }
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
        if (chkAutoBid != null && loading) {
            chkAutoBid.setDisable(true);
            chkAutoBid.setSelected(false);
        }
    }

    private void disableBidControls() {
        if (txtBidAmount != null) {
            txtBidAmount.setDisable(true);
        }
        if (chkAutoBid != null) {
            chkAutoBid.setDisable(true);
            chkAutoBid.setSelected(false);
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

        updateBidHistoryUI(List.of());
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

        if (!timestamp.contains("T")) {
            return timestamp;
        }

        String value = timestamp.substring(timestamp.indexOf('T') + 1);
        int millisIndex = value.indexOf('.');
        return millisIndex >= 0 ? value.substring(0, millisIndex) : value;
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

    @FXML
    public void handleBackToDashboard(ActionEvent event) {
        stopCountdown();
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
