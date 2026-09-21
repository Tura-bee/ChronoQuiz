package com.chronoquiz.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Short-answer question with free-text input evaluated against one or more accepted answers.
 */
public class ShortAnswerQuestion extends Question {
    private String primaryCorrectAnswer;
    private List<String> acceptedAnswers = new ArrayList<>();

    public ShortAnswerQuestion() {
        setType("short");
    }

    public ShortAnswerQuestion(int id, int categoryId, String questionText, String difficulty,
                               String primaryCorrectAnswer, List<String> acceptedAnswers) {
        super(id, categoryId, "short", questionText, difficulty);
        this.primaryCorrectAnswer = primaryCorrectAnswer;
        if (acceptedAnswers != null) {
            this.acceptedAnswers = new ArrayList<>(acceptedAnswers);
        } else if (primaryCorrectAnswer != null) {
            this.acceptedAnswers.add(primaryCorrectAnswer);
        }
    }

    public String getPrimaryCorrectAnswer() {
        return primaryCorrectAnswer;
    }

    public void setPrimaryCorrectAnswer(String primaryCorrectAnswer) {
        this.primaryCorrectAnswer = primaryCorrectAnswer;
    }

    public List<String> getAcceptedAnswers() {
        return acceptedAnswers;
    }

    public void setAcceptedAnswers(List<String> acceptedAnswers) {
        this.acceptedAnswers = acceptedAnswers != null ? new ArrayList<>(acceptedAnswers) : new ArrayList<>();
    }

    public void setAcceptedAnswersFromString(String commaSeparated) {
        this.acceptedAnswers.clear();
        if (commaSeparated != null && !commaSeparated.trim().isEmpty()) {
            String[] parts = commaSeparated.split(",");
            for (String part : parts) {
                String trimmed = part.trim();
                if (!trimmed.isEmpty()) {
                    this.acceptedAnswers.add(trimmed);
                }
            }
        }
        if (this.primaryCorrectAnswer != null && !this.acceptedAnswers.contains(this.primaryCorrectAnswer)) {
            this.acceptedAnswers.add(0, this.primaryCorrectAnswer);
        }
    }

    @Override
    public boolean checkAnswer(String answer) {
        if (answer == null) {
            return false;
        }
        String cleanAnswer = answer.trim().toLowerCase();
        if (cleanAnswer.isEmpty()) {
            return false;
        }

        if (primaryCorrectAnswer != null && cleanAnswer.equalsIgnoreCase(primaryCorrectAnswer.trim())) {
            return true;
        }

        for (String accepted : acceptedAnswers) {
            if (cleanAnswer.equalsIgnoreCase(accepted.trim())) {
                return true;
            }
        }
        return false;
    }

    @Override
    public String getCorrectAnswerDisplay() {
        if (primaryCorrectAnswer != null && !primaryCorrectAnswer.trim().isEmpty()) {
            return primaryCorrectAnswer;
        }
        if (!acceptedAnswers.isEmpty()) {
            return acceptedAnswers.get(0);
        }
        return "";
    }

    @Override
    public List<String> getOptions() {
        return Collections.emptyList();
    }

    @Override
    public void setOptions(List<String> options) {
        // Short answer questions do not have fixed options
    }

    @Override
    public String getAcceptedAnswersString() {
        if (acceptedAnswers.isEmpty()) {
            return primaryCorrectAnswer != null ? primaryCorrectAnswer : "";
        }
        return String.join(", ", acceptedAnswers);
    }
}
