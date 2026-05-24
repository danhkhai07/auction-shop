package com.frontendauction.controller;

import com.frontendauction.AppWindow;
import com.frontendauction.model.LiveAuctionModel;
import com.frontendauction.model.ProductManagementModel;
import com.frontendauction.model.UserProfileModel;
import com.frontendauction.service.TokenStore;
import com.frontendauction.service.UserProfileService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.event.Event;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Objects;
import java.util.Set;

public class ProfileController {

    @FXML private Label lblUsername;
    @FXML private Label lblUserId;
    @FXML private Label lblRoles;
    @FXML private Label lblItemCount;
    @FXML private Label lblAuctionCount;

    @FXML private TableView<ProductManagementModel> tvOwnedItems;
    @FXML private TableColumn<ProductManagementModel, String> colOwnedItemId;
    @FXML private TableColumn<ProductManagementModel, String> colOwnedItemName;
    @FXML private TableColumn<ProductManagementModel, String> colOwnedItemDescription;

    @FXML private TableView<LiveAuctionModel.AuctionDetail> tvOwnedAuctions;
    @FXML private TableColumn<LiveAuctionModel.AuctionDetail, String> colOwnedAuctionId;
    @FXML private TableColumn<LiveAuctionModel.AuctionDetail, String> colOwnedAuctionName;
    @FXML private TableColumn<LiveAuctionModel.AuctionDetail, Double> colOwnedAuctionPrice;
    @FXML private TableColumn<LiveAuctionModel.AuctionDetail, String> colOwnedAuctionStatus;
    @FXML private TableColumn<LiveAuctionModel.AuctionDetail, Void> colOwnedAuctionAction;

    private final UserProfileService userProfileService = new UserProfileService();

    @FXML
    public void initialize() {
        setupTables();
        loadProfile();
    }

    @FXML
    public void goBack(Event event) throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/com/frontendauction/dashboard.fxml")));
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

    private void setupTables() {
        colOwnedItemId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOwnedItemName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colOwnedItemDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        colOwnedAuctionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colOwnedAuctionName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colOwnedAuctionPrice.setCellValueFactory(new PropertyValueFactory<>("currentPrice"));
        colOwnedAuctionStatus.setCellValueFactory(new PropertyValueFactory<>("status"));

        colOwnedAuctionPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(Double value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setText(null);
                    return;
                }
                setText(value == null ? "-" : String.format("%,.0f VND", value));
            }
        });

        colOwnedAuctionAction.setCellFactory(column -> new TableCell<>() {
            private final Button btnStart = new Button("Start");

            {
                btnStart.setStyle("-fx-background-color: #22c55e; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4; -fx-font-weight: bold;");
                btnStart.setOnAction(event -> {
                    LiveAuctionModel.AuctionDetail auction = getTableView().getItems().get(getIndex());
                    if (auction != null) {
                        startAuction(auction.getId());
                    }
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || getTableView().getItems().get(getIndex()) == null) {
                    setGraphic(null);
                } else {
                    LiveAuctionModel.AuctionDetail auction = getTableView().getItems().get(getIndex());
                    if ("OPEN".equalsIgnoreCase(auction.getStatus())) {
                        setGraphic(btnStart);
                    } else {
                        setGraphic(null);
                    }
                }
            }
        });
    }

    private void loadProfile() {
        userProfileService.getCurrentUser()
                .thenCompose(user -> {
                    Platform.runLater(() -> updateUserInfo(user));
                    return userProfileService.getOwnedItems(user)
                            .thenCombine(userProfileService.getOwnedAuctions(user),
                                    (items, auctions) -> new ProfileData(user, items, auctions));
                })
                .thenAccept(data -> Platform.runLater(() -> updateTables(data)))
                .exceptionally(exception -> {
                    Platform.runLater(() -> showLoadFailure(resolveErrorMessage(exception)));
                    return null;
                });
    }

    private void updateUserInfo(UserProfileModel user) {
        lblUsername.setText(fallback(user.getUsername(), "Unknown user"));
        lblUserId.setText("User ID: " + fallback(user.getId(), "-"));
        lblRoles.setText("Roles: " + joinRoles(user.getRoles()));
        lblItemCount.setText(String.valueOf(sizeOf(user.getItemList())));
        lblAuctionCount.setText(String.valueOf(sizeOf(user.getAuctionList())));
    }

    private void updateTables(ProfileData data) {
        tvOwnedItems.setItems(FXCollections.observableArrayList(data.items()));
        tvOwnedAuctions.setItems(FXCollections.observableArrayList(data.auctions()));
        lblItemCount.setText(String.valueOf(data.items().size()));
        lblAuctionCount.setText(String.valueOf(data.auctions().size()));
    }

    private void showLoadFailure(String message) {
        lblUsername.setText("Unable to load profile");
        lblUserId.setText(message);
        lblRoles.setText("Roles: -");
        lblItemCount.setText("0");
        lblAuctionCount.setText("0");
        tvOwnedItems.setItems(FXCollections.observableArrayList());
        tvOwnedAuctions.setItems(FXCollections.observableArrayList());

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Profile");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void startAuction(String auctionId) {
        userProfileService.startAuction(auctionId)
                .thenAccept(v -> Platform.runLater(this::loadProfile))
                .exceptionally(ex -> {
                    Platform.runLater(() -> {
                        Alert alert = new Alert(Alert.AlertType.ERROR);
                        alert.setTitle("Start Auction Error");
                        alert.setHeaderText(null);
                        alert.setContentText(resolveErrorMessage(ex));
                        alert.showAndWait();
                    });
                    return null;
                });
    }

    private String joinRoles(Set<String> roles) {
        return roles == null || roles.isEmpty() ? "none" : String.join(", ", roles);
    }

    private int sizeOf(List<String> values) {
        return values == null ? 0 : values.size();
    }

    private String fallback(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String resolveErrorMessage(Throwable exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        String message = cause.getMessage();
        return message == null || message.isBlank() ? "Unable to load profile data." : message;
    }

    private record ProfileData(
            UserProfileModel user,
            List<ProductManagementModel> items,
            List<LiveAuctionModel.AuctionDetail> auctions
    ) {
    }
}
