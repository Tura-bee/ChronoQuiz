package com.chronoquiz.model;

/**
 * Represents a recorded answer for a single question within an attempt.
 */
public class AttemptAnswer {
    private int id;
    private int attemptId;
    private int questionId;
    private String questionText;
    private String userAnswer;
    private String correctAnswer;
    private boolean isCorrect;

    public AttemptAnswer() {
    }

    public AttemptAnswer(int id, int attemptId, int questionId, String questionText,
                         String userAnswer, String correctAnswer, boolean isCorrect) {
        this.id = id;
        this.attemptId = attemptId;
        this.questionId = questionId;
        this.questionText = questionText;
        this.userAnswer = userAnswer;
        this.correctAnswer = correctAnswer;
        this.isCorrect = isCorrect;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getAttemptId() {
        return attemptId;
    }

    public void setAttemptId(int attemptId) {
        this.attemptId = attemptId;
    }

    public int getQuestionId() {
        return questionId;
    }

    public void setQuestionId(int questionId) {
        this.questionId = questionId;
    }

    public String getQuestionText() {
        return questionText;
    }

    public void setQuestionText(String questionText) {
        this.questionText = questionText;
    }

    public String getUserAnswer() {
        return userAnswer;
    }

    public void setUserAnswer(String userAnswer) {
        this.userAnswer = userAnswer;
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    public boolean isCorrect() {
        return isCorrect;
    }

    public void setCorrect(boolean correct) {
        isCorrect = correct;
    }
}
