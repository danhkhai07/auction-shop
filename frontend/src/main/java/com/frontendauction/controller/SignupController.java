package com.frontendauction.controller;

import com.frontendauction.AppWindow;
import com.frontendauction.model.SignupResult;
import com.frontendauction.service.AuthService;
import com.frontendauction.service.HttpAuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;
import javafx.scene.image.ImageView;
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public class SignupController {
    private final AuthService authService;

    public SignupController() {
        this(new HttpAuthService());
    }

    public SignupController(AuthService authService) {
        this.authService = authService;
    }
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private PasswordField confirmpassField;
    @FXML
    private Button signupButton;
    @FXML
    private Label errorLabel;
    @FXML
    private Button backButton;

    @FXML
    private ImageView hammerImage;
    @FXML
    private ImageView diamondImage;
    @FXML
    private ImageView clockImage;

    public void initialize() {
        applyFloatingEffect(hammerImage, 3.0, 15);
        applyFloatingEffect(diamondImage, 3.0, 15);
        applyFloatingEffect(clockImage, 3.0, 15);
        hideError();
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private boolean validateInput(String username, String password, String confirmpass) {
        if (username.isBlank() || password.isBlank() || confirmpass.isBlank()) {
            showError("Please fill all fields");
            return false;
        }
        if (!username.matches("^[a-zA-Z0-9_]{6,20}$")) {
            showError("Username need length 6-20 characters and only have words, numbers, _");
            return false;
        }
        if (password.length() < 10 || password.contains(" ")) {
            showError("Password must have at least 10 characters and not include space");
            return false;
        }
        if (!password.equals(confirmpass)) {
            showError("Confirm password does not match");
            return false;
        }

        return true;
    }

    //Doi trang thai Button Sign Up
    private void setLoading(boolean loading) {
        signupButton.setDisable(loading); // Lock Button Sign Up tranh bam nhieu lan
        signupButton.setText(loading ? "Signing up..." : "Sign Up");
    }

    @FXML
    private void handleSignup() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmpassField.getText();

        hideError();

        if (!validateInput(username, password, confirmPassword)) {
            return;
        }

        setLoading(true);

        authService.signup(username, password)
                .thenAccept(result -> Platform.runLater(() -> handleResponse(result)))
                //Platform.runLater dam bao handleResponse phai dien ra tren Main Thread UI
                .exceptionally(exception -> {
                    Platform.runLater(() -> {
                        setLoading(false);
                        showError(resolveErrorMessage(exception));
                    });
                    return null;
                });
    }

    private void handleResponse(SignupResult result) {
        setLoading(false);

        if (result.success()) {
            hideError();
            try {
                handleBacktoLogin();
            } catch (IOException exception) {
                showError("Signup success but cannot open login");
            }
            return;
        }

        showError(result.errorMessage());
    }

    private String resolveErrorMessage(Throwable exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        String message = cause.getMessage();

        if (message == null || message.isBlank()) {
            return "Can't connected to server";
        }

        return message;
    }

    @FXML
    private void handleBacktoLogin() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/com/frontendauction/login.fxml")));

        loader.setControllerFactory(type -> {//Tu tao controller truyen cung AuthService sang login Scene
            if (type == LoginController.class) {//Neu FXMLLoader can tao LoginController
                return new LoginController(authService);//Tao LoginController va truyen cung authService dang dung o signup
            }

            try {//Neu la Controller khac
                return type.getDeclaredConstructor().newInstance();//Tao bang constructor mac dinh
            }
            catch (Exception exception) {//Tao that bai
                throw new IllegalStateException("Cannot create controller: " + type.getName(), exception);//Bao loi
            }
        });
        Stage stage = (Stage) backButton.getScene().getWindow();
        Parent root = loader.load();
        AppWindow.applyScene(stage, root);
        stage.show();
    }

    private void applyFloatingEffect(Node node, double durationSec, double deltaY) {
        TranslateTransition floatTransition = new TranslateTransition(Duration.seconds(durationSec), node);
        floatTransition.setByY(deltaY);
        floatTransition.setAutoReverse(true); // Đi xuống xong tự động đi ngược lên
        floatTransition.setCycleCount(TranslateTransition.INDEFINITE); // Lặp lại vô hạn lần

        // Tạo độ trễ ngẫu nhiên để các vật thể không trôi lên xuống cùng một nhịp
        floatTransition.setDelay(Duration.seconds(Math.random()));
        floatTransition.play();

        // 2. Tạo hiệu ứng phóng to (Khi chuột chỉ vào)
        ScaleTransition scaleUp = new ScaleTransition(Duration.millis(200), node);
        scaleUp.setToX(1.1); // Phóng to 10% chiều ngang
        scaleUp.setToY(1.1); // Phóng to 10% chiều dọc

        // 3. Tạo hiệu ứng thu nhỏ (Khi chuột rời đi)
        ScaleTransition scaleDown = new ScaleTransition(Duration.millis(200), node);
        scaleDown.setToX(1.0); // Trở về kích thước gốc
        scaleDown.setToY(1.0);

        // 4. Bắt sự kiện tương tác chuột
        node.setOnMouseEntered(e -> {
            floatTransition.pause(); // Dừng lơ lửng
            scaleUp.play(); // Phóng to lên
        });

        node.setOnMouseExited(e -> {
            scaleDown.play(); // Thu nhỏ lại
            // Đợi thu nhỏ xong thì mới cho trôi lơ lửng tiếp
            scaleDown.setOnFinished(event -> floatTransition.play());
        });
    }
}
