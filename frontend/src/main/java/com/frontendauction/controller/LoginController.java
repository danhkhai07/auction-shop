package com.frontendauction.controller;

import com.frontendauction.AppWindow;
import com.frontendauction.service.AuthService;
import com.frontendauction.service.HttpAuthService;
import com.frontendauction.model.LoginResult;
import com.frontendauction.service.TokenStore;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;//Nap file FXML thanh giao dien JavaFx
import javafx.scene.Parent;
import javafx.scene.Scene;//Tao Scene moi cho cua so
import javafx.scene.layout.VBox;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;//Dieu khien cua so hien tai
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.image.ImageView;

import java.io.IOException;//Bat loi khi load FXML failed
import java.util.Objects;//Kiem tra resource tranh bi null
import javafx.animation.ScaleTransition;
import javafx.animation.TranslateTransition;
import javafx.scene.Node;
import javafx.util.Duration;

public class LoginController {

    private final AuthService authService;

    public LoginController() {
        this(new HttpAuthService());
    }

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @FXML
    private StackPane loginContainer;
    @FXML
    private VBox loginForm;
    @FXML
    private TextField usernameField;
    @FXML
    private PasswordField passwordField;
    @FXML
    private Label errorLabel;
    @FXML
    private Button loginButton;

    @FXML // Danh dau la ham duoc goi tu FXML qua OnAction
    private void handleSignup() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/com/frontendauction/signup.fxml")));

        loader.setControllerFactory(type -> {// Tu tao controller truyen cung AuthService sang signup Scene
            if (type == SignupController.class) {// Neu FXMLLoader can tao SignupController
                return new SignupController(authService);// Tao SignupController va truyen cung authService dang dung o
                                                         // login
            }

            try {// Neu la Controller khac
                return type.getDeclaredConstructor().newInstance();// Tao bang constructor mac dinh
            } catch (Exception exception) {// Tao that bai
                throw new IllegalStateException("Cannot create controller: " + type.getName(), exception);// Bao loi
            }
        });
        Stage stage = (Stage) loginButton.getScene().getWindow();// Lay cua so hien tai tu Login Button
        Parent root = loader.load();
        AppWindow.applyScene(stage, root);// Chuyen qua Scene Signup
        stage.show();
    }

    @FXML
    public void initialize() {
        hideError();
    }

    private void showError(String message) {
        errorLabel.setText(message);
        errorLabel.setVisible(true);
    }

    private void hideError() {
        errorLabel.setText("");
        errorLabel.setVisible(false);
    }

    private boolean validateInput(String username, String password) {
        if (username.isBlank() || password.isBlank()) {
            showError("Please type fully username and password!");
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

        return true;
    }

    // Doi trang thai cua button Login
    private void setLoading(boolean loading) {
        loginButton.setDisable(loading);
        loginButton.setText(loading ? "Loading..." : "Login");
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        hideError();

        if (!validateInput(username, password)) {
            return;
        }

        setLoading(true);

        authService.login(username, password)
                .thenAccept(result -> Platform.runLater(() -> handleResponse(result)))
                .exceptionally(exception -> {
                    Platform.runLater(() -> {
                        setLoading(false);
                        showError(resolveErrorMessage(exception));
                    });
                    return null;
                });
    }

    private void handleResponse(LoginResult result) {
        setLoading(false);

        if (result.success()) {
            if (result.token() == null || result.token().isBlank()) {
                showError("Login response missing token");
                return;
            }

            hideError();
            TokenStore.setToken(result.token());
            try {
                navigateToDashboard();
            } catch (IOException exception) {
                showError("Cannot open dashboard screen");
            }
            return;
        }

        showError(result.errorMessage());
    }

    private void navigateToDashboard() throws IOException {
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(getClass().getResource("/com/frontendauction/dashboard.fxml")));
        Stage stage = (Stage) loginButton.getScene().getWindow();
        Parent root = loader.load();
        AppWindow.applyScene(stage, root);
        stage.show();
    }

    private String resolveErrorMessage(Throwable exception) {
        Throwable cause = exception.getCause() == null ? exception : exception.getCause();
        String message = cause.getMessage();

        if (message == null || message.isBlank()) {
            return "Can't connected to server";
        }

        return message;
    }


}
