package com.frontendauction.controller;

import com.frontendauction.service.UserProfileService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SellerControlPanelController extends LiveAuctionController {

    @FXML private Button btnPause;
    @FXML private Button btnUnpause;
    @FXML private TextField txtExtendTime;
    @FXML private Button btnExtend;
    @FXML private Button btnEnd;
    @FXML private Button btnCancel;

    private final UserProfileService userProfileService = new UserProfileService();

    @FXML
    @Override
    public void initialize() {
        super.initialize();

        if (btnPause != null) {
            btnPause.setOnAction(this::handlePauseAuction);
        }
        if (btnUnpause != null) {
            btnUnpause.setOnAction(this::handleUnpauseAuction);
        }
        if (btnExtend != null) {
            btnExtend.setOnAction(this::handleExtendAuction);
        }
        if (btnEnd != null) {
            btnEnd.setOnAction(this::handleEndAuction);
        }
        if (btnCancel != null) {
            btnCancel.setOnAction(this::handleCancelAuction);
        }
    }



    private void handlePauseAuction(ActionEvent event) {
        String currentAuctionId = getCurrentAuctionId();
        if (currentAuctionId == null || currentAuctionId.isBlank()) {
            showError("No auction is selected.");
            return;
        }
        userProfileService.pauseAuction(currentAuctionId)
                .thenAccept(ignored -> Platform.runLater(() -> {
                    stopCountdown();
                    showSuccess("Auction paused.");
                }))
                .exceptionally(exception -> {
                    Platform.runLater(() -> showError("Failed to pause: " + exception.getMessage()));
                    return null;
                });
    }

    private void handleUnpauseAuction(ActionEvent event) {
        String currentAuctionId = getCurrentAuctionId();
        if (currentAuctionId == null || currentAuctionId.isBlank()) {
            showError("No auction is selected.");
            return;
        }
        userProfileService.unpauseAuction(currentAuctionId)
                .thenAccept(ignored -> Platform.runLater(() -> {
                    loadAuctionData();
                    showSuccess("Auction unpaused.");
                }))
                .exceptionally(exception -> {
                    Platform.runLater(() -> showError("Failed to unpause: " + exception.getMessage()));
                    return null;
                });
    }

    private void handleExtendAuction(ActionEvent event) {
        String currentAuctionId = getCurrentAuctionId();
        if (currentAuctionId == null || currentAuctionId.isBlank()) {
            showError("No auction is selected.");
            return;
        }
        if (txtExtendTime == null || txtExtendTime.getText().isBlank()) {
            showError("Please enter minutes to extend.");
            return;
        }
        try {
            int minutes = Integer.parseInt(txtExtendTime.getText().trim());
            long timeLeft = getTimeLeftSeconds();
            LocalDateTime newEndTime = LocalDateTime.now().plusSeconds(timeLeft).plusMinutes(minutes);
            String newEndTimeStr = newEndTime.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"));
            
            userProfileService.extendAuctionTime(currentAuctionId, newEndTimeStr)
                    .thenAccept(ignored -> Platform.runLater(() -> {
                        loadAuctionData();
                        showSuccess("Auction time extended.");
                    }))
                    .exceptionally(exception -> {
                        Platform.runLater(() -> showError("Failed to extend time: " + exception.getMessage()));
                        return null;
                    });
        } catch (NumberFormatException e) {
            showError("Invalid minutes value.");
        }
    }

    private void handleEndAuction(ActionEvent event) {
        String currentAuctionId = getCurrentAuctionId();
        if (currentAuctionId == null || currentAuctionId.isBlank()) {
            showError("No auction is selected.");
            return;
        }
        userProfileService.endAuction(currentAuctionId)
                .thenAccept(ignored -> Platform.runLater(() -> {
                    stopCountdown();
                    showSuccess("Auction ended early. Status will be changed to FINISHED.");
                    handleBackToDashboard(event);
                }))
                .exceptionally(exception -> {
                    Platform.runLater(() -> showError("Failed to end auction: " + exception.getMessage()));
                    return null;
                });
    }

    private void handleCancelAuction(ActionEvent event) {
        String currentAuctionId = getCurrentAuctionId();
        if (currentAuctionId == null || currentAuctionId.isBlank()) {
            showError("No auction is selected.");
            return;
        }
        userProfileService.cancelAuction(currentAuctionId)
                .thenAccept(ignored -> Platform.runLater(() -> {
                    stopCountdown();
                    showSuccess("Auction cancelled.");
                    handleBackToDashboard(event);
                }))
                .exceptionally(exception -> {
                    Platform.runLater(() -> showError("Failed to cancel auction: " + exception.getMessage()));
                    return null;
                });
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
}
