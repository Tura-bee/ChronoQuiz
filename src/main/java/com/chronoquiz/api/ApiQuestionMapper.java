package com.chronoquiz.api;

import com.chronoquiz.db.QuestionDAO;
import com.chronoquiz.model.MultipleChoiceQuestion;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Maps Open Trivia DB questions into application model objects with URL/HTML decoding.
 */
public class ApiQuestionMapper {
    private final QuestionDAO questionDAO = new QuestionDAO();

    public MultipleChoiceQuestion mapToQuestion(ApiQuestion apiQ) {
        String rawCategory = decode(apiQ.getCategory());
        String questionText = decode(apiQ.getQuestion());
        String correctAnswer = decode(apiQ.getCorrectAnswer());
        String difficulty = apiQ.getDifficulty() != null ? apiQ.getDifficulty().toLowerCase() : "medium";

        int categoryId = questionDAO.getOrCreateCategory(rawCategory);

        List<String> options = new ArrayList<>();
        options.add(correctAnswer);

        if (apiQ.getIncorrectAnswers() != null) {
            for (String inc : apiQ.getIncorrectAnswers()) {
                options.add(decode(inc));
            }
        }

        // True/False or Multiple choice
        if ("boolean".equalsIgnoreCase(apiQ.getType())) {
            // Keep True / False order consistent
            options.clear();
            options.add("True");
            options.add("False");
        } else {
            Collections.shuffle(options);
        }

        MultipleChoiceQuestion q = new MultipleChoiceQuestion(
            0, categoryId, questionText, difficulty, correctAnswer, options
        );
        q.setCategoryName(rawCategory);
        return q;
    }

    private String decode(String text) {
        if (text == null) return "";
        try {
            // Decode URL encoding
            String decoded = URLDecoder.decode(text, StandardCharsets.UTF_8);
            // Replace common leftover HTML entities
            return decoded
                .replace("&quot;", "\"")
                .replace("&#039;", "'")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&rsquo;", "'")
                .replace("&lsquo;", "'")
                .replace("&ldquo;", "\"")
                .replace("&rdquo;", "\"");
        } catch (Exception e) {
            return text;
        }
    }
}
