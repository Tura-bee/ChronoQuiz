package com.chronoquiz.controller;

import com.chronoquiz.ChronoQuizApp;
import com.chronoquiz.db.AttemptDAO;
import com.chronoquiz.model.Attempt;
import com.chronoquiz.model.MultipleChoiceQuestion;
import com.chronoquiz.model.Question;
import com.chronoquiz.model.Quiz;
import com.chronoquiz.model.ShortAnswerQuestion;
import com.chronoquiz.service.QuizTimerService;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.RadioButton;
import javafx.scene.control.TextField;
import javafx.scene.control.ToggleGroup;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.Optional;

/**
 * Controller and View Builder for the Active Quiz Screen.
 * Demonstrates background-thread countdown timer, thread synchronization,
 * auto-submission on timeout, dynamic UI controls, and non-blocking database persistence.
 */
public class QuizController {
    private final ChronoQuizApp app;
    private final Quiz quiz;
    private final AttemptDAO attemptDAO = new AttemptDAO();
    private QuizTimerService timerService;

    private BorderPane root;
    private Label timerLabel;
    private ProgressBar timeProgressBar;
    private Label questionCounterLabel;
    private Label questionTextLabel;
    private VBox inputContainer;
    private Button nextButton;

    // Active question UI inputs
    private ToggleGroup mcToggleGroup;
    private TextField shortAnswerField;

    public QuizController(ChronoQuizApp app, Quiz quiz) {
        this.app = app;
        this.quiz = quiz;
    }

    public Parent getView() {
        root = new BorderPane();
        root.getStyleClass().add("app-container");

        // Top Header: Info + Timer + Progress
        VBox topBox = new VBox(12);
        topBox.setPadding(new Insets(0, 0, 20, 0));

        HBox metaBar = new HBox(16);
        metaBar.setAlignment(Pos.CENTER_LEFT);

        Label playerBadge = new Label("👤 " + quiz.getPlayer().getName());
        playerBadge.getStyleClass().add("badge");

        Label categoryBadge = new Label("📂 " + quiz.getCategoryName());
        categoryBadge.getStyleClass().add("badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        timerLabel = new Label(formatTime(quiz.getTimeLimitSeconds()));
        timerLabel.getStyleClass().add("timer-label");

        metaBar.getChildren().addAll(playerBadge, categoryBadge, spacer, timerLabel);

        timeProgressBar = new ProgressBar(1.0);
        timeProgressBar.setMaxWidth(Double.MAX_VALUE);
        timeProgressBar.setPrefHeight(12);

        topBox.getChildren().addAll(metaBar, timeProgressBar);
        root.setTop(topBox);

        // Center Area: Question Card
        VBox card = new VBox(20);
        card.getStyleClass().add("card");
        card.setMaxWidth(760);
        card.setAlignment(Pos.TOP_LEFT);

        HBox counterBox = new HBox();
        counterBox.setAlignment(Pos.CENTER_LEFT);
        questionCounterLabel = new Label();
        questionCounterLabel.getStyleClass().add("subtitle");
        counterBox.getChildren().add(questionCounterLabel);

        questionTextLabel = new Label();
        questionTextLabel.setWrapText(true);
        questionTextLabel.getStyleClass().add("section-title");

        inputContainer = new VBox(14);
        inputContainer.setPadding(new Insets(10, 0, 10, 0));

        card.getChildren().addAll(counterBox, questionTextLabel, inputContainer);

        VBox centerWrap = new VBox(card);
        centerWrap.setAlignment(Pos.CENTER);
        root.setCenter(centerWrap);

        // Bottom Navigation Bar
        HBox bottomBar = new HBox(20);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(20, 0, 0, 0));

        Button abandonBtn = new Button("Abandon Quiz");
        abandonBtn.getStyleClass().addAll("button", "btn-secondary");
        abandonBtn.setOnAction(e -> handleAbandon());

        Region bSpacer = new Region();
        HBox.setHgrow(bSpacer, Priority.ALWAYS);

        nextButton = new Button("Next Question ➔");
        nextButton.getStyleClass().addAll("button", "btn-primary");
        nextButton.setDisable(true); // Must remain disabled until an answer is selected/entered
        nextButton.setOnAction(e -> handleNextOrSubmit());

        bottomBar.getChildren().addAll(abandonBtn, bSpacer, nextButton);
        root.setBottom(bottomBar);

        // Render first question
        displayCurrentQuestion();

        // Initialize and start background timer
        initTimer();

        return root;
    }

    private void initTimer() {
        timerService = new QuizTimerService(quiz.getTimeLimitSeconds(), new QuizTimerService.TimerCallback() {
            @Override
            public void onTick(int remainingSeconds, double progressFraction) {
                quiz.setTimeRemainingSeconds(remainingSeconds);
                timerLabel.setText(formatTime(remainingSeconds));
                timeProgressBar.setProgress(progressFraction);

                timerLabel.getStyleClass().removeAll("timer-warning", "timer-danger");
                if (progressFraction <= 0.20) {
                    timerLabel.getStyleClass().add("timer-danger");
                } else if (progressFraction <= 0.50) {
                    timerLabel.getStyleClass().add("timer-warning");
                }
            }

            @Override
            public void onTimeExpired() {
                handleAutoSubmitTimeout();
            }
        });

        timerService.start();
    }

    private void displayCurrentQuestion() {
        Question current = quiz.getCurrentQuestion();
        if (current == null) return;

        questionCounterLabel.setText("Question " + (quiz.getCurrentIndex() + 1) + " of " + quiz.getTotalQuestions()
            + " • Difficulty: " + current.getDifficulty().toUpperCase());
        questionTextLabel.setText(current.getQuestionText());

        inputContainer.getChildren().clear();
        nextButton.setDisable(true);

        if (!quiz.hasNext()) {
            nextButton.setText("Submit Quiz ✔");
            nextButton.getStyleClass().removeAll("btn-primary");
            nextButton.getStyleClass().add("btn-success");
        } else {
            nextButton.setText("Next Question ➔");
            nextButton.getStyleClass().removeAll("btn-success");
            nextButton.getStyleClass().add("btn-primary");
        }

        // Render controls based on Question subclass polymorphism
        if (current instanceof MultipleChoiceQuestion mcq) {
            mcToggleGroup = new ToggleGroup();
            for (String opt : mcq.getOptions()) {
                RadioButton rb = new RadioButton(opt);
                rb.setToggleGroup(mcToggleGroup);
                rb.setMaxWidth(Double.MAX_VALUE);
                rb.setWrapText(true);
                inputContainer.getChildren().add(rb);
            }

            mcToggleGroup.selectedToggleProperty().addListener((obs, oldVal, newVal) -> {
                nextButton.setDisable(newVal == null);
            });

        } else if (current instanceof ShortAnswerQuestion) {
            shortAnswerField = new TextField();
            shortAnswerField.setPromptText("Type your short answer here...");
            shortAnswerField.textProperty().addListener((obs, oldVal, newVal) -> {
                nextButton.setDisable(newVal == null || newVal.trim().isEmpty());
            });
            inputContainer.getChildren().add(shortAnswerField);
            Platform.runLater(() -> shortAnswerField.requestFocus());
        }
    }

    private void captureCurrentAnswer() {
        Question current = quiz.getCurrentQuestion();
        if (current == null) return;

        if (current instanceof MultipleChoiceQuestion) {
            if (mcToggleGroup != null && mcToggleGroup.getSelectedToggle() != null) {
                RadioButton selected = (RadioButton) mcToggleGroup.getSelectedToggle();
                current.setUserAnswer(selected.getText());
            }
        } else if (current instanceof ShortAnswerQuestion) {
            if (shortAnswerField != null) {
                current.setUserAnswer(shortAnswerField.getText().trim());
            }
        }
    }

    private void handleNextOrSubmit() {
        captureCurrentAnswer();

        if (quiz.hasNext()) {
            quiz.nextQuestion();
            displayCurrentQuestion();
        } else {
            finalizeAndSubmitQuiz(false);
        }
    }

    private void handleAutoSubmitTimeout() {
        captureCurrentAnswer();
        finalizeAndSubmitQuiz(true);
    }

    private void finalizeAndSubmitQuiz(boolean timedOut) {
        cleanup();
        quiz.setCompleted(true);

        Attempt attempt = quiz.toAttempt();

        // Persist attempt to SQLite in a non-blocking background Task
        Task<Boolean> saveTask = new Task<>() {
            @Override
            protected Boolean call() {
                return attemptDAO.saveAttempt(attempt);
            }
        };

        saveTask.setOnSucceeded(e -> {
            app.showResultsScreen(quiz, attempt);
            if (timedOut) {
                Platform.runLater(() -> {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Time Expired");
                    alert.setHeaderText("⏰ Time is up!");
                    alert.setContentText("Your quiz was automatically submitted with answers completed so far.");
                    alert.showAndWait();
                });
            }
        });

        saveTask.setOnFailed(e -> {
            System.err.println("Failed to save attempt in background: " + saveTask.getException().getMessage());
            app.showResultsScreen(quiz, attempt);
        });

        Thread th = new Thread(saveTask, "ChronoQuiz-AttemptSaver");
        th.setDaemon(true);
        th.start();
    }

    private void handleAbandon() {
        if (timerService != null) {
            timerService.pause();
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Abandon Quiz");
        alert.setHeaderText("Are you sure you want to abandon the quiz?");
        alert.setContentText("Your current progress will not be saved.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            cleanup();
            app.showStartScreen();
        } else {
            if (timerService != null) {
                timerService.resume();
            }
        }
    }

    public void cleanup() {
        if (timerService != null) {
            timerService.stop();
            timerService = null;
        }
    }

    private String formatTime(int totalSeconds) {
        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
