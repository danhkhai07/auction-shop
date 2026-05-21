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
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;

public class ProductManagementController {

    @FXML private TableView<ProductManagementModel> tvProducts;
    @FXML private TableColumn<ProductManagementModel, String> colId;
    @FXML private TableColumn<ProductManagementModel, String> colName;
    @FXML private TableColumn<ProductManagementModel, Double> colPrice;
    @FXML private TableColumn<ProductManagementModel, String> colDescription;

    @FXML private TextField txtId;
    @FXML private TextField txtName;
    @FXML private TextField txtPrice;
    @FXML private TextArea txtDescription;

    @FXML private Button btnAdd;
    @FXML private Button btnUpdate;
    @FXML private Button btnDelete;
    @FXML private Button btnBack;

    private final ProductManagementService productService = new ProductManagementService();
    private ObservableList<ProductManagementModel> productList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        loadData();

        tvProducts.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateForm(newSelection);
            }
        });

        btnAdd.setOnAction(e -> handleAdd());
        btnUpdate.setOnAction(e -> handleUpdate());
        btnDelete.setOnAction(e -> handleDelete());
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("startingPrice"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));
        
        colPrice.setCellFactory(tc -> new TableCell<ProductManagementModel, Double>() {
            @Override
            protected void updateItem(Double price, boolean empty) {
                super.updateItem(price, empty);
                if (empty || price == null) {
                    setText(null);
                } else {
                    setText(String.format("%,.0f VNĐ", price));
                }
            }
        });

        tvProducts.setItems(productList);
    }

    private void loadData() {
        productService.getAllProducts().thenAccept(products -> {
            Platform.runLater(() -> {
                productList.clear();
                productList.addAll(products);
            });
        });
    }

    private void populateForm(ProductManagementModel product) {
        txtId.setText(product.getId() != null ? product.getId() : "");
        txtName.setText(product.getName() != null ? product.getName() : "");
        txtPrice.setText(product.getStartingPrice() != null ? String.valueOf(product.getStartingPrice()) : "");
        txtDescription.setText(product.getDescription() != null ? product.getDescription() : "");
    }

    private void clearForm() {
        txtId.clear();
        txtName.clear();
        txtPrice.clear();
        txtDescription.clear();
        tvProducts.getSelectionModel().clearSelection();
    }

    private void handleAdd() {
        if (!validateForm()) return;

        ProductManagementModel newProduct = new ProductManagementModel();
        newProduct.setName(txtName.getText());
        newProduct.setStartingPrice(Double.parseDouble(txtPrice.getText()));
        newProduct.setDescription(txtDescription.getText());

        btnAdd.setDisable(true);
        btnAdd.setText("Processing...");

        productService.addProduct(newProduct).thenAccept(success -> {
            Platform.runLater(() -> {
                btnAdd.setDisable(false);
                btnAdd.setText("Thêm Mới");
                if (success) {
                    showSuccess("Product added successfully!");
                    clearForm();
                    loadData();
                } else {
                    showError("Failed to add product. Please check API.");
                }
            });
        });
    }

    private void handleUpdate() {
        ProductManagementModel selected = tvProducts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a product from the table to update.");
            return;
        }

        if (!validateForm()) return;

        selected.setName(txtName.getText());
        selected.setStartingPrice(Double.parseDouble(txtPrice.getText()));
        selected.setDescription(txtDescription.getText());

        btnUpdate.setDisable(true);
        btnUpdate.setText("Processing...");

        productService.updateProduct(selected.getId(), selected).thenAccept(success -> {
            Platform.runLater(() -> {
                btnUpdate.setDisable(false);
                btnUpdate.setText("Cập Nhật");
                if (success) {
                    showSuccess("Product updated successfully!");
                    clearForm();
                    loadData();
                } else {
                    showError("Failed to update product. Please check API.");
                }
            });
        });
    }

    private void handleDelete() {
        ProductManagementModel selected = tvProducts.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showError("Please select a product from the table to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText(null);
        confirm.setContentText("Are you sure you want to delete this product?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                btnDelete.setDisable(true);
                btnDelete.setText("Processing...");

                productService.deleteProduct(selected.getId()).thenAccept(success -> {
                    Platform.runLater(() -> {
                        btnDelete.setDisable(false);
                        btnDelete.setText("Xóa");
                        if (success) {
                            showSuccess("Product deleted successfully!");
                            clearForm();
                            loadData();
                        } else {
                            showError("Failed to delete product. Please check API.");
                        }
                    });
                });
            }
        });
    }

    private boolean validateForm() {
        if (txtName.getText().trim().isEmpty()) {
            showError("Product name cannot be empty.");
            return false;
        }
        try {
            Double.parseDouble(txtPrice.getText().trim());
        } catch (NumberFormatException e) {
            showError("Price must be a valid number.");
            return false;
        }
        return true;
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
        } catch (IOException e) {
            e.printStackTrace();
            showError("Failed to return to Dashboard: " + e.getMessage());
        }
    }
}
