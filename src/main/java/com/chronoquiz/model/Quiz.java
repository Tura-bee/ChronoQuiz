package com.chronoquiz.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Manages an active quiz session, questions, progress, and scoring logic.
 */
public class Quiz {
    private Player player;
    private int categoryId;
    private String categoryName;
    private String difficulty;
    private int timeLimitSeconds;
    private int timeRemainingSeconds;
    private List<Question> questions = new ArrayList<>();
    private int currentIndex = 0;
    private boolean completed = false;

    public Quiz(Player player, int categoryId, String categoryName, String difficulty,
                int timeLimitSeconds, List<Question> questions) {
        this.player = player != null ? player : new Player("Player");
        this.categoryId = categoryId;
        this.categoryName = categoryName != null ? categoryName : "All Categories";
        this.difficulty = difficulty != null ? difficulty : "medium";
        this.timeLimitSeconds = timeLimitSeconds;
        this.timeRemainingSeconds = timeLimitSeconds;
        if (questions != null) {
            this.questions = new ArrayList<>(questions);
        }
    }

    public Player getPlayer() {
        return player;
    }

    public int getCategoryId() {
        return categoryId;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public String getDifficulty() {
        return difficulty;
    }

    public int getTimeLimitSeconds() {
        return timeLimitSeconds;
    }

    public int getTimeRemainingSeconds() {
        return timeRemainingSeconds;
    }

    public void setTimeRemainingSeconds(int timeRemainingSeconds) {
        this.timeRemainingSeconds = Math.max(0, timeRemainingSeconds);
    }

    public int getTimeTakenSeconds() {
        return Math.max(0, timeLimitSeconds - timeRemainingSeconds);
    }

    public List<Question> getQuestions() {
        return questions;
    }

    public int getCurrentIndex() {
        return currentIndex;
    }

    public int getTotalQuestions() {
        return questions.size();
    }

    public Question getCurrentQuestion() {
        if (questions.isEmpty() || currentIndex < 0 || currentIndex >= questions.size()) {
            return null;
        }
        return questions.get(currentIndex);
    }

    public boolean hasNext() {
        return currentIndex < questions.size() - 1;
    }

    public void nextQuestion() {
        if (hasNext()) {
            currentIndex++;
        }
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public int calculateScore() {
        int score = 0;
        for (Question q : questions) {
            if (q.isUserAnswerCorrect()) {
                score++;
            }
        }
        return score;
    }

    public int getAnsweredCount() {
        int count = 0;
        for (Question q : questions) {
            if (q.getUserAnswer() != null && !q.getUserAnswer().trim().isEmpty()) {
                count++;
            }
        }
        return count;
    }

    /**
     * Converts the completed quiz into an Attempt object ready for database persistence.
     */
    public Attempt toAttempt() {
        Attempt attempt = new Attempt();
        attempt.setUserId(player != null ? player.getId() : 1);
        attempt.setUserName(player != null ? player.getName() : "Anonymous");
        attempt.setCategoryId(categoryId);
        attempt.setCategoryName(categoryName);
        attempt.setScore(calculateScore());
        attempt.setTotal(getTotalQuestions());
        attempt.setTimeTakenSec(getTimeTakenSeconds());

        List<AttemptAnswer> attemptAnswers = new ArrayList<>();
        for (Question q : questions) {
            AttemptAnswer ans = new AttemptAnswer();
            ans.setQuestionId(q.getId());
            ans.setQuestionText(q.getQuestionText());
            ans.setUserAnswer(q.getUserAnswer() != null ? q.getUserAnswer() : "(Unanswered)");
            ans.setCorrectAnswer(q.getCorrectAnswerDisplay());
            ans.setCorrect(q.isUserAnswerCorrect());
            attemptAnswers.add(ans);
        }
        attempt.setAnswers(attemptAnswers);

        return attempt;
    }
}
