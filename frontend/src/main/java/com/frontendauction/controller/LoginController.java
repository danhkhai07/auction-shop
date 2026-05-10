package com.frontendauction.controller;

import com.frontendauction.service.AuthService;
import com.frontendauction.service.HttpAuthService;
import com.frontendauction.model.LoginResult;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;//Nap file FXML thanh giao dien JavaFx
import javafx.scene.Scene;//Tao Scene moi cho cua so
import javafx.stage.Stage;//Dieu khien cua so hien tai
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import java.io.IOException;//Bat loi khi load FXML failed
import java.util.Objects;//Kiem tra resource tranh bi null

public class LoginController {

    private final AuthService authService;

    public LoginController() {
        this(new HttpAuthService());
    }

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

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

        loader.setControllerFactory(type -> {//Tu tao controller truyen cung AuthService sang signup Scene
            if (type == SignupController.class) {//Neu FXMLLoader can tao SignupController
                return new SignupController(authService);//Tao SignupController va truyen cung authService dang dung o login
            }

            try {//Neu la Controller khac
                return type.getDeclaredConstructor().newInstance();//Tao bang constructor mac dinh
            }
            catch (Exception exception) {//Tao that bai
                throw new IllegalStateException("Cannot create controller: " + type.getName(), exception);//Bao loi
            }
        });
        Stage stage = (Stage) loginButton.getScene().getWindow();//Lay cua so hien tai tu Login Button
        Scene scene = new Scene(loader.load(), 600, 400);
        stage.setScene(scene);//Chuyen qua Scene Signup
        stage.show();
    }

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
    //Doi trang thai cua button Login
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
            hideError();
            System.out.println("Login success. Token = " + result.token());
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
}
