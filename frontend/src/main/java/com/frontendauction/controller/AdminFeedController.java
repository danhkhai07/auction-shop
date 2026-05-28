package com.frontendauction.controller;

import com.frontendauction.model.LiveAuctionModel;
import com.frontendauction.model.UserProfileModel;
import com.frontendauction.service.AdminService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;

public class AdminFeedController {

    @FXML
    private TextField searchUserField;

    @FXML
    private Button btnSearchUser;

    @FXML
    private Button btnGetAllUsers;

    @FXML
    private Button btnDeleteUser;

    @FXML
    private Button btnBanUser;

    @FXML
    private Button btnUnbanUser;

    @FXML
    private Button btnUnbanUser1; // Elevate User

    @FXML
    private TableView<UserProfileModel> userTable;

    @FXML
    private TableColumn<UserProfileModel, String> colUserId;

    @FXML
    private TableColumn<UserProfileModel, String> colUsername;

    @FXML
    private TableColumn<UserProfileModel, String> colUserStatus;

    @FXML
    private TableColumn<UserProfileModel, String> colUserRole;

    @FXML
    private TextField searchAuctionField;

    @FXML
    private Button btnSearchAuction;

    @FXML
    private Button btnGetAllAuctions;

    @FXML
    private Button btnDeleteAuction;

    @FXML
    private TableView<LiveAuctionModel.AuctionDetail> auctionTable;

    @FXML
    private TableColumn<LiveAuctionModel.AuctionDetail, String> colAuctionId;

    @FXML
    private TableColumn<LiveAuctionModel.AuctionDetail, String> colAuctionName;

    @FXML
    private TableColumn<LiveAuctionModel.AuctionDetail, String> colAuctionStatus;

    private AdminService adminService;
    private ObservableList<UserProfileModel> userList;
    private ObservableList<LiveAuctionModel.AuctionDetail> auctionList;

    @FXML
    public void initialize() {
        adminService = new AdminService();
        userList = FXCollections.observableArrayList();
        auctionList = FXCollections.observableArrayList();

        userTable.setItems(userList);
        auctionTable.setItems(auctionList);

        setupColumns();
        setupActions();

        loadAllUsers();
        loadAllAuctions();
    }

    private void setupColumns() {
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        colUserStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
        colUserRole.setCellValueFactory(new PropertyValueFactory<>("roles"));

        colAuctionId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colAuctionName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colAuctionStatus.setCellValueFactory(new PropertyValueFactory<>("status"));
    }

    private void setupActions() {
        btnSearchUser.setOnAction(e -> searchUser());
        btnGetAllUsers.setOnAction(e -> loadAllUsers());

        btnDeleteUser.setOnAction(e -> {
            UserProfileModel selected = userTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                adminService.deleteUser(selected.getId()).thenAccept(result -> {
                    Platform.runLater(() -> {
                        if ("SUCCESS".equals(result)) {
                            userList.remove(selected);
                            showAlert("Success", "User deleted successfully", Alert.AlertType.INFORMATION);
                        } else {
                            showAlert("Error", "Failed to delete user: " + result, Alert.AlertType.ERROR);
                        }
                    });
                });
            } else {
                showAlert("Warning", "Please select a user first", Alert.AlertType.WARNING);
            }
        });

        btnBanUser.setOnAction(e -> {
            UserProfileModel selected = userTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                adminService.banUser(selected.getId()).thenAccept(result -> {
                    Platform.runLater(() -> {
                        if ("SUCCESS".equals(result)) {
                            selected.setStatus("BANNED");
                            userTable.refresh();
                            showAlert("Success", "User banned successfully", Alert.AlertType.INFORMATION);
                        } else {
                            showAlert("Error", "Failed to ban user: " + result, Alert.AlertType.ERROR);
                        }
                    });
                });
            } else {
                showAlert("Warning", "Please select a user first", Alert.AlertType.WARNING);
            }
        });

        btnUnbanUser.setOnAction(e -> {
            UserProfileModel selected = userTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                adminService.unbanUser(selected.getId()).thenAccept(result -> {
                    Platform.runLater(() -> {
                        if ("SUCCESS".equals(result)) {
                            selected.setStatus("ACTIVE");
                            userTable.refresh();
                            showAlert("Success", "User unbanned successfully", Alert.AlertType.INFORMATION);
                        } else {
                            showAlert("Error", "Failed to unban user: " + result, Alert.AlertType.ERROR);
                        }
                    });
                });
            } else {
                showAlert("Warning", "Please select a user first", Alert.AlertType.WARNING);
            }
        });

        btnUnbanUser1.setOnAction(e -> {
            UserProfileModel selected = userTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                adminService.elevateUser(selected.getId()).thenAccept(result -> {
                    Platform.runLater(() -> {
                        if ("SUCCESS".equals(result)) {
                            if (selected.getRoles() != null) {
                                selected.getRoles().add("ADMIN");
                            }
                            userTable.refresh();
                            showAlert("Success", "User elevated to admin successfully", Alert.AlertType.INFORMATION);
                        } else {
                            showAlert("Error", "Failed to elevate user: " + result, Alert.AlertType.ERROR);
                        }
                    });
                });
            } else {
                showAlert("Warning", "Please select a user first", Alert.AlertType.WARNING);
            }
        });

        btnSearchAuction.setOnAction(e -> searchAuction());
        btnGetAllAuctions.setOnAction(e -> loadAllAuctions());

        btnDeleteAuction.setOnAction(e -> {
            LiveAuctionModel.AuctionDetail selected = auctionTable.getSelectionModel().getSelectedItem();
            if (selected != null) {
                adminService.deleteAuction(selected.getId()).thenAccept(result -> {
                    Platform.runLater(() -> {
                        if ("SUCCESS".equals(result)) {
                            auctionList.remove(selected);
                            showAlert("Success", "Auction deleted successfully", Alert.AlertType.INFORMATION);
                        } else {
                            showAlert("Error", "Failed to delete auction: " + result, Alert.AlertType.ERROR);
                        }
                    });
                });
            } else {
                showAlert("Warning", "Please select an auction first", Alert.AlertType.WARNING);
            }
        });
    }

    private void loadAllUsers() {
        adminService.getAllUsers().thenAccept(users -> {
            Platform.runLater(() -> {
                userList.setAll(users);
            });
        });
    }

    private void searchUser() {
        String query = searchUserField.getText().trim();
        if (!query.isEmpty()) {
            adminService.searchUser(query).thenAccept(users -> {
                Platform.runLater(() -> {
                    userList.setAll(users);
                });
            });
        } else {
            loadAllUsers();
        }
    }

    private void loadAllAuctions() {
        adminService.getAllAuctions().thenAccept(auctions -> {
            Platform.runLater(() -> {
                auctionList.setAll(auctions);
            });
        });
    }

    private void searchAuction() {
        String query = searchAuctionField.getText().trim();
        if (!query.isEmpty()) {
            adminService.searchAuction(query).thenAccept(auctions -> {
                Platform.runLater(() -> {
                    auctionList.setAll(auctions);
                });
            });
        } else {
            loadAllAuctions();
        }
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    @FXML
    public void goToDashboard(javafx.event.Event event) throws java.io.IOException {
        javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                java.util.Objects.requireNonNull(getClass().getResource("/com/frontendauction/dashboard.fxml")));
        javafx.stage.Stage stage = (javafx.stage.Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();
        javafx.scene.Parent root = loader.load();
        com.frontendauction.AppWindow.applyScene(stage, root);
        stage.show();
    }
}
