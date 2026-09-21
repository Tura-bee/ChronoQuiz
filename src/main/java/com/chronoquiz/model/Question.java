package com.chronoquiz.model;

import java.util.List;

/**
 * Abstract base class representing a quiz question.
 * Encapsulates common attributes and defines polymorphic answer-checking.
 */
public abstract class Question {
    private int id;
    private int categoryId;
    private String categoryName = "General Knowledge";
    private String type; // "multiple" or "short"
    private String questionText;
    private String difficulty = "medium"; // "easy", "medium", "hard"
    private String userAnswer;

    public Question() {
    }

    public Question(int id, int categoryId, String type, String questionText, String difficulty) {
        this.id = id;
        this.categoryId = categoryId;
        this.type = type;
        this.questionText = questionText;
        this.difficulty = difficulty;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public void setCategoryName(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public void setDifficulty(String difficulty) {
        this.difficulty = difficulty;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    /**
     * Checks if the provided answer is considered correct.
     * Overridden polymorphically by subclasses.
     */
    public abstract boolean checkAnswer(String answer);

    /**
     * Checks if the user's recorded answer is correct.
     */
    public boolean isUserAnswerCorrect() {
        if (userAnswer == null) {
            return false;
        }
        return checkAnswer(userAnswer);
    }

    /**
     * Returns a string representation of the correct answer for display/review.
     */
    public abstract String getCorrectAnswerDisplay();

    /**
     * Returns options for multiple choice questions, or empty list for short-answer.
     */
    public abstract List<String> getOptions();

    /**
     * Sets options for multiple choice questions.
     */
    public abstract void setOptions(List<String> options);

    /**
     * Returns accepted answers as a comma-separated string for short-answer questions.
     */
    public abstract String getAcceptedAnswersString();
}
