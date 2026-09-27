package com.chronoquiz.controller;

import com.chronoquiz.ChronoQuizApp;
import com.chronoquiz.api.ApiService;
import com.chronoquiz.db.QuestionDAO;
import com.chronoquiz.db.UserDAO;
import com.chronoquiz.model.Category;
import com.chronoquiz.model.Player;
import com.chronoquiz.model.Question;
import com.chronoquiz.model.Quiz;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Controller and View Builder for the Start Screen.
 * Handles player identity, quiz configuration, source selection, and async loading.
 */
public class StartController {
    private final ChronoQuizApp app;
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final UserDAO userDAO = new UserDAO();
    private final ApiService apiService = new ApiService();

    private TextField nameField;
    private ComboBox<String> sourceCombo;
    private ComboBox<String> categoryCombo;
    private ComboBox<String> difficultyCombo;
    private ComboBox<Integer> questionCountCombo;
    private Label previewInfoLabel;
    private Button startButton;
    private ProgressIndicator loadingIndicator;

    public StartController(ChronoQuizApp app) {
        this.app = app;
    }

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-container");

        // Top Header
        VBox headerBox = new VBox(8);
        headerBox.setAlignment(Pos.CENTER);
        headerBox.setPadding(new Insets(10, 0, 20, 0));

        Label logoLabel = new Label("⏱ ChronoQuiz");
        logoLabel.getStyleClass().add("app-title");

        Label subtitleLabel = new Label("Timed Multi-Choice & Short-Answer Exam Simulator");
        subtitleLabel.getStyleClass().add("subtitle");

        headerBox.getChildren().addAll(logoLabel, subtitleLabel);
        root.setTop(headerBox);

        // Center Configuration Card
        VBox card = new VBox(20);
        card.getStyleClass().add("card");
        card.setMaxWidth(620);
        card.setAlignment(Pos.CENTER);

        Label cardTitle = new Label("Configure Your Challenge");
        cardTitle.getStyleClass().add("section-title");

        GridPane grid = new GridPane();
        grid.setHgap(16);
        grid.setVgap(16);
        grid.setAlignment(Pos.CENTER);

        // 1. Player Name
        Label nameLabel = new Label("Player Name:");
        nameLabel.getStyleClass().add("field-label");
        nameField = new TextField("Student");
        nameField.setPromptText("Enter your name");
        grid.add(nameLabel, 0, 0);
        grid.add(nameField, 1, 0);

        // 2. Question Source
        Label sourceLabel = new Label("Question Source:");
        sourceLabel.getStyleClass().add("field-label");
        sourceCombo = new ComboBox<>(FXCollections.observableArrayList(
            "Local SQLite Database",
            "Online Trivia API (OpenTDB)"
        ));
        sourceCombo.setValue("Local SQLite Database");
        sourceCombo.setMaxWidth(Double.MAX_VALUE);
        grid.add(sourceLabel, 0, 1);
        grid.add(sourceCombo, 1, 1);

        // 3. Category
        Label catLabel = new Label("Category:");
        catLabel.getStyleClass().add("field-label");
        categoryCombo = new ComboBox<>();
        populateCategories();
        categoryCombo.setMaxWidth(Double.MAX_VALUE);
        grid.add(catLabel, 0, 2);
        grid.add(categoryCombo, 1, 2);

        // 4. Difficulty
        Label diffLabel = new Label("Difficulty:");
        diffLabel.getStyleClass().add("field-label");
        difficultyCombo = new ComboBox<>(FXCollections.observableArrayList(
            "Any", "Easy", "Medium", "Hard"
        ));
        difficultyCombo.setValue("Any");
        difficultyCombo.setMaxWidth(Double.MAX_VALUE);
        grid.add(diffLabel, 0, 3);
        grid.add(difficultyCombo, 1, 3);

        // 5. Question Count
        Label countLabel = new Label("Number of Questions:");
        countLabel.getStyleClass().add("field-label");
        questionCountCombo = new ComboBox<>(FXCollections.observableArrayList(5, 10, 15, 20));
        questionCountCombo.setValue(5);
        questionCountCombo.setMaxWidth(Double.MAX_VALUE);
        grid.add(countLabel, 0, 4);
        grid.add(questionCountCombo, 1, 4);

        // Make fields fill column
        GridPane.setHgrow(nameField, Priority.ALWAYS);
        GridPane.setHgrow(sourceCombo, Priority.ALWAYS);
        GridPane.setHgrow(categoryCombo, Priority.ALWAYS);
        GridPane.setHgrow(difficultyCombo, Priority.ALWAYS);
        GridPane.setHgrow(questionCountCombo, Priority.ALWAYS);

        // Info Badge
        previewInfoLabel = new Label();
        previewInfoLabel.getStyleClass().add("badge");
        updatePreviewInfo();

        // Listeners to update preview info
        questionCountCombo.setOnAction(e -> updatePreviewInfo());
        difficultyCombo.setOnAction(e -> updatePreviewInfo());

        // Loading indicator
        loadingIndicator = new ProgressIndicator();
        loadingIndicator.setMaxSize(30, 30);
        loadingIndicator.setVisible(false);

        // Start Button
        startButton = new Button("🚀 Start ChronoQuiz");
        startButton.getStyleClass().addAll("button", "btn-primary");
        startButton.setMaxWidth(Double.MAX_VALUE);
        startButton.setOnAction(e -> handleStartQuiz());

        card.getChildren().addAll(cardTitle, grid, previewInfoLabel, loadingIndicator, startButton);

        // Wrap card in center VBox for centering
        VBox centerBox = new VBox(card);
        centerBox.setAlignment(Pos.CENTER);
        root.setCenter(centerBox);

        // Bottom Navigation Bar
        HBox bottomNav = new HBox(16);
        bottomNav.setAlignment(Pos.CENTER);
        bottomNav.setPadding(new Insets(20, 0, 10, 0));

        Button adminBtn = new Button("🔐 Admin Panel");
        adminBtn.getStyleClass().addAll("button", "btn-secondary");
        adminBtn.setOnAction(e -> app.showAdminPanel());

        Button historyBtn = new Button("📊 Past Attempts & Stats");
        historyBtn.getStyleClass().addAll("button", "btn-secondary");
        historyBtn.setOnAction(e -> app.showHistory());

        bottomNav.getChildren().addAll(adminBtn, historyBtn);
        root.setBottom(bottomNav);

        return root;
    }

    private void populateCategories() {
        List<Category> categories = questionDAO.getAllCategories();
        categoryCombo.getItems().clear();
        categoryCombo.getItems().add("All Categories");
        for (Category c : categories) {
            categoryCombo.getItems().add(c.getName());
        }
        categoryCombo.setValue("All Categories");
    }

    private void updatePreviewInfo() {
        int questions = questionCountCombo.getValue() != null ? questionCountCombo.getValue() : 5;
        int timeLimitSeconds = questions * 20; // 20 seconds per question
        int minutes = timeLimitSeconds / 60;
        int seconds = timeLimitSeconds % 60;
        String timeStr = String.format("%02d:%02d", minutes, seconds);

        previewInfoLabel.setText("Estimated Time: " + timeStr + " (" + questions + " questions @ 20s/q) • Auto-Submit on Zero");
    }

    private void handleStartQuiz() {
        String playerName = nameField.getText().trim();
        if (playerName.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Name Required", "Please enter your name to start the quiz.");
            return;
        }

        Player player = userDAO.getOrCreateUser(playerName);
        String selectedSource = sourceCombo.getValue();
        String selectedCategory = categoryCombo.getValue();
        String selectedDifficulty = difficultyCombo.getValue();
        int questionCount = questionCountCombo.getValue() != null ? questionCountCombo.getValue() : 5;
        int timeLimitSeconds = questionCount * 20;

        startButton.setDisable(true);
        loadingIndicator.setVisible(true);

        if ("Online Trivia API (OpenTDB)".equals(selectedSource)) {
            loadOnlineQuestions(player, selectedCategory, selectedDifficulty, questionCount, timeLimitSeconds);
        } else {
            loadLocalQuestions(player, selectedCategory, selectedDifficulty, questionCount, timeLimitSeconds);
        }
    }

    private void loadLocalQuestions(Player player, String categoryName, String difficulty, int count, int timeLimit) {
        int initialCatId = 0;
        if (!"All Categories".equalsIgnoreCase(categoryName)) {
            for (Category c : questionDAO.getAllCategories()) {
                if (c.getName().equalsIgnoreCase(categoryName)) {
                    initialCatId = c.getId();
                    break;
                }
            }
        }
        final int resolvedCatId = initialCatId;

        Task<List<Question>> task = new Task<>() {
            @Override
            protected List<Question> call() {
                List<Question> questions = questionDAO.getQuestionsForQuiz(resolvedCatId, difficulty, count);
                // If not enough questions in specific difficulty/category, fetch any local questions as fallback
                if (questions.isEmpty()) {
                    questions = questionDAO.getQuestionsForQuiz(0, "any", count);
                }
                return questions;
            }
        };

        task.setOnSucceeded(e -> {
            startButton.setDisable(false);
            loadingIndicator.setVisible(false);
            List<Question> questions = task.getValue();

            if (questions.isEmpty()) {
                showAlert(Alert.AlertType.WARNING, "No Questions Available",
                    "No questions found in local question bank. Please add some questions in Question Manager or try Online mode.");
                return;
            }

            Quiz quiz = new Quiz(player, resolvedCatId, categoryName, difficulty, timeLimit, questions);
            app.startQuiz(quiz);
        });

        task.setOnFailed(e -> {
            startButton.setDisable(false);
            loadingIndicator.setVisible(false);
            showAlert(Alert.AlertType.ERROR, "Database Error", "Failed to load local questions: " + task.getException().getMessage());
        });

        Thread th = new Thread(task, "LocalQuestionLoader");
        th.setDaemon(true);
        th.start();
    }

    private void loadOnlineQuestions(Player player, String categoryName, String difficulty, int count, int timeLimit) {
        Integer apiCatId = null;
        if (categoryName != null && ApiService.CATEGORY_MAP.containsKey(categoryName)) {
            apiCatId = ApiService.CATEGORY_MAP.get(categoryName);
        }

        int onlineCatId = 0;
        if (!"All Categories".equalsIgnoreCase(categoryName)) {
            for (Category c : questionDAO.getAllCategories()) {
                if (c.getName().equalsIgnoreCase(categoryName)) {
                    onlineCatId = c.getId();
                    break;
                }
            }
        }
        final int resolvedOnlineCatId = onlineCatId;

        apiService.fetchQuestionsAsync(count, apiCatId, difficulty)
            .thenAccept(questions -> Platform.runLater(() -> {
                startButton.setDisable(false);
                loadingIndicator.setVisible(false);

                if (questions == null || questions.isEmpty()) {
                    fallbackToLocal(player, categoryName, difficulty, count, timeLimit, "No questions returned from API.");
                    return;
                }

                Quiz quiz = new Quiz(player, resolvedOnlineCatId, categoryName, difficulty, timeLimit, questions);
                app.startQuiz(quiz);
            }))
            .exceptionally(ex -> {
                Platform.runLater(() -> {
                    fallbackToLocal(player, categoryName, difficulty, count, timeLimit, ex.getMessage());
                });
                return null;
            });
    }

    private void fallbackToLocal(Player player, String categoryName, String difficulty, int count, int timeLimit, String reason) {
        startButton.setDisable(false);
        loadingIndicator.setVisible(false);

        showAlert(Alert.AlertType.WARNING, "Online API Offline / Fallback",
            "Could not fetch online questions (" + reason + "). Falling back to questions from your local SQLite question bank.");

        loadLocalQuestions(player, categoryName, difficulty, count, timeLimit);
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
