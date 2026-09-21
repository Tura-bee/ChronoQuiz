package com.chronoquiz.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a completed quiz attempt.
 */
public class Attempt {
    private int id;
    private int userId;
    private String userName = "Anonymous";
    private int categoryId;
    private String categoryName = "General";
    private int score;
    private int total;
    private int timeTakenSec;
    private String takenAt;
    private List<AttemptAnswer> answers = new ArrayList<>();

    public Attempt() {
    }

    public Attempt(int id, int userId, String userName, int categoryId, String categoryName,
                   int score, int total, int timeTakenSec, String takenAt) {
        this.id = id;
        this.userId = userId;
        this.userName = userName;
        this.categoryId = categoryId;
        this.categoryName = categoryName;
        this.score = score;
        this.total = total;
        this.timeTakenSec = timeTakenSec;
        this.takenAt = takenAt;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
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

    public int getScore() {
        return score;
    }

    public void setScore(int score) {
        this.score = score;
    }

    public int getTotal() {
        return total;
    }

    public void setTotal(int total) {
        this.total = total;
    }

    public int getTimeTakenSec() {
        return timeTakenSec;
    }

    public void setTimeTakenSec(int timeTakenSec) {
        this.timeTakenSec = timeTakenSec;
    }

    public String getTakenAt() {
        return takenAt;
    }

    public void setTakenAt(String takenAt) {
        this.takenAt = takenAt;
    }

    public List<AttemptAnswer> getAnswers() {
        return answers;
    }

    public void setAnswers(List<AttemptAnswer> answers) {
        this.answers = answers != null ? answers : new ArrayList<>();
    }

    public double getPercentage() {
        if (total == 0) return 0.0;
        return ((double) score / total) * 100.0;
    }

    public String getFormattedTime() {
        int minutes = timeTakenSec / 60;
        int seconds = timeTakenSec % 60;
        return String.format("%02d:%02d", minutes, seconds);
    }
}
