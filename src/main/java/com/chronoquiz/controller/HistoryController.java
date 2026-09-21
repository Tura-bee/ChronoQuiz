package com.chronoquiz.controller;

import com.chronoquiz.ChronoQuizApp;
import com.chronoquiz.db.AttemptDAO;
import com.chronoquiz.db.QuestionDAO;
import com.chronoquiz.model.Attempt;
import com.chronoquiz.model.Category;
import com.chronoquiz.util.JsonExporter;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Controller and View Builder for Quiz History and Statistics Screen.
 * Demonstrates TableView with SQLite attempt records, category filtering,
 * statistical aggregation (Best/Average/Total), and JSON export.
 */
public class HistoryController {
    private final ChronoQuizApp app;
    private final AttemptDAO attemptDAO = new AttemptDAO();
    private final QuestionDAO questionDAO = new QuestionDAO();

    private TableView<Attempt> tableView;
    private ObservableList<Attempt> attemptList;
    private ComboBox<String> categoryFilterCombo;

    // Stat card labels
    private Label totalAttemptsLabel;
    private Label bestScoreLabel;
    private Label avgScoreLabel;
    private Label categoryBreakdownLabel;

    public HistoryController(ChronoQuizApp app) {
        this.app = app;
    }

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-container");

        // Top Header
        VBox topBox = new VBox(12);
        topBox.setPadding(new Insets(0, 0, 16, 0));

        HBox headerRow = new HBox(16);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("📊 Quiz History & Performance");
        title.getStyleClass().add("app-title");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        // Category Filter
        Label filterLabel = new Label("Filter Category:");
        filterLabel.getStyleClass().add("field-label");

        categoryFilterCombo = new ComboBox<>();
        categoryFilterCombo.getItems().add("All Categories");
        for (Category c : questionDAO.getAllCategories()) {
            categoryFilterCombo.getItems().add(c.getName());
        }
        categoryFilterCombo.setValue("All Categories");
        categoryFilterCombo.setOnAction(e -> applyFilter());

        Button exportBtn = new Button("💾 Export History JSON");
        exportBtn.getStyleClass().addAll("button", "btn-secondary");
        exportBtn.setOnAction(e -> handleExportJson());

        headerRow.getChildren().addAll(title, spacer, filterLabel, categoryFilterCombo, exportBtn);
        topBox.getChildren().add(headerRow);

        // Stats Summary Ribbon
        HBox statsRibbon = new HBox(16);
        statsRibbon.setAlignment(Pos.CENTER);

        totalAttemptsLabel = new Label("0");
        bestScoreLabel = new Label("0.0%");
        avgScoreLabel = new Label("0.0%");
        categoryBreakdownLabel = new Label("None");

        statsRibbon.getChildren().addAll(
            createStatCard("Total Attempts", totalAttemptsLabel, "#89b4fa"),
            createStatCard("Best Score", bestScoreLabel, "#a6e3a1"),
            createStatCard("Average Score", avgScoreLabel, "#f9e2af"),
            createStatCard("Top Categories", categoryBreakdownLabel, "#cdd6f4")
        );
        topBox.getChildren().add(statsRibbon);
        root.setTop(topBox);

        // Center TableView
        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);

        TableColumn<Attempt, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()));
        idCol.setMaxWidth(60);

        TableColumn<Attempt, String> userCol = new TableColumn<>("Player");
        userCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUserName()));
        userCol.setMinWidth(120);

        TableColumn<Attempt, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoryName()));
        catCol.setMinWidth(160);

        TableColumn<Attempt, String> scoreCol = new TableColumn<>("Score");
        scoreCol.setCellValueFactory(data -> new SimpleStringProperty(
            data.getValue().getScore() + " / " + data.getValue().getTotal()
        ));
        scoreCol.setMaxWidth(90);

        TableColumn<Attempt, String> pctCol = new TableColumn<>("Accuracy");
        pctCol.setCellValueFactory(data -> new SimpleStringProperty(
            String.format("%.1f%%", data.getValue().getPercentage())
        ));
        pctCol.setMaxWidth(100);

        TableColumn<Attempt, String> timeCol = new TableColumn<>("Time Taken");
        timeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getFormattedTime()));
        timeCol.setMaxWidth(100);

        TableColumn<Attempt, String> dateCol = new TableColumn<>("Date & Time");
        dateCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getTakenAt()));
        dateCol.setMinWidth(160);

        tableView.getColumns().addAll(idCol, userCol, catCol, scoreCol, pctCol, timeCol, dateCol);
        root.setCenter(tableView);

        // Bottom Action Bar
        HBox bottomBar = new HBox(16);
        bottomBar.setAlignment(Pos.CENTER_RIGHT);
        bottomBar.setPadding(new Insets(16, 0, 0, 0));

        Button backBtn = new Button("⬅ Back to Home");
        backBtn.getStyleClass().addAll("button", "btn-secondary");
        backBtn.setOnAction(e -> app.showStartScreen());

        Region bSpacer = new Region();
        HBox.setHgrow(bSpacer, Priority.ALWAYS);

        Button clearBtn = new Button("🗑 Clear History");
        clearBtn.getStyleClass().addAll("button", "btn-danger");
        clearBtn.setOnAction(e -> handleClearHistory());

        Button reviewBtn = new Button("🔍 Review Selected Attempt");
        reviewBtn.getStyleClass().addAll("button", "btn-primary");
        reviewBtn.setOnAction(e -> handleReviewSelected());

        bottomBar.getChildren().addAll(backBtn, bSpacer, clearBtn, reviewBtn);
        root.setBottom(bottomBar);

        // Load data
        loadAttempts(null);
        refreshStatistics();

        return root;
    }

    private VBox createStatCard(String title, Label valueLabel, String colorHex) {
        VBox card = new VBox(4);
        card.getStyleClass().add("card");
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(10, 20, 10, 20));
        HBox.setHgrow(card, Priority.ALWAYS);

        Label titleLabel = new Label(title);
        titleLabel.getStyleClass().add("field-label");

        valueLabel.setStyle("-fx-font-size: 18px; -fx-font-weight: 800; -fx-text-fill: " + colorHex + ";");

        card.getChildren().addAll(titleLabel, valueLabel);
        return card;
    }

    private void loadAttempts(Integer categoryId) {
        List<Attempt> list = attemptDAO.getAllAttempts(categoryId);
        attemptList = FXCollections.observableArrayList(list);
        tableView.setItems(attemptList);
    }

    private void applyFilter() {
        String selected = categoryFilterCombo.getValue();
        if (selected == null || "All Categories".equalsIgnoreCase(selected)) {
            loadAttempts(null);
        } else {
            Integer catId = null;
            for (Category c : questionDAO.getAllCategories()) {
                if (c.getName().equalsIgnoreCase(selected)) {
                    catId = c.getId();
                    break;
                }
            }
            loadAttempts(catId);
        }
    }

    private void refreshStatistics() {
        Map<String, Object> stats = attemptDAO.getHistoryStatistics();

        int total = (int) stats.getOrDefault("totalAttempts", 0);
        double best = (double) stats.getOrDefault("bestPct", 0.0);
        double avg = (double) stats.getOrDefault("avgPct", 0.0);

        totalAttemptsLabel.setText(String.valueOf(total));
        bestScoreLabel.setText(String.format("%.1f%%", best));
        avgScoreLabel.setText(String.format("%.1f%%", avg));

        @SuppressWarnings("unchecked")
        Map<String, Integer> catCounts = (Map<String, Integer>) stats.get("categoryCounts");
        if (catCounts != null && !catCounts.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            catCounts.forEach((cat, cnt) -> sb.append(cat).append(" (").append(cnt).append(")  "));
            categoryBreakdownLabel.setText(sb.toString().trim());
        } else {
            categoryBreakdownLabel.setText("No attempts recorded");
        }
    }

    private void handleReviewSelected() {
        Attempt selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Selection Required");
            alert.setHeaderText(null);
            alert.setContentText("Please select an attempt from the table to review.");
            alert.showAndWait();
            return;
        }

        app.showReviewScreen(selected, true);
    }

    private void handleClearHistory() {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Clear All History");
        alert.setHeaderText("Delete all attempt history records?");
        alert.setContentText("This action cannot be undone.");

        Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (attemptDAO.clearHistory()) {
                loadAttempts(null);
                refreshStatistics();
            }
        }
    }

    private void handleExportJson() {
        if (attemptList == null || attemptList.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Nothing to Export");
            alert.setHeaderText(null);
            alert.setContentText("No attempt records to export.");
            alert.showAndWait();
            return;
        }

        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Attempts History to JSON");
        chooser.setInitialFileName("chronoquiz_history.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));

        File file = chooser.showSaveDialog(app.getPrimaryStage());
        if (file != null) {
            try {
                JsonExporter.exportAttempts(attemptList, file);
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Export Success");
                alert.setHeaderText(null);
                alert.setContentText("Exported " + attemptList.size() + " attempt records to " + file.getName());
                alert.showAndWait();
            } catch (Exception e) {
                Alert alert = new Alert(Alert.AlertType.ERROR);
                alert.setTitle("Export Failed");
                alert.setHeaderText(null);
                alert.setContentText("Error exporting JSON: " + e.getMessage());
                alert.showAndWait();
            }
        }
    }
}
