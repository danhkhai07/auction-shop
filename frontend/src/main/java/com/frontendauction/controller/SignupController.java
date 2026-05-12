package com.frontendauction.controller;

import com.frontendauction.model.SignupResult;
import com.frontendauction.service.AuthService;
import com.frontendauction.service.HttpAuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.control.PasswordField;
import javafx.scene.control.Label;
import javafx.stage.Stage;
import java.io.IOException;
import java.util.Objects;

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

    public void initialize() {
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
            System.out.println("Signup success. Message = " + result.message());

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
        Scene scene = new Scene(loader.load(), 600, 400);
        stage.setScene(scene);
        stage.show();
    }
}
