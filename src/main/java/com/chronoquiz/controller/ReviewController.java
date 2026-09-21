package com.chronoquiz.controller;

import com.chronoquiz.ChronoQuizApp;
import com.chronoquiz.db.AttemptDAO;
import com.chronoquiz.model.Attempt;
import com.chronoquiz.model.AttemptAnswer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.util.List;

/**
 * Controller and View Builder for Answer Review Screen.
 * Provides detailed side-by-side analysis of user responses versus accepted correct answers.
 */
public class ReviewController {
    private final ChronoQuizApp app;
    private final Attempt attempt;
    private final boolean returnToHistory;
    private final AttemptDAO attemptDAO = new AttemptDAO();

    public ReviewController(ChronoQuizApp app, Attempt attempt, boolean returnToHistory) {
        this.app = app;
        this.attempt = attempt;
        this.returnToHistory = returnToHistory;
    }

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-container");

        // Top Header
        VBox header = new VBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        header.setPadding(new Insets(0, 0, 16, 0));

        HBox topRow = new HBox(16);
        topRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("Answer Review");
        title.getStyleClass().add("app-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label scoreBadge = new Label("Score: " + attempt.getScore() + "/" + attempt.getTotal()
            + " (" + String.format("%.1f%%", attempt.getPercentage()) + ")");
        scoreBadge.getStyleClass().add("badge");

        topRow.getChildren().addAll(title, spacer, scoreBadge);

        Label subtitle = new Label("Player: " + attempt.getUserName() + " • Category: " + attempt.getCategoryName()
            + " • Time Taken: " + attempt.getFormattedTime());
        subtitle.getStyleClass().add("subtitle");

        header.getChildren().addAll(topRow, subtitle);
        root.setTop(header);

        // Center: Scrollable list of questions
        VBox questionsList = new VBox(16);
        questionsList.setPadding(new Insets(10, 16, 10, 16));

        List<AttemptAnswer> answers = attempt.getAnswers();
        if (answers == null || answers.isEmpty()) {
            answers = attemptDAO.getAnswersForAttempt(attempt.getId());
            attempt.setAnswers(answers);
        }

        if (answers.isEmpty()) {
            Label emptyLabel = new Label("No question details recorded for this attempt.");
            emptyLabel.getStyleClass().add("subtitle");
            questionsList.getChildren().add(emptyLabel);
        } else {
            int qNum = 1;
            for (AttemptAnswer ans : answers) {
                questionsList.getChildren().add(createQuestionCard(qNum++, ans));
            }
        }

        ScrollPane scrollPane = new ScrollPane(questionsList);
        scrollPane.setFitToWidth(true);
        scrollPane.setStyle("-fx-background: transparent; -fx-background-color: transparent;");
        root.setCenter(scrollPane);

        // Bottom Navigation Bar
        HBox bottomNav = new HBox(16);
        bottomNav.setAlignment(Pos.CENTER_RIGHT);
        bottomNav.setPadding(new Insets(16, 0, 0, 0));

        Button backBtn = new Button(returnToHistory ? "⬅ Back to History" : "⬅ Back to Results");
        backBtn.getStyleClass().addAll("button", "btn-secondary");
        backBtn.setOnAction(e -> {
            if (returnToHistory) {
                app.showHistory();
            } else {
                app.showResultsScreen(null, attempt);
            }
        });

        Button homeBtn = new Button("🏠 Home Screen");
        homeBtn.getStyleClass().addAll("button", "btn-primary");
        homeBtn.setOnAction(e -> app.showStartScreen());

        bottomNav.getChildren().addAll(backBtn, homeBtn);
        root.setBottom(bottomNav);

        return root;
    }

    private VBox createQuestionCard(int index, AttemptAnswer ans) {
        VBox card = new VBox(10);
        card.getStyleClass().add(ans.isCorrect() ? "review-card-correct" : "review-card-incorrect");

        HBox cardHeader = new HBox(12);
        cardHeader.setAlignment(Pos.CENTER_LEFT);

        Label qNumLabel = new Label("Question " + index);
        qNumLabel.getStyleClass().add("field-label");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Label statusLabel = new Label(ans.isCorrect() ? "✔ Correct" : "✖ Incorrect");
        statusLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: " + (ans.isCorrect() ? "#a6e3a1" : "#f38ba8") + ";");

        cardHeader.getChildren().addAll(qNumLabel, spacer, statusLabel);

        Label questionText = new Label(ans.getQuestionText());
        questionText.setWrapText(true);
        questionText.setStyle("-fx-font-size: 15px; -fx-font-weight: bold; -fx-text-fill: #cdd6f4;");

        // User answer
        HBox userAnsBox = new HBox(8);
        Label userAnsTitle = new Label("Your Answer:");
        userAnsTitle.getStyleClass().add("field-label");
        Label userAnsVal = new Label(ans.getUserAnswer() != null && !ans.getUserAnswer().isEmpty() ? ans.getUserAnswer() : "(No answer given)");
        userAnsVal.setStyle("-fx-font-weight: 600; -fx-text-fill: " + (ans.isCorrect() ? "#a6e3a1" : "#f38ba8") + ";");
        userAnsBox.getChildren().addAll(userAnsTitle, userAnsVal);

        // Correct answer (highlighted especially if user was wrong)
        HBox correctAnsBox = new HBox(8);
        Label correctAnsTitle = new Label("Correct Answer:");
        correctAnsTitle.getStyleClass().add("field-label");
        Label correctAnsVal = new Label(ans.getCorrectAnswer());
        correctAnsVal.setStyle("-fx-font-weight: bold; -fx-text-fill: #a6e3a1;");
        correctAnsBox.getChildren().addAll(correctAnsTitle, correctAnsVal);

        card.getChildren().addAll(cardHeader, questionText, userAnsBox, correctAnsBox);
        return card;
    }
}
