package com.chronoquiz.util;

import com.chronoquiz.model.Attempt;
import com.chronoquiz.model.Question;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Utility for exporting Question Banks and Quiz Attempt Results to structured JSON files.
 */
public class JsonExporter {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    public static boolean exportQuestions(List<Question> questions, File targetFile) throws IOException {
        List<Map<String, Object>> dataList = new ArrayList<>();
        for (Question q : questions) {
            Map<String, Object> map = new HashMap<>();
            map.put("category", q.getCategoryName());
            map.put("type", q.getType());
            map.put("difficulty", q.getDifficulty());
            map.put("text", q.getQuestionText());
            map.put("correctAnswer", q.getCorrectAnswerDisplay());
            map.put("acceptedAnswers", q.getAcceptedAnswersString());
            map.put("options", q.getOptions());
            dataList.add(map);
        }

        try (FileWriter writer = new FileWriter(targetFile, StandardCharsets.UTF_8)) {
            GSON.toJson(dataList, writer);
            return true;
        }
    }

    public static boolean exportAttempts(List<Attempt> attempts, File targetFile) throws IOException {
        try (FileWriter writer = new FileWriter(targetFile, StandardCharsets.UTF_8)) {
            GSON.toJson(attempts, writer);
            return true;
        }
    }
}
