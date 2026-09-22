package com.chronoquiz.controller;

import com.chronoquiz.ChronoQuizApp;
import com.chronoquiz.db.AdminDAO;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Controller and View Builder for Admin Login Screen.
 * Enforces password authentication before granting administrative privileges.
 */
public class AdminLoginController {
    private final ChronoQuizApp app;
    private final AdminDAO adminDAO = new AdminDAO();

    private TextField usernameField;
    private PasswordField passwordField;
    private Label errorLabel;

    public AdminLoginController(ChronoQuizApp app) {
        this.app = app;
    }

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-container");

        VBox centerBox = new VBox(24);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setMaxWidth(480);

        // Header Title
        Label title = new Label("🔐 Administrator Portal");
        title.getStyleClass().add("app-title");

        Label subtitle = new Label("Secure sign-in for Question Management & Exam Controls");
        subtitle.getStyleClass().add("subtitle");

        // Login Card
        VBox card = new VBox(18);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(28));

        GridPane formGrid = new GridPane();
        formGrid.setHgap(12);
        formGrid.setVgap(14);
        formGrid.setAlignment(Pos.CENTER);

        Label userLabel = new Label("Admin Username:");
        userLabel.getStyleClass().add("field-label");
        usernameField = new TextField("admin");
        usernameField.setPromptText("Enter admin username");

        Label passLabel = new Label("Password:");
        passLabel.getStyleClass().add("field-label");
        passwordField = new PasswordField();
        passwordField.setPromptText("Enter password");
        passwordField.setOnAction(e -> handleLogin());

        formGrid.add(userLabel, 0, 0);
        formGrid.add(usernameField, 1, 0);
        formGrid.add(passLabel, 0, 1);
        formGrid.add(passwordField, 1, 1);

        GridPane.setHgrow(usernameField, Priority.ALWAYS);
        GridPane.setHgrow(passwordField, Priority.ALWAYS);

        errorLabel = new Label();
        errorLabel.setStyle("-fx-text-fill: #f38ba8; -fx-font-weight: bold;");
        errorLabel.setVisible(false);

        // Login Button
        Button loginBtn = new Button("🔑 Sign In to Admin Panel");
        loginBtn.getStyleClass().addAll("button", "btn-primary");
        loginBtn.setMaxWidth(Double.MAX_VALUE);
        loginBtn.setOnAction(e -> handleLogin());

        // Default credentials hint
        Label hintLabel = new Label("Default evaluation credentials: admin / admin123");
        hintLabel.getStyleClass().add("badge");

        card.getChildren().addAll(formGrid, errorLabel, loginBtn, hintLabel);

        // Bottom: Back to user home
        HBox bottomBox = new HBox();
        bottomBox.setAlignment(Pos.CENTER);
        Button backBtn = new Button("⬅ Return to Quiz Home");
        backBtn.getStyleClass().addAll("button", "btn-secondary");
        backBtn.setOnAction(e -> app.showStartScreen());
        bottomBox.getChildren().add(backBtn);

        centerBox.getChildren().addAll(title, subtitle, card, bottomBox);

        VBox outer = new VBox(centerBox);
        outer.setAlignment(Pos.CENTER);
        root.setCenter(outer);

        return root;
    }

    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        if (username.isEmpty() || password.isEmpty()) {
            errorLabel.setText("Please enter both username and password.");
            errorLabel.setVisible(true);
            return;
        }

        if (adminDAO.authenticate(username, password)) {
            errorLabel.setVisible(false);
            app.setAdminLoggedIn(true);
            app.showAdminPanel();
        } else {
            errorLabel.setText("Invalid credentials. Try: admin / admin123");
            errorLabel.setVisible(true);
            passwordField.clear();
            passwordField.requestFocus();
        }
    }
}
