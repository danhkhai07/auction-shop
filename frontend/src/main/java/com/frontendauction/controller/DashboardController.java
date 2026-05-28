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
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.AnchorPane;
import javafx.scene.layout.VBox;
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
    @FXML private Button btnHamburger;
    @FXML private AnchorPane menuOverlay;
    @FXML private VBox sideMenu;
    @FXML private VBox auctionListContainer;
    @FXML private Button btnAdminPanel;

    private final UserProfileService userProfileService = new UserProfileService();
    private final LiveAuctionService liveAuctionService = new LiveAuctionService();

    private String firstActiveAuctionId;

    @FXML
    public void initialize() {
        loadDashboardData();
    }

    @FXML
    public void toggleMenu() {
        if (menuOverlay != null) {
            menuOverlay.setVisible(!menuOverlay.isVisible());
        }
    }

    @FXML
    public void closeMenu() {
        if (menuOverlay != null) {
            menuOverlay.setVisible(false);
        }
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

    @FXML
    public void goToAdminPanel(Event event) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/com/frontendauction/admin_dashboard.fxml")));
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
        
        if (activeAuctions != null) {
            activeAuctions = activeAuctions.stream()
                    .filter(a -> a.getRemainingSeconds() > 0)
                    .toList();
        }
        
        int ownedItems = safeSize(user.getItemList());
        int ownedAuctions = safeSize(user.getAuctionList());
        int activeCount = activeAuctions == null ? 0 : activeAuctions.size();

        firstActiveAuctionId = activeCount > 0 ? activeAuctions.getFirst().getId() : null;

        lblWelcome.setText("Welcome, " + fallback(user.getUsername(), "user"));
        lblSummary.setText("User ID: " + fallback(user.getId(), "-") + " | Roles: " + joinRoles(user.getRoles()));
        if (lblProfileMeta != null) lblProfileMeta.setText(ownedItems + " item(s), " + ownedAuctions + " auction(s)");
        if (lblAuctionMeta != null) lblAuctionMeta.setText(activeCount > 0 ? activeCount + " active auction(s)" : "No active auctions");
        if (lblProductMeta != null) lblProductMeta.setText(ownedItems > 0 ? ownedItems + " product(s) managed" : "No owned products");

        if (btnAdminPanel != null && user.getRoles() != null && user.getRoles().contains("ADMIN")) {
            btnAdminPanel.setVisible(true);
            btnAdminPanel.setManaged(true);
        }

        loadAuctionCards(activeAuctions);
    }

    private void loadAuctionCards(List<LiveAuctionModel.AuctionDetail> auctions) {
        if (auctionListContainer == null) return;
        auctionListContainer.getChildren().clear();
        if (auctions == null || auctions.isEmpty()) {
            Label noAuction = new Label("No auctions available.");
            auctionListContainer.getChildren().add(noAuction);
            return;
        }
        for (LiveAuctionModel.AuctionDetail auction : auctions) {
            try {
                FXMLLoader loader = new FXMLLoader(getClass().getResource("/com/frontendauction/auction_card.fxml"));
                Node card = loader.load();
                AuctionCardController controller = loader.getController();
                controller.setAuction(auction);
                auctionListContainer.getChildren().add(card);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void showLoadFailure(String message) {
        lblWelcome.setText("Dashboard");
        lblSummary.setText(message);
        if (lblProfileMeta != null) lblProfileMeta.setText("Unable to load profile");
        if (lblAuctionMeta != null) lblAuctionMeta.setText("Unable to load auctions");
        if (lblProductMeta != null) lblProductMeta.setText("Unable to load products");
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
