package com.chronoquiz.api;

import com.chronoquiz.db.QuestionDAO;
import com.chronoquiz.model.Question;
import com.google.gson.Gson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * Service to asynchronously fetch questions from Open Trivia DB API,
 * parse the JSON response, map them to Question models, and cache them locally in SQLite.
 */
public class ApiService {
    private static final String BASE_URL = "https://opentdb.com/api.php";
    private final HttpClient httpClient;
    private final Gson gson;
    private final ApiQuestionMapper mapper;
    private final QuestionDAO questionDAO;

    // Mapping common display category names to OpenTDB category IDs
    public static final Map<String, Integer> CATEGORY_MAP = new HashMap<>() {{
        put("General Knowledge", 9);
        put("Science: Computers", 18);
        put("Science & Nature", 17);
        put("History", 23);
        put("Geography", 22);
        put("Sports", 21);
    }};

    public ApiService() {
        this.httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();
        this.gson = new Gson();
        this.mapper = new ApiQuestionMapper();
        this.questionDAO = new QuestionDAO();
    }

    /**
     * Builds request URL and sends asynchronous HTTP GET.
     */
    public CompletableFuture<List<Question>> fetchQuestionsAsync(int amount, Integer categoryId, String difficulty) {
        StringBuilder urlBuilder = new StringBuilder(BASE_URL);
        urlBuilder.append("?amount=").append(Math.max(1, amount));

        if (categoryId != null && categoryId > 0) {
            urlBuilder.append("&category=").append(categoryId);
        }

        if (difficulty != null && !difficulty.equalsIgnoreCase("any") && !difficulty.trim().isEmpty()) {
            urlBuilder.append("&difficulty=").append(difficulty.toLowerCase().trim());
        }

        urlBuilder.append("&encode=url3986");

        String targetUrl = urlBuilder.toString();
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(targetUrl))
            .timeout(Duration.ofSeconds(12))
            .header("Accept", "application/json")
            .GET()
            .build();

        return httpClient.sendAsync(request, HttpResponse.BodyHandlers.ofString())
            .thenApply(response -> {
                if (response.statusCode() != 200) {
                    throw new RuntimeException("API returned HTTP error code: " + response.statusCode());
                }
                return response.body();
            })
            .thenApply(jsonBody -> {
                ApiResponse apiResponse = gson.fromJson(jsonBody, ApiResponse.class);
                if (apiResponse == null) {
                    throw new RuntimeException("Empty response from trivia API.");
                }

                if (apiResponse.getResponseCode() != 0) {
                    String errorMsg = switch (apiResponse.getResponseCode()) {
                        case 1 -> "No questions found matching your criteria. Try different options.";
                        case 2 -> "Invalid query parameter supplied to trivia API.";
                        case 5 -> "Too many requests. Open Trivia DB rate limit reached. Please wait a few seconds.";
                        default -> "Trivia API returned code: " + apiResponse.getResponseCode();
                    };
                    throw new RuntimeException(errorMsg);
                }

                List<Question> resultQuestions = new ArrayList<>();
                if (apiResponse.getResults() != null) {
                    for (ApiQuestion apiQ : apiResponse.getResults()) {
                        Question q = mapper.mapToQuestion(apiQ);
                        // Cache locally in SQLite if question does not already exist
                        if (!questionDAO.questionExistsByText(q.getQuestionText())) {
                            questionDAO.addQuestion(q);
                        }
                        resultQuestions.add(q);
                    }
                }
                return resultQuestions;
            });
    }
}
