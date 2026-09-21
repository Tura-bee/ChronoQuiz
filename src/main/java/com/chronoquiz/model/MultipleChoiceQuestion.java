package com.chronoquiz.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Multiple-choice question with a list of options and a single correct option.
 */
public class MultipleChoiceQuestion extends Question {
    private List<String> options = new ArrayList<>();
    private String correctAnswer;

    public MultipleChoiceQuestion() {
        setType("multiple");
    }

    public MultipleChoiceQuestion(int id, int categoryId, String questionText, String difficulty,
                                  String correctAnswer, List<String> options) {
        super(id, categoryId, "multiple", questionText, difficulty);
        this.correctAnswer = correctAnswer;
        if (options != null) {
            this.options = new ArrayList<>(options);
        }
    }

    public String getCorrectAnswer() {
        return correctAnswer;
    }

    public void setCorrectAnswer(String correctAnswer) {
        this.correctAnswer = correctAnswer;
    }

    @Override
    public List<String> getOptions() {
        return options;
    }

    @Override
    public void setOptions(List<String> options) {
        this.options = options != null ? new ArrayList<>(options) : new ArrayList<>();
    }

    public void shuffleOptions() {
        if (options != null && options.size() > 1) {
            Collections.shuffle(options);
        }
    }

    @Override
    public boolean checkAnswer(String answer) {
        if (answer == null || correctAnswer == null) {
            return false;
        }
        return answer.trim().equalsIgnoreCase(correctAnswer.trim());
    }

    @Override
    public String getCorrectAnswerDisplay() {
        return correctAnswer != null ? correctAnswer : "";
    }

    @Override
    public String getAcceptedAnswersString() {
        return correctAnswer != null ? correctAnswer : "";
    }
}
