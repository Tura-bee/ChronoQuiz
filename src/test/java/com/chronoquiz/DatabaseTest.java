package com.chronoquiz;

import com.chronoquiz.db.DatabaseManager;
import com.chronoquiz.db.QuestionDAO;
import com.chronoquiz.model.Category;
import com.chronoquiz.model.Question;

import java.util.List;

public class DatabaseTest {
    public static void main(String[] args) {
        System.out.println("Testing Database initialization...");
        DatabaseManager.getInstance().initializeDatabase();

        QuestionDAO dao = new QuestionDAO();
        List<Category> categories = dao.getAllCategories();
        System.out.println("Loaded " + categories.size() + " categories:");
        for (Category c : categories) {
            System.out.println(" - " + c.getName());
        }

        List<Question> questions = dao.getAllQuestions();
        System.out.println("Loaded " + questions.size() + " questions from SQLite question bank:");
        for (Question q : questions) {
            System.out.println(" - [" + q.getType() + "] " + q.getQuestionText() + " -> " + q.getCorrectAnswerDisplay());
        }

        System.out.println("Database verification completed successfully!");
        System.exit(0);
    }
}
