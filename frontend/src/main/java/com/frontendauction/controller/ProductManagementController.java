package com.frontendauction.controller;

import com.frontendauction.AppWindow;
import com.frontendauction.model.ProductManagementModel;
import com.frontendauction.service.ProductManagementService;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;

public class ProductManagementController {

    @FXML private TableView<ProductManagementModel> tvProducts;
    @FXML private TableColumn<ProductManagementModel, String> colId;
    @FXML private TableColumn<ProductManagementModel, String> colName;
    @FXML private TableColumn<ProductManagementModel, String> colDescription;

    @FXML private TextField txtId;
    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;

    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML private Button btnBack;
    @FXML private Button btnCreateAuction;
    @FXML private TextField txtAuctionPrice;
    @FXML private TextField txtMinBidStep;
    @FXML private TextField txtStartTime;
    @FXML private TextField txtEndTime;

    private final ProductManagementService productService = new ProductManagementService();
    private final ObservableList<ProductManagementModel> productList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        setupForm();
        setupAuctionForm();
        loadData();

        tvProducts.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateForm(newSelection);
            }
        });

        btnAdd.setOnAction(event -> handleAdd());
        btnUpdate.setOnAction(event -> handleUpdate());
        btnDelete.setOnAction(event -> handleDelete());
        if (btnCreateAuction != null) {
            btnCreateAuction.setOnAction(event -> handleCreateAuction());
        }
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        tvProducts.setItems(productList);
    }

    private void setupForm() {
        // Price field is enabled for user input
    }

    private void loadData() {
        productService.getAllProducts()
                .thenAccept(products -> Platform.runLater(() -> {
                    productList.clear();
                    productList.addAll(products);
                }))
                .exceptionally(exception -> {
                    Platform.runLater(() -> showError(resolveErrorMessage(exception)));
                    return null;
                });
    }



    private void populateForm(ProductManagementModel product) {
        txtId.setText(product.getId() != null ? product.getId() : "");
        txtName.setText(product.getName() != null ? product.getName() : "");
        txtDescription.setText(product.getDescription() != null ? product.getDescription() : "");
    }

    private void clearForm() {
        txtId.clear();
        txtName.clear();
        txtDescription.clear();
        tvProducts.getSelectionModel().clearSelection();
    }

    private void handleAdd() {
        if (!validateForm()) {
            return;
        }

        String name = txtName.getText().trim();
        String description = txtDescription.getText().trim();

        ProductManagementModel newProduct = new ProductManagementModel();
        newProduct.setName(name);
        newProduct.setDescription(description);

        btnAdd.setDisable(true);
        btnAdd.setText("Processing...");

        productService.addProduct(newProduct)
                .thenAccept(optionalId -> Platform.runLater(() -> {
                    btnAdd.setDisable(false);
                    btnAdd.setText("Add");
                    if (optionalId.isPresent()) {
                        // Thêm sản phẩm mới trực tiếp vào local list để hiển thị ngay
                        ProductManagementModel addedProduct = new ProductManagementModel(
                                optionalId.get(), name, description, null);
                        productList.add(addedProduct);
                        showSuccess("Product added successfully.");
                        clearForm();
                    } else {
                        showError("Failed to add product. Check console for details.");
                    }
                }))
                .exceptionally(exception -> {
                    Platform.runLater(() -> {
                        btnAdd.setDisable(false);
                        btnAdd.setText("Add");
                        showError(resolveErrorMessage(exception));
                    });
                    return null;
                });
    }

    private void handleUpdate() {
        ProductManagementModel selected = tvProducts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a product to update.");
            return;
        }

        if (!validateForm()) {
            return;
        }

        // Lưu giá trị mới ở local, KHÔNG mutate object cho đến khi server confirm
        String newName = txtName.getText().trim();
        String newDescription = txtDescription.getText().trim();
        String oldName = selected.getName();
        String oldDescription = selected.getDescription();

        ProductManagementModel payload = new ProductManagementModel();
        payload.setId(selected.getId());
        payload.setName(newName);
        payload.setDescription(newDescription);
        payload.setSellerId(selected.getSellerId());

        btnUpdate.setDisable(true);
        btnUpdate.setText("Processing...");

        productService.updateProduct(selected.getId(), payload)
                .thenAccept(success -> Platform.runLater(() -> {
                    btnUpdate.setDisable(false);
                    btnUpdate.setText("Update");
                    if (success) {
                        // Cập nhật trực tiếp object trong local list
                        selected.setName(newName);
                        selected.setDescription(newDescription);
                        tvProducts.refresh();
                        showSuccess("Product updated successfully.");
                        clearForm();
                    } else {
                        showError("Failed to update product.");
                    }
                }))
                .exceptionally(exception -> {
                    Platform.runLater(() -> {
                        btnUpdate.setDisable(false);
                        btnUpdate.setText("Update");
                        showError(resolveErrorMessage(exception));
                    });
                    return null;
                });
    }

    private void handleDelete() {
        ProductManagementModel selected = tvProducts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a product to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm deletion");
        confirm.setHeaderText(null);
        confirm.setContentText("Delete this product?");

        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                btnDelete.setDisable(true);
                btnDelete.setText("Processing...");

                productService.deleteProduct(selected.getId())
                        .thenAccept(success -> Platform.runLater(() -> {
                            btnDelete.setDisable(false);
                            btnDelete.setText("Delete");
                            if (success) {
                                // Xóa khỏi local list
                                productList.remove(selected);
                                showSuccess("Product deleted successfully.");
                                clearForm();
                            } else {
                                showError("Failed to delete product.");
                            }
                        }))
                        .exceptionally(exception -> {
                            Platform.runLater(() -> {
                                btnDelete.setDisable(false);
                                btnDelete.setText("Delete");
                                showError(resolveErrorMessage(exception));
                            });
                            return null;
                        });
            }
        });
    }

    private boolean validateForm() {
        if (txtName.getText() == null || txtName.getText().trim().isEmpty()) {
            showError("Product name cannot be empty.");
            return false;
        }

        if (txtDescription.getText() == null || txtDescription.getText().trim().isEmpty()) {
            showError("Product description cannot be empty.");
            return false;
        }

        return true;
    }

    private void setupAuctionForm() {
        if (txtStartTime != null) {
            java.time.LocalDateTime now = java.time.LocalDateTime.now();
            txtStartTime.setText(now.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        }
        if (txtEndTime != null) {
            java.time.LocalDateTime endDefault = java.time.LocalDateTime.now().plusHours(1);
            txtEndTime.setText(endDefault.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss")));
        }
        if (txtMinBidStep != null) {
            txtMinBidStep.clear();
        }
    }

    private void handleCreateAuction() {
        ProductManagementModel selected = tvProducts.getSelectionModel().getSelectedItem();
        if (selected == null || selected.getId() == null || selected.getId().isBlank()) {
            showError("Please select a product from the table to create an auction.");
            return;
        }

        if (txtAuctionPrice == null || txtStartTime == null || txtEndTime == null) return;

        String priceText = txtAuctionPrice.getText().trim();
        String minBidStepText = txtMinBidStep != null ? txtMinBidStep.getText().trim() : "";
        String startTime = txtStartTime.getText().trim();
        String endTime = txtEndTime.getText().trim();

        if (priceText.isEmpty()) {
            showError("Please enter a starting price for the auction.");
            return;
        }
        if (startTime.isEmpty() || endTime.isEmpty()) {
            showError("Please enter start and end time.");
            return;
        }

        double startingPrice;
        try {
            startingPrice = Double.parseDouble(priceText);
        } catch (NumberFormatException e) {
            showError("Starting price must be a valid number.");
            return;
        }

        double minBidIncrement = 1.0;
        if (!minBidStepText.isEmpty()) {
            try {
                minBidIncrement = Double.parseDouble(minBidStepText);
            } catch (NumberFormatException e) {
                showError("Min Bid Step must be a valid number.");
                return;
            }
        }

        btnCreateAuction.setDisable(true);
        btnCreateAuction.setText("Creating...");

        productService.createAuction(selected.getId(), startingPrice, minBidIncrement, startTime, endTime)
                .thenAccept(optionalId -> Platform.runLater(() -> {
                    btnCreateAuction.setDisable(false);
                    btnCreateAuction.setText("Create Auction");
                    if (optionalId.isPresent()) {
                        showSuccess("Auction created! ID: " + optionalId.get());
                        txtAuctionPrice.clear();
                        setupAuctionForm();
                    } else {
                        showError("Failed to create auction. Check console for details.");
                    }
                }))
                .exceptionally(exception -> {
                    Platform.runLater(() -> {
                        btnCreateAuction.setDisable(false);
                        btnCreateAuction.setText("Create Auction");
                        showError(resolveErrorMessage(exception));
                    });
                    return null;
                });
    }

    private String resolveErrorMessage(Throwable exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        String message = cause.getMessage();
        return message == null || message.isBlank() ? "Unable to load your products." : message;
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
            AppWindow.applyScene(stage, root);
            stage.show();
        } catch (IOException exception) {
            exception.printStackTrace();
            showError("Failed to return to Dashboard: " + exception.getMessage());
        }
    }
}
