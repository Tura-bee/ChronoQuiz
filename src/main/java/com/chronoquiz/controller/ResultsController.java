package com.chronoquiz.controller;

import com.chronoquiz.ChronoQuizApp;
import com.chronoquiz.model.Attempt;
import com.chronoquiz.model.Quiz;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/**
 * Controller and View Builder for the Quiz Results Screen.
 * Displays immediate performance breakdown, score calculation, time taken, and navigation.
 */
public class ResultsController {
    private final ChronoQuizApp app;
    private final Quiz quiz;
    private final Attempt attempt;

    public ResultsController(ChronoQuizApp app, Quiz quiz, Attempt attempt) {
        this.app = app;
        this.quiz = quiz;
        this.attempt = attempt;
    }

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-container");

        VBox centerBox = new VBox(24);
        centerBox.setAlignment(Pos.CENTER);
        centerBox.setMaxWidth(680);

        // Header Title
        Label titleLabel = new Label("Quiz Completed!");
        titleLabel.getStyleClass().add("app-title");

        Label playerSubtitle = new Label("Attempt by " + attempt.getUserName() + " • " + attempt.getCategoryName());
        playerSubtitle.getStyleClass().add("subtitle");

        // Results Card
        VBox card = new VBox(20);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);

        // Big Percentage Display
        double pct = attempt.getPercentage();
        Label pctLabel = new Label(String.format("%.1f%%", pct));
        pctLabel.setStyle("-fx-font-size: 52px; -fx-font-weight: 900; -fx-text-fill: " + getScoreColor(pct) + ";");

        Label scoreBadge = new Label(attempt.getScore() + " out of " + attempt.getTotal() + " Correct");
        scoreBadge.getStyleClass().add("badge");

        // Performance Verdict
        Label verdictLabel = new Label(getVerdict(pct));
        verdictLabel.getStyleClass().add("section-title");

        // Metrics Grid
        GridPane statsGrid = new GridPane();
        statsGrid.setHgap(24);
        statsGrid.setVgap(12);
        statsGrid.setAlignment(Pos.CENTER);

        int incorrect = attempt.getTotal() - attempt.getScore();
        addStatItem(statsGrid, 0, 0, "Correct Answers:", String.valueOf(attempt.getScore()), "#a6e3a1");
        addStatItem(statsGrid, 1, 0, "Incorrect / Skipped:", String.valueOf(incorrect), "#f38ba8");
        addStatItem(statsGrid, 0, 1, "Time Taken:", attempt.getFormattedTime(), "#89dceb");
        addStatItem(statsGrid, 1, 1, "Total Questions:", String.valueOf(attempt.getTotal()), "#cdd6f4");

        card.getChildren().addAll(pctLabel, scoreBadge, verdictLabel, statsGrid);

        // Buttons
        HBox buttonBar = new HBox(16);
        buttonBar.setAlignment(Pos.CENTER);
        buttonBar.setPadding(new Insets(10, 0, 0, 0));

        Button reviewBtn = new Button("🔍 Review Answers");
        reviewBtn.getStyleClass().addAll("button", "btn-primary");
        reviewBtn.setOnAction(e -> app.showReviewScreen(attempt, false));

        Button newQuizBtn = new Button("🔄 New Quiz");
        newQuizBtn.getStyleClass().addAll("button", "btn-secondary");
        newQuizBtn.setOnAction(e -> app.showStartScreen());

        Button historyBtn = new Button("📊 View History");
        historyBtn.getStyleClass().addAll("button", "btn-secondary");
        historyBtn.setOnAction(e -> app.showHistory());

        buttonBar.getChildren().addAll(reviewBtn, newQuizBtn, historyBtn);

        centerBox.getChildren().addAll(titleLabel, playerSubtitle, card, buttonBar);

        VBox outerWrapper = new VBox(centerBox);
        outerWrapper.setAlignment(Pos.CENTER);
        root.setCenter(outerWrapper);

        return root;
    }

    private void addStatItem(GridPane grid, int col, int row, String label, String value, String colorHex) {
        VBox box = new VBox(4);
        box.setAlignment(Pos.CENTER);
        box.setPadding(new Insets(8, 16, 8, 16));
        box.setStyle("-fx-background-color: rgba(255, 255, 255, 0.04); -fx-background-radius: 8px;");

        Label titleLbl = new Label(label);
        titleLbl.getStyleClass().add("field-label");

        Label valLbl = new Label(value);
        valLbl.setStyle("-fx-font-size: 18px; -fx-font-weight: bold; -fx-text-fill: " + colorHex + ";");

        box.getChildren().addAll(titleLbl, valLbl);
        grid.add(box, col, row);
    }

    private String getScoreColor(double pct) {
        if (pct >= 80.0) return "#a6e3a1"; // Green
        if (pct >= 60.0) return "#f9e2af"; // Yellow
        return "#f38ba8"; // Red
    }

    private String getVerdict(double pct) {
        if (pct >= 90.0) return "Mastery! Excellent Work! 🏆";
        if (pct >= 75.0) return "Great Job! Solid Understanding! ⭐";
        if (pct >= 50.0) return "Good Effort! Room for Improvement. 💡";
        return "Keep Practicing! Review the questions below. 📚";
    }
}
