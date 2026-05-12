package com.frontendauction;

import com.frontendauction.service.AuthService;
import com.frontendauction.service.HttpAuthService;
import com.frontendauction.service.MockAuthService;
import com.frontendauction.controller.LoginController;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.Locale;
import java.util.Objects;

public class AuctionShopApplication extends Application {

    @Override
    public void start(Stage stage) throws IOException {
        AuthService authService = createAuthService();
        FXMLLoader loader = new FXMLLoader(
                Objects.requireNonNull(
                        AuctionShopApplication.class.getResource("/com/frontendauction/login.fxml"),
                        "login.fxml not found"
                )
        );
        loader.setControllerFactory(type -> {
            if (type == LoginController.class) {
                return new LoginController(authService);
            }

            try {
                return type.getDeclaredConstructor().newInstance();
            } catch (Exception exception) {
                throw new IllegalStateException("Cannot create controller: " + type.getName(), exception);
            }
        });

        Scene scene = new Scene(loader.load(), 600, 400);
        stage.setTitle("Auction Shop");
        stage.setScene(scene);
        stage.setResizable(false);
        stage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }

    private AuthService createAuthService() {
        String appMode = System.getProperty("app.mode", "http")
                .trim()
                .toLowerCase(Locale.ROOT);

        return switch (appMode) {
            case "http" -> new HttpAuthService();
            case "mock" -> new MockAuthService();
            default -> throw new IllegalArgumentException("Unsupported app.mode: " + appMode);
        };
    }
}
