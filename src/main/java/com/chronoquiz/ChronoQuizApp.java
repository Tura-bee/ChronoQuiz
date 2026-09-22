package com.chronoquiz;

import com.chronoquiz.controller.AdminLoginController;
import com.chronoquiz.controller.HistoryController;
import com.chronoquiz.controller.QuestionManagerController;
import com.chronoquiz.controller.QuizController;
import com.chronoquiz.controller.ResultsController;
import com.chronoquiz.controller.ReviewController;
import com.chronoquiz.controller.StartController;
import com.chronoquiz.db.DatabaseManager;
import com.chronoquiz.model.Attempt;
import com.chronoquiz.model.Quiz;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.stage.Stage;

import java.net.URL;

/**
 * Main JavaFX Application class for ChronoQuiz.
 * Manages Stage initialization, Scene transitions, and global CSS styling.
 */
public class ChronoQuizApp extends Application {
    private Stage primaryStage;
    private String stylesheetUrl;

    @Override
    public void start(Stage stage) {
        this.primaryStage = stage;
        this.primaryStage.setTitle("ChronoQuiz — Timed Exam & Knowledge Challenge");
        this.primaryStage.setMinWidth(900);
        this.primaryStage.setMinHeight(680);

        // Initialize Database asynchronously or on startup
        DatabaseManager.getInstance();

        // Load CSS stylesheet
        URL cssResource = getClass().getResource("/style.css");
        if (cssResource != null) {
            stylesheetUrl = cssResource.toExternalForm();
        }

        // Show start screen
        showStartScreen();

        this.primaryStage.show();
    }

    public void applyTheme(Scene scene) {
        if (stylesheetUrl != null && scene != null) {
            scene.getStylesheets().clear();
            scene.getStylesheets().add(stylesheetUrl);
        }
    }

    public void showStartScreen() {
        StartController controller = new StartController(this);
        Scene scene = new Scene(controller.getView(), 960, 700);
        applyTheme(scene);
        primaryStage.setScene(scene);
    }

    public void startQuiz(Quiz quiz) {
        QuizController controller = new QuizController(this, quiz);
        Scene scene = new Scene(controller.getView(), 960, 700);
        applyTheme(scene);
        primaryStage.setScene(scene);

        // Ensure timer is properly stopped if user closes window mid-quiz
        primaryStage.setOnCloseRequest(e -> controller.cleanup());
    }

    public void showResultsScreen(Quiz quiz, Attempt attempt) {
        primaryStage.setOnCloseRequest(null);
        ResultsController controller = new ResultsController(this, quiz, attempt);
        Scene scene = new Scene(controller.getView(), 960, 700);
        applyTheme(scene);
        primaryStage.setScene(scene);
    }

    public void showReviewScreen(Attempt attempt, boolean returnToHistory) {
        ReviewController controller = new ReviewController(this, attempt, returnToHistory);
        Scene scene = new Scene(controller.getView(), 960, 700);
        applyTheme(scene);
        primaryStage.setScene(scene);
    }

    private boolean adminLoggedIn = false;

    public boolean isAdminLoggedIn() {
        return adminLoggedIn;
    }

    public void setAdminLoggedIn(boolean adminLoggedIn) {
        this.adminLoggedIn = adminLoggedIn;
    }

    public void showAdminLogin() {
        AdminLoginController controller = new AdminLoginController(this);
        Scene scene = new Scene(controller.getView(), 960, 700);
        applyTheme(scene);
        primaryStage.setScene(scene);
    }

    public void showAdminPanel() {
        if (!adminLoggedIn) {
            showAdminLogin();
            return;
        }
        QuestionManagerController controller = new QuestionManagerController(this);
        Scene scene = new Scene(controller.getView(), 1040, 750);
        applyTheme(scene);
        primaryStage.setScene(scene);
    }

    public void logoutAdmin() {
        this.adminLoggedIn = false;
        showStartScreen();
    }

    public void showQuestionManager() {
        showAdminPanel();
    }

    public void showHistory() {
        HistoryController controller = new HistoryController(this);
        Scene scene = new Scene(controller.getView(), 1020, 740);
        applyTheme(scene);
        primaryStage.setScene(scene);
    }

    public Stage getPrimaryStage() {
        return primaryStage;
    }

    @Override
    public void stop() {
        Platform.exit();
        System.exit(0);
    }

    public static void main(String[] args) {
        launch(args);
    }
}
