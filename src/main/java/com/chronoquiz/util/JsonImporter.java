package com.chronoquiz.util;

import com.chronoquiz.db.QuestionDAO;
import com.chronoquiz.model.MultipleChoiceQuestion;
import com.chronoquiz.model.Question;
import com.chronoquiz.model.ShortAnswerQuestion;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Utility for importing questions from JSON files into the SQLite database.
 */
public class JsonImporter {
    private static final Gson GSON = new Gson();

    public static int importQuestions(File sourceFile, QuestionDAO questionDAO) throws IOException {
        if (!sourceFile.exists()) {
            throw new IOException("File does not exist: " + sourceFile.getAbsolutePath());
        }

        try (FileReader reader = new FileReader(sourceFile, StandardCharsets.UTF_8)) {
            Type listType = new TypeToken<List<Map<String, Object>>>() {}.getType();
            List<Map<String, Object>> rawList = GSON.fromJson(reader, listType);

            if (rawList == null || rawList.isEmpty()) {
                return 0;
            }

            int count = 0;
            for (Map<String, Object> map : rawList) {
                String text = (String) map.get("text");
                if (text == null || text.trim().isEmpty() || questionDAO.questionExistsByText(text)) {
                    continue; // Skip existing or empty
                }

                String catName = (String) map.getOrDefault("category", "General Knowledge");
                String type = (String) map.getOrDefault("type", "multiple");
                String difficulty = (String) map.getOrDefault("difficulty", "medium");
                String correctAnswer = (String) map.getOrDefault("correctAnswer", "");
                String acceptedAnswers = (String) map.getOrDefault("acceptedAnswers", "");

                int catId = questionDAO.getOrCreateCategory(catName);
                Question q;

                if ("short".equalsIgnoreCase(type)) {
                    ShortAnswerQuestion saq = new ShortAnswerQuestion(0, catId, text, difficulty, correctAnswer, null);
                    saq.setAcceptedAnswersFromString(acceptedAnswers);
                    q = saq;
                } else {
                    List<?> rawOptions = (List<?>) map.get("options");
                    List<String> options = new ArrayList<>();
                    if (rawOptions != null) {
                        for (Object o : rawOptions) {
                            options.add(String.valueOf(o));
                        }
                    }
                    if (!options.contains(correctAnswer) && !correctAnswer.isEmpty()) {
                        options.add(correctAnswer);
                    }
                    q = new MultipleChoiceQuestion(0, catId, text, difficulty, correctAnswer, options);
                }

                if (questionDAO.addQuestion(q)) {
                    count++;
                }
            }
            return count;
        }
    }
}
