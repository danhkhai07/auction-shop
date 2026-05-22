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
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ProductManagementController {

    @FXML private TableView<ProductManagementModel> tvProducts;
    @FXML private TableColumn<ProductManagementModel, String> colId;
    @FXML private TableColumn<ProductManagementModel, String> colName;
    @FXML private TableColumn<ProductManagementModel, String> colPrice;
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
    private final ObservableList<ProductManagementModel> productList = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        setupTable();
        setupForm();
        loadData();

        tvProducts.getSelectionModel().selectedItemProperty().addListener((obs, oldSelection, newSelection) -> {
            if (newSelection != null) {
                populateForm(newSelection);
            }
        });

        btnAdd.setOnAction(event -> handleAdd());
        btnUpdate.setOnAction(event -> handleUpdate());
        btnDelete.setOnAction(event -> handleDelete());
    }

    private void setupTable() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colName.setCellValueFactory(new PropertyValueFactory<>("name"));
        colPrice.setCellValueFactory(new PropertyValueFactory<>("sellerId"));
        colDescription.setCellValueFactory(new PropertyValueFactory<>("description"));

        colPrice.setText("Seller ID");
        colPrice.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String value, boolean empty) {
                super.updateItem(value, empty);
                if (empty) {
                    setText(null);
                    return;
                }
                setText(value == null || value.isBlank() ? "-" : value);
            }
        });

        tvProducts.setItems(productList);
    }

    private void setupForm() {
        txtPrice.setDisable(true);
        txtPrice.setText("-");
        txtPrice.setPromptText("Derived from logged-in user");
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

    /**
     * Đợi 700ms trước khi reload để server kịp cập nhật itemList trong user profile.
     * Nguyên nhân: sau khi add/update/delete, /auth/me có thể chưa phản ánh thay đổi ngay.
     */
    private void loadDataWithDelay() {
        CompletableFuture.delayedExecutor(700, TimeUnit.MILLISECONDS)
                .execute(this::loadData);
    }

    private void populateForm(ProductManagementModel product) {
        txtId.setText(product.getId() != null ? product.getId() : "");
        txtName.setText(product.getName() != null ? product.getName() : "");
        txtPrice.setText(product.getSellerId() != null && !product.getSellerId().isBlank()
                ? product.getSellerId()
                : "-");
        txtDescription.setText(product.getDescription() != null ? product.getDescription() : "");
    }

    private void clearForm() {
        txtId.clear();
        txtName.clear();
        txtPrice.setText("-");
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
                        // Thêm trực tiếp vào list với ID từ server, không cần reload
                        ProductManagementModel created = new ProductManagementModel();
                        created.setId(optionalId.get());
                        created.setName(name);
                        created.setDescription(description);
                        productList.add(created);
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
                        // Chỉ cập nhật object sau khi server xác nhận
                        selected.setName(newName);
                        selected.setDescription(newDescription);
                        // Buộc TableView refresh
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
                                // Xóa trực tiếp khỏi list, không cần reload
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
        if (txtName.getText().trim().isEmpty()) {
            showError("Product name cannot be empty.");
            return false;
        }

        if (txtDescription.getText().trim().isEmpty()) {
            showError("Product description cannot be empty.");
            return false;
        }

        return true;
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
