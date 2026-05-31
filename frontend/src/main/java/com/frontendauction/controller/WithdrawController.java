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

public class WithdrawController {

    @FXML private TextField txtWithdrawAmount;
    @FXML private Button btnWithdraw;

    private final UserProfileService userProfileService = new UserProfileService();
    private Runnable onSuccessCallback;

    public void setOnSuccessCallback(Runnable callback) {
        this.onSuccessCallback = callback;
    }

    @FXML
    public void handleWithdrawAction(ActionEvent event) {
        String amountStr = txtWithdrawAmount.getText().trim();
        if (amountStr.isEmpty()) {
            showError("Vui lòng nhập số tiền cần rút.");
            return;
        }

        try {
            double amount = Double.parseDouble(amountStr);
            if (amount <= 0) {
                showError("Số tiền rút phải lớn hơn 0.");
                return;
            }

            btnWithdraw.setDisable(true);
            btnWithdraw.setText("Đang xử lý...");

            userProfileService.withdraw(amount)
                    .thenAccept(balanceResponse -> Platform.runLater(() -> {
                        showSuccess("Rút tiền thành công! Đã rút " + String.format("%,.0f VND", amount) + " từ ví.");
                        if (onSuccessCallback != null) {
                            onSuccessCallback.run();
                        }
                        handleClose(event);
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> {
                            btnWithdraw.setDisable(false);
                            btnWithdraw.setText("Withdraw");
                            String msg = ex.getCause() != null ? ex.getCause().getMessage() : ex.getMessage();
                            showError("Lỗi khi rút tiền: " + (msg == null ? "Unknown" : msg));
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
