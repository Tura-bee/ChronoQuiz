package com.chronoquiz.controller;

import com.chronoquiz.ChronoQuizApp;
import com.chronoquiz.api.ApiService;
import com.chronoquiz.db.QuestionDAO;
import com.chronoquiz.model.Category;
import com.chronoquiz.model.MultipleChoiceQuestion;
import com.chronoquiz.model.Question;
import com.chronoquiz.model.ShortAnswerQuestion;
import com.chronoquiz.util.JsonExporter;
import com.chronoquiz.util.JsonImporter;
import javafx.application.Platform;
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
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * Controller and View Builder for the Question Bank Manager.
 * Demonstrates full CRUD operations on SQLite, TableView binding,
 * JSON Import/Export, and asynchronous API question fetching.
 */
public class QuestionManagerController {
    private final ChronoQuizApp app;
    private final QuestionDAO questionDAO = new QuestionDAO();
    private final ApiService apiService = new ApiService();

    private TableView<Question> tableView;
    private ObservableList<Question> questionList;

    // Form inputs
    private TextField idField;
    private ComboBox<String> categoryCombo;
    private ComboBox<String> typeCombo;
    private ComboBox<String> difficultyCombo;
    private TextArea questionTextArea;
    private TextField correctAnswerField;
    private TextField optionsOrAcceptedField;
    private Label optionsLabel;

    public QuestionManagerController(ChronoQuizApp app) {
        this.app = app;
    }

    public Parent getView() {
        BorderPane root = new BorderPane();
        root.getStyleClass().add("app-container");

        // Top Header & Action Toolbar
        VBox topBox = new VBox(12);
        topBox.setPadding(new Insets(0, 0, 16, 0));

        HBox headerRow = new HBox(12);
        headerRow.setAlignment(Pos.CENTER_LEFT);

        Label title = new Label("🛡️ Admin Question Manager");
        title.getStyleClass().add("app-title");

        Label adminBadge = new Label("👤 Admin: Active");
        adminBadge.getStyleClass().add("badge");

        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);

        Button apiImportBtn = new Button("🌐 Fetch From Trivia API");
        apiImportBtn.getStyleClass().addAll("button", "btn-primary");
        apiImportBtn.setOnAction(e -> handleFetchFromApi());

        Button jsonExportBtn = new Button("💾 Export JSON");
        jsonExportBtn.getStyleClass().addAll("button", "btn-secondary");
        jsonExportBtn.setOnAction(e -> handleExportJson());

        Button jsonImportBtn = new Button("📂 Import JSON");
        jsonImportBtn.getStyleClass().addAll("button", "btn-secondary");
        jsonImportBtn.setOnAction(e -> handleImportJson());

        Button logoutBtn = new Button("🚪 Logout");
        logoutBtn.getStyleClass().addAll("button", "btn-danger");
        logoutBtn.setOnAction(e -> app.logoutAdmin());

        headerRow.getChildren().addAll(title, adminBadge, spacer, apiImportBtn, jsonExportBtn, jsonImportBtn, logoutBtn);
        topBox.getChildren().add(headerRow);
        root.setTop(topBox);

        // Center: Split layout with Table on Left/Top and Editor on Right/Bottom
        VBox centerBox = new VBox(16);

        // 1. TableView of Questions
        tableView = new TableView<>();
        tableView.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tableView.setPrefHeight(320);

        TableColumn<Question, Number> idCol = new TableColumn<>("ID");
        idCol.setCellValueFactory(data -> new SimpleIntegerProperty(data.getValue().getId()));
        idCol.setMaxWidth(60);
        idCol.setMinWidth(40);

        TableColumn<Question, String> catCol = new TableColumn<>("Category");
        catCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCategoryName()));
        catCol.setMinWidth(130);

        TableColumn<Question, String> typeCol = new TableColumn<>("Type");
        typeCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getType().toUpperCase()));
        typeCol.setMaxWidth(90);
        typeCol.setMinWidth(70);

        TableColumn<Question, String> diffCol = new TableColumn<>("Difficulty");
        diffCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getDifficulty().toUpperCase()));
        diffCol.setMaxWidth(90);
        diffCol.setMinWidth(70);

        TableColumn<Question, String> textCol = new TableColumn<>("Question Text");
        textCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getQuestionText()));
        textCol.setMinWidth(280);

        TableColumn<Question, String> ansCol = new TableColumn<>("Correct Answer");
        ansCol.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getCorrectAnswerDisplay()));
        ansCol.setMinWidth(130);

        tableView.getColumns().addAll(idCol, catCol, typeCol, diffCol, textCol, ansCol);

        // Selection Listener: load into edit form
        tableView.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, selected) -> {
            if (selected != null) {
                populateForm(selected);
            }
        });

        // 2. Editor Form Card
        VBox formCard = new VBox(14);
        formCard.getStyleClass().add("card");

        Label formTitle = new Label("Question Editor");
        formTitle.getStyleClass().add("section-title");

        GridPane formGrid = new GridPane();
        formGrid.setHgap(14);
        formGrid.setVgap(10);

        idField = new TextField();
        idField.setPromptText("Auto (New)");
        idField.setEditable(false);
        idField.setMaxWidth(100);

        categoryCombo = new ComboBox<>();
        categoryCombo.setEditable(true);
        refreshCategoryCombo();

        typeCombo = new ComboBox<>(FXCollections.observableArrayList("multiple", "short"));
        typeCombo.setValue("multiple");
        typeCombo.setOnAction(e -> updateFieldLabelsForType());

        difficultyCombo = new ComboBox<>(FXCollections.observableArrayList("easy", "medium", "hard"));
        difficultyCombo.setValue("medium");

        questionTextArea = new TextArea();
        questionTextArea.setPromptText("Enter the question text...");
        questionTextArea.setPrefRowCount(2);
        questionTextArea.setWrapText(true);

        correctAnswerField = new TextField();
        correctAnswerField.setPromptText("Enter correct answer...");

        optionsOrAcceptedField = new TextField();
        optionsLabel = new Label("Options (comma-separated):");
        optionsLabel.getStyleClass().add("field-label");

        // Layout rows
        formGrid.add(new Label("ID:"), 0, 0);
        formGrid.add(idField, 1, 0);

        formGrid.add(new Label("Category:"), 2, 0);
        formGrid.add(categoryCombo, 3, 0);

        formGrid.add(new Label("Type:"), 0, 1);
        formGrid.add(typeCombo, 1, 1);

        formGrid.add(new Label("Difficulty:"), 2, 1);
        formGrid.add(difficultyCombo, 3, 1);

        formGrid.add(new Label("Question:"), 0, 2);
        formGrid.add(questionTextArea, 1, 2, 3, 1);

        formGrid.add(new Label("Correct Answer:"), 0, 3);
        formGrid.add(correctAnswerField, 1, 3, 3, 1);

        formGrid.add(optionsLabel, 0, 4);
        formGrid.add(optionsOrAcceptedField, 1, 4, 3, 1);

        GridPane.setHgrow(questionTextArea, Priority.ALWAYS);
        GridPane.setHgrow(correctAnswerField, Priority.ALWAYS);
        GridPane.setHgrow(optionsOrAcceptedField, Priority.ALWAYS);
        GridPane.setHgrow(categoryCombo, Priority.ALWAYS);

        // CRUD Action Buttons
        HBox formButtons = new HBox(12);
        formButtons.setAlignment(Pos.CENTER_RIGHT);

        Button addBtn = new Button("➕ Add New");
        addBtn.getStyleClass().addAll("button", "btn-success");
        addBtn.setOnAction(e -> handleAddQuestion());

        Button updateBtn = new Button("💾 Update Selected");
        updateBtn.getStyleClass().addAll("button", "btn-primary");
        updateBtn.setOnAction(e -> handleUpdateQuestion());

        Button deleteBtn = new Button("🗑 Delete Selected");
        deleteBtn.getStyleClass().addAll("button", "btn-danger");
        deleteBtn.setOnAction(e -> handleDeleteQuestion());

        Button clearBtn = new Button("🧹 Clear Form");
        clearBtn.getStyleClass().addAll("button", "btn-secondary");
        clearBtn.setOnAction(e -> clearForm());

        formButtons.getChildren().addAll(addBtn, updateBtn, deleteBtn, clearBtn);

        formCard.getChildren().addAll(formTitle, formGrid, formButtons);

        centerBox.getChildren().addAll(tableView, formCard);
        root.setCenter(centerBox);

        // Bottom Bar
        HBox bottomBar = new HBox();
        bottomBar.setAlignment(Pos.CENTER_LEFT);
        bottomBar.setPadding(new Insets(16, 0, 0, 0));

        Button backBtn = new Button("⬅ Back to Home");
        backBtn.getStyleClass().addAll("button", "btn-secondary");
        backBtn.setOnAction(e -> app.showStartScreen());

        bottomBar.getChildren().add(backBtn);
        root.setBottom(bottomBar);

        // Load initial data
        loadQuestions();

        return root;
    }

    private void updateFieldLabelsForType() {
        String type = typeCombo.getValue();
        if ("short".equalsIgnoreCase(type)) {
            optionsLabel.setText("Accepted Synonyms (comma-separated):");
            optionsOrAcceptedField.setPromptText("e.g. JVM, Java Virtual Machine");
        } else {
            optionsLabel.setText("Options (comma-separated):");
            optionsOrAcceptedField.setPromptText("e.g. Option A, Option B, Option C, Option D");
        }
    }

    private void refreshCategoryCombo() {
        categoryCombo.getItems().clear();
        for (Category c : questionDAO.getAllCategories()) {
            categoryCombo.getItems().add(c.getName());
        }
        if (!categoryCombo.getItems().isEmpty()) {
            categoryCombo.setValue(categoryCombo.getItems().get(0));
        }
    }

    private void loadQuestions() {
        List<Question> list = questionDAO.getAllQuestions();
        questionList = FXCollections.observableArrayList(list);
        tableView.setItems(questionList);
    }

    private void populateForm(Question q) {
        idField.setText(String.valueOf(q.getId()));
        categoryCombo.setValue(q.getCategoryName());
        typeCombo.setValue(q.getType());
        difficultyCombo.setValue(q.getDifficulty());
        questionTextArea.setText(q.getQuestionText());
        correctAnswerField.setText(q.getCorrectAnswerDisplay());

        if (q instanceof MultipleChoiceQuestion mcq) {
            optionsOrAcceptedField.setText(String.join(", ", mcq.getOptions()));
        } else if (q instanceof ShortAnswerQuestion saq) {
            optionsOrAcceptedField.setText(saq.getAcceptedAnswersString());
        }
        updateFieldLabelsForType();
    }

    private void clearForm() {
        idField.clear();
        questionTextArea.clear();
        correctAnswerField.clear();
        optionsOrAcceptedField.clear();
        tableView.getSelectionModel().clearSelection();
    }

    private void handleAddQuestion() {
        Question q = buildQuestionFromForm();
        if (q == null) return;

        if (questionDAO.addQuestion(q)) {
            loadQuestions();
            clearForm();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Question successfully added to database.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not save question to database.");
        }
    }

    private void handleUpdateQuestion() {
        if (idField.getText().isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a question from the table to update.");
            return;
        }

        Question q = buildQuestionFromForm();
        if (q == null) return;
        q.setId(Integer.parseInt(idField.getText()));

        if (questionDAO.updateQuestion(q)) {
            loadQuestions();
            showAlert(Alert.AlertType.INFORMATION, "Success", "Question successfully updated.");
        } else {
            showAlert(Alert.AlertType.ERROR, "Database Error", "Could not update question in database.");
        }
    }

    private void handleDeleteQuestion() {
        Question selected = tableView.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showAlert(Alert.AlertType.WARNING, "Selection Required", "Please select a question from the table to delete.");
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirm Deletion");
        confirm.setHeaderText("Delete Question #" + selected.getId() + "?");
        confirm.setContentText("This will permanently remove the question and its options from the database.");

        Optional<ButtonType> result = confirm.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            if (questionDAO.deleteQuestion(selected.getId())) {
                loadQuestions();
                clearForm();
            } else {
                showAlert(Alert.AlertType.ERROR, "Error", "Failed to delete question.");
            }
        }
    }

    private Question buildQuestionFromForm() {
        String text = questionTextArea.getText().trim();
        String correct = correctAnswerField.getText().trim();
        String catName = categoryCombo.getValue() != null ? categoryCombo.getValue().trim() : "General";
        String type = typeCombo.getValue();
        String diff = difficultyCombo.getValue();

        if (text.isEmpty() || correct.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Validation Error", "Question text and Correct Answer cannot be empty.");
            return null;
        }

        int catId = questionDAO.getOrCreateCategory(catName);

        if ("short".equalsIgnoreCase(type)) {
            ShortAnswerQuestion saq = new ShortAnswerQuestion(0, catId, text, diff, correct, null);
            saq.setCategoryName(catName);
            saq.setAcceptedAnswersFromString(optionsOrAcceptedField.getText());
            return saq;
        } else {
            List<String> options = new ArrayList<>();
            String rawOpts = optionsOrAcceptedField.getText().trim();
            if (!rawOpts.isEmpty()) {
                for (String opt : rawOpts.split(",")) {
                    if (!opt.trim().isEmpty()) {
                        options.add(opt.trim());
                    }
                }
            }
            if (!options.contains(correct)) {
                options.add(correct);
            }
            MultipleChoiceQuestion mcq = new MultipleChoiceQuestion(0, catId, text, diff, correct, options);
            mcq.setCategoryName(catName);
            return mcq;
        }
    }

    private void handleFetchFromApi() {
        Dialog<Boolean> dialog = new Dialog<>();
        dialog.setTitle("Import Questions from Trivia API");
        dialog.setHeaderText("Fetch fresh questions from Open Trivia DB and cache into SQLite");

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20));

        ComboBox<String> catBox = new ComboBox<>(FXCollections.observableArrayList(ApiService.CATEGORY_MAP.keySet()));
        catBox.setValue("Science: Computers");

        ComboBox<String> diffBox = new ComboBox<>(FXCollections.observableArrayList("any", "easy", "medium", "hard"));
        diffBox.setValue("medium");

        ComboBox<Integer> countBox = new ComboBox<>(FXCollections.observableArrayList(5, 10, 15));
        countBox.setValue(10);

        grid.add(new Label("Category:"), 0, 0);
        grid.add(catBox, 1, 0);
        grid.add(new Label("Difficulty:"), 0, 1);
        grid.add(diffBox, 1, 1);
        grid.add(new Label("Amount:"), 0, 2);
        grid.add(countBox, 1, 2);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);

        dialog.setResultConverter(btn -> {
            if (btn == ButtonType.OK) {
                String cat = catBox.getValue();
                Integer catId = ApiService.CATEGORY_MAP.get(cat);
                String diff = diffBox.getValue();
                int amt = countBox.getValue();

                apiService.fetchQuestionsAsync(amt, catId, diff)
                    .thenAccept(questions -> Platform.runLater(() -> {
                        loadQuestions();
                        refreshCategoryCombo();
                        showAlert(Alert.AlertType.INFORMATION, "Import Complete",
                            "Successfully imported " + questions.size() + " questions into your local SQLite question bank!");
                    }))
                    .exceptionally(ex -> {
                        Platform.runLater(() -> showAlert(Alert.AlertType.ERROR, "Import Failed", ex.getMessage()));
                        return null;
                    });
                return true;
            }
            return false;
        });

        dialog.showAndWait();
    }

    private void handleExportJson() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Export Question Bank to JSON");
        chooser.setInitialFileName("chronoquiz_questions.json");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));

        File file = chooser.showSaveDialog(app.getPrimaryStage());
        if (file != null) {
            try {
                JsonExporter.exportQuestions(questionList, file);
                showAlert(Alert.AlertType.INFORMATION, "Export Successful",
                    "Exported " + questionList.size() + " questions to: " + file.getName());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Export Failed", "Error writing JSON: " + e.getMessage());
            }
        }
    }

    private void handleImportJson() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Import Questions from JSON");
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("JSON Files (*.json)", "*.json"));

        File file = chooser.showOpenDialog(app.getPrimaryStage());
        if (file != null) {
            try {
                int imported = JsonImporter.importQuestions(file, questionDAO);
                loadQuestions();
                refreshCategoryCombo();
                showAlert(Alert.AlertType.INFORMATION, "Import Successful",
                    "Successfully imported " + imported + " new questions from " + file.getName());
            } catch (Exception e) {
                showAlert(Alert.AlertType.ERROR, "Import Failed", "Error reading JSON: " + e.getMessage());
            }
        }
    }

    private void showAlert(Alert.AlertType type, String title, String msg) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(msg);
        alert.showAndWait();
    }
}
