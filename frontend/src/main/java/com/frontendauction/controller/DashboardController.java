package com.frontendauction.controller;

import com.frontendauction.AppWindow;
import com.frontendauction.model.LiveAuctionModel;
import com.frontendauction.model.UserProfileModel;
import com.frontendauction.service.LiveAuctionService;
import com.frontendauction.service.TokenStore;
import com.frontendauction.service.UserProfileService;
import javafx.application.Platform;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Label;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class DashboardController {

    @FXML private Label lblWelcome;
    @FXML private Label lblSummary;
    @FXML private Label lblProfileMeta;
    @FXML private Label lblAuctionMeta;
    @FXML private Label lblProductMeta;

    private final UserProfileService userProfileService = new UserProfileService();
    private final LiveAuctionService liveAuctionService = new LiveAuctionService();

    private String firstActiveAuctionId;

    @FXML
    public void initialize() {
        loadDashboardData();
    }

    @FXML
    public void goToLiveAuction(Event event) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/com/frontendauction/live-auction.fxml")));
        Parent root = loader.load();

        LiveAuctionController controller = loader.getController();
        if (firstActiveAuctionId != null && !firstActiveAuctionId.isBlank()) {
            controller.setAuctionId(firstActiveAuctionId);
        }

        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
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

    private void loadDashboardData() {
        userProfileService.getCurrentUser()
                .thenCombine(liveAuctionService.getActiveAuctions(), DashboardData::new)
                .thenAccept(data -> Platform.runLater(() -> updateDashboard(data)))
                .exceptionally(exception -> {
                    Platform.runLater(() -> showLoadFailure(resolveErrorMessage(exception)));
                    return null;
                });
    }

    private void updateDashboard(DashboardData data) {
        UserProfileModel user = data.user();
        List<LiveAuctionModel.AuctionDetail> activeAuctions = data.activeAuctions();
        int ownedItems = safeSize(user.getItemList());
        int ownedAuctions = safeSize(user.getAuctionList());
        int activeCount = activeAuctions == null ? 0 : activeAuctions.size();

        firstActiveAuctionId = activeCount > 0 ? activeAuctions.getFirst().getId() : null;

        lblWelcome.setText("Welcome, " + fallback(user.getUsername(), "user"));
        lblSummary.setText("User ID: " + fallback(user.getId(), "-") + " | Roles: " + joinRoles(user.getRoles()));
        lblProfileMeta.setText(ownedItems + " item(s), " + ownedAuctions + " auction(s)");
        lblAuctionMeta.setText(activeCount > 0 ? activeCount + " active auction(s)" : "No active auctions");
        lblProductMeta.setText(ownedItems > 0 ? ownedItems + " product(s) managed" : "No owned products");
    }

    private void showLoadFailure(String message) {
        lblWelcome.setText("Dashboard");
        lblSummary.setText(message);
        lblProfileMeta.setText("Unable to load profile");
        lblAuctionMeta.setText("Unable to load auctions");
        lblProductMeta.setText("Unable to load products");
        firstActiveAuctionId = null;
    }

    private String joinRoles(Set<String> roles) {
        return roles == null || roles.isEmpty() ? "none" : String.join(", ", roles);
    }

    private int safeSize(List<String> values) {
        return values == null ? 0 : values.size();
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String resolveErrorMessage(Throwable exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        String message = cause.getMessage();
        return message == null || message.isBlank() ? "Unable to load dashboard data." : message;
    }

    private record DashboardData(UserProfileModel user, List<LiveAuctionModel.AuctionDetail> activeAuctions) {
    }
}
