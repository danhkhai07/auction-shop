package com.frontendauction.controller;

import com.frontendauction.service.UserProfileService;
import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class DepositController {

    @FXML private TextField txtDepositAmount;
    @FXML private Button btnCheckPayment;

    private final UserProfileService userProfileService = new UserProfileService();
    private Runnable onSuccessCallback;

    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    @FXML
    public void handleCheckPayment(ActionEvent event) {
        String amountStr = txtDepositAmount.getText().trim();
        if (amountStr.isEmpty()) {
            showError("Vui lòng nhập số tiền cần nạp.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                showError("Số tiền nạp phải lớn hơn 0.");
                return;
            }

            btnCheckPayment.setDisable(true);
            btnCheckPayment.setText("Đang xử lý...");

            userProfileService.deposit(amount)
                    .thenAccept(balanceResponse -> Platform.runLater(() -> {
                        showSuccess("Thanh toán thành công! Đã nạp " + String.format("%,.0f VND", amount) + " vào ví.");
                        if (onSuccessCallback != null) {
                            onSuccessCallback.run();
                        }
                        handleClose(event);
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            btnCheckPayment.setDisable(false);
                            btnCheckPayment.setText("Kiểm tra thanh toán");
                            showError("Lỗi khi nạp tiền: " + ex.getCause().getMessage());
                        });
                        return null;
                    });
        } catch (NumberFormatException e) {
            showError("Số tiền không hợp lệ.");
        }
    }

    @FXML
    public void handleClose(ActionEvent event) {
        Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();
        stage.close();
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
